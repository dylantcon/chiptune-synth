/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chiptunesynth;

import javax.sound.sampled.*;

/**
 * A small nes-style chiptune synthesizer
 *
 * models the 2A03 APU at a high level: - two pulse (square) channels with
 * selectable duty cycles - one triangle channel with a quantized waveform - one
 * noise channel driven by a 15 bit linear feedback shift register
 *
 * a frame-rate sequencer drives all four channels at approximately 60 hz, the
 * same rate the actual NES ticked its music engine via the NMI handler. note
 * durations are measured in "frames" (60th of second), exactly the unit the
 * original Nintendo Entertainment System drivers used.
 *
 * Audio output is 44.1khz, 16-bit, stereo, via javax.sound.sampled. The synth
   * mixes a single mono signal and duplicates it to both channels: a desktop JVM
   * will hand out a mono SourceDataLine, but CheerpJ's Web Audio-backed mixer
   * only exposes stereo lines, so requesting mono throws at getSourceDataLine.
 *
 * @author dylan
 */
public class ChiptuneSynth implements Runnable {

  // audio constants
  public static final int SAMPLE_RATE = 44100;
  public static final int FRAME_RATE = 60;
  public static final int SAMPLES_PER_FRAME = SAMPLE_RATE / FRAME_RATE;

  // convert a midi note number to frequency in Hz. A4 = 69 = 440Hz.
  // the double overload keeps modulated (fractional) pitches in tune 
  // vibrato/slide/arp land on pitches between the integer semitones.
  public static double freqOf(double midi) {
    return 440.0 * Math.pow(2.0, (midi - 69) / 12.0);
  }

  public static double midiToFreq(int midi) {
    return freqOf(midi);
  }

  /* MAIN INSTANCE MEMBER DATA */
  // thread associated with this chiptune synthesizer
  private Thread synthThread = null;

  // player state
  private final PulseChannel p1 = new PulseChannel();
  private final PulseChannel p2 = new PulseChannel();
  private final TriangleChannel tri = new TriangleChannel();
  private final NoiseChannel noi = new NoiseChannel();
  private final KickVoice kick = new KickVoice();
  private final SnareVoice snare = new SnareVoice();
  private final TomVoice tom = new TomVoice();

  private Track p1Track, p2Track, triTrack, noiTrack;
  private boolean looping = true;
  private volatile boolean running = false;

  private volatile double targetVolume = 0.5;
  private double volume = 0.5;
  private static final double VOLUME_SLEW = 0.001;

  // music speed factor (gameplay knob: level scaling, game-over slowdown).
  // 1.0 always means "normal" to callers.
  private volatile double speed = 1.0;
  // per-song playback-rate correction, set from ChiptuneSong.getTempoScale()
  // and multiplied ON TOP OF speed. corrects songs whose frame data was
  // generated from a MIDI with an imperfect frames-per-second assumption.
  // independent of speed so restartMusic()/level scaling never wipe it.
  private volatile double tempoScale = 1.0;
  private double sequencerAccumulator = 0;

  // musical frames consumed since the song's frame 0  an exact integer
  // count of sequencer steps, adjusted by seek/rewind. Written by the audio
  // thread (increments) and by seek/rewind (absolute sets); a lost increment
  // in that race costs one frame of readout, never audio.
  private volatile long musicalFrame = 0;

  private final java.util.List<ChiptuneSynthListener> listeners =
      new java.util.concurrent.CopyOnWriteArrayList<ChiptuneSynthListener>();

  // per channel mixing weights
  private final double p1Mix = 0.25;
  private final double p2Mix = 0.25;
  private final double triMix = 0.30;
  private final double noiMix = 0.18;
  // kick is loud on purpose: the original drives the groove with sub-150Hz
  // thud (~25% of total energy). tune this if it overpowers the mix.
  private final double kickMix = 0.55;
  // snare/tom each get their own dedicated voice (layered tone + noise) so they
  // punch through dense arrangements instead of drowning on the 0.18 noise bus.
  private final double snareMix = 0.40;
  private final double tomMix = 0.38;

  public ChiptuneSynth setSong(Track p1, Track p2, Track triangle, Track noise) {
    this.p1Track = p1;
    this.p2Track = p2;
    this.triTrack = triangle;
    this.noiTrack = noise;

    if (p1Track != null) {
      p1Track.reset();
    }
    if (p2Track != null) {
      p2Track.reset();
    }
    if (triTrack != null) {
      triTrack.reset();
    }
    if (noiTrack != null) {
      noiTrack.reset();
    }
    musicalFrame = 0;
    return this;
  }

  /** Register for device-clock-grounded playback-position callbacks. */
  public void addListener(ChiptuneSynthListener l) {
    if (l != null && !listeners.contains(l)) {
      listeners.add(l);
    }
  }

  public void removeListener(ChiptuneSynthListener l) {
    listeners.remove(l);
  }
  
  public ChiptuneSynth setSong(ChiptuneSong song) {
    this.setSong(
            song.getLead(),
            song.getHarmony(),
            song.getBass(),
            song.getDrums()
    );
    this.tempoScale = song.getTempoScale();
    // one loop point stamped onto all four tracks: the channels wrap
    // together by construction (a per-track point could never drift less
    // than a whole loop out of alignment)
    int loopStart = song.getLoopStartFrames();
    if (p1Track != null) p1Track.setLoopStart(loopStart);
    if (p2Track != null) p2Track.setLoopStart(loopStart);
    if (triTrack != null) triTrack.setLoopStart(loopStart);
    if (noiTrack != null) noiTrack.setLoopStart(loopStart);
    return this;
  }

  public ChiptuneSynth setLooping(boolean loop) {
    this.looping = loop;
    return this;
  }

  // advance one track by one frame. returns the note that should be sounding
  // (or null when the track is empty, or finished and not looping). updates
  // the track's live state: started=true only on the frame a note
  // (re)triggers, elapsed counts frames into the note, and the portamento
  // origin (glideFrom) is captured from the previous pitch at each onset.
  private Note advance(Track t) {
    if (t == null || t.notes.isEmpty()) {
      return null;
    }
    t.started = false;
    if (t.framesLeft <= 0) {
      if (t.cursor >= t.notes.size()) {
        if (looping) {
          t.cursor = t.loopCursor();   // NSF-style: wrap past the head
        } else {
          return null;
        }
      }
      t.cur = t.notes.get(t.cursor++);
      t.framesLeft = t.cur.durationFrames;
      t.elapsed = 0;
      t.started = true;
      if (t.seekElapsed > 0) {
        // resuming mid-note after Track.seek(): skip ahead inside the note
        // so vibrato/swell/arp/slide land where continuous playback would be
        t.elapsed = t.seekElapsed;
        t.framesLeft -= t.seekElapsed;
        t.seekElapsed = 0;
      }
      if (t.cur.midi >= 0) {
        // a rest does not update prevMidi, so a slide still works across one
        t.glideFrom = Double.isNaN(t.prevMidi) ? t.cur.midi : t.prevMidi;
        t.prevMidi = t.cur.midi;
      }
    } else {
      t.elapsed++;
    }
    t.framesLeft--;
    return t.cur;
  }

  // effective (possibly fractional) midi pitch of a sounding note this frame:
  // the base pitch, optionally glided in from the previous note (portamento),
  // plus the additive arpeggio + vibrato + pitch-envelope offset.
  private static double effMidi(Track t, Note n) {
    double base = n.midi;
    Effect fx = n.fx;
    if (fx.slideFrames > 0 && !Double.isNaN(t.glideFrom)) {
      double a = Math.min(1.0, (double) t.elapsed / fx.slideFrames);
      base = t.glideFrom + (n.midi - t.glideFrom) * a;
    }
    return base + fx.pitchOffset(t.elapsed);
  }

  @Override
  public void run() {
    AudioFormat fmt = new AudioFormat(SAMPLE_RATE, 16, 2, true, false);
    try (SourceDataLine line = AudioSystem.getSourceDataLine(fmt)) {
      // buffer of 4 frames, ~67ms latency (4 bytes/frame * frame size * 4)
      line.open(fmt, SAMPLES_PER_FRAME * 4 * 4);
      line.start();
      byte[] buf = new byte[SAMPLES_PER_FRAME * 4];

      // fresh line per run means a fresh device sample counter; the
      // chronometer maps that counter back onto musical frames
      SongChronometer chron = new SongChronometer(musicalFrame);

      while (running) {
        sequencerAccumulator += speed * tempoScale;
        while (sequencerAccumulator >= 1.0) {
          // decrement sequencer accumulator by 1
          sequencerAccumulator -= 1.0;
          musicalFrame++;

          Note n1 = advance(p1Track);
          if (n1 != null) {
            if (n1.midi < 0) {
              p1.noteOff();
            } else {
              if (p1Track.started) {
                p1.noteOn(freqOf(effMidi(p1Track, n1)), n1.volume, n1.duty, n1.decay);
              } else {
                p1.setFrequency(freqOf(effMidi(p1Track, n1)));
              }
              // re-shape amplitude every frame for swell pads
              if (n1.fx.swell) {
                p1.setLevel(n1.volume * n1.fx.ampScale(p1Track.elapsed, n1.durationFrames));
              }
            }
          }
          Note n2 = advance(p2Track);
          if (n2 != null) {
            if (n2.midi < 0) {
              p2.noteOff();
            } else {
              if (p2Track.started) {
                p2.noteOn(freqOf(effMidi(p2Track, n2)), n2.volume, n2.duty, n2.decay);
              } else {
                p2.setFrequency(freqOf(effMidi(p2Track, n2)));
              }
              if (n2.fx.swell) {
                p2.setLevel(n2.volume * n2.fx.ampScale(p2Track.elapsed, n2.durationFrames));
              }
            }
          }
          Note n3 = advance(triTrack);
          if (n3 != null) {
            if (n3.midi < 0) {
              tri.noteOff();
            } else if (triTrack.started) {
              tri.noteOn(freqOf(effMidi(triTrack, n3)));
            } else {
              tri.setFrequency(freqOf(effMidi(triTrack, n3)));
            }
          }

          Note nd = advance(noiTrack);
          if (nd != null && noiTrack.started) {
            if (nd.midi < 0) {
              noi.noteOff();
            } else if (nd.midi == ChiptuneSong.KICK) {
              kick.trigger();
            } else if (nd.midi == ChiptuneSong.SNARE) {
              snare.trigger(nd.volume);            // layered tone + sizzle
            } else if (nd.midi == ChiptuneSong.HIHAT) {
              // closed hi-hat: tight, fast-decaying broadband tick
              noi.noteOn(midiToFreq(nd.midi) * 8, nd.volume, 80.0, false);
            } else if (nd.midi == ChiptuneSong.CYMBAL) {
              // open hi-hat / crash: long broadband wash
              noi.noteOn(midiToFreq(nd.midi) * 8, nd.volume, 7.0, false);
            } else if (nd.decay == ChiptuneSong.SUSTAINED) {
              // pitched noise, held: raw LFSR at the note's pitch with no
              // decay. Back-to-back notes re-attack seamlessly (the LFSR and
              // envelope never dip), so a run of these is one continuous
              // roar stepping in pitch  the NES noise-sweep texture
              // (Silver Surfer's head), which a percussive tom can't make.
              noi.noteOn(midiToFreq(nd.midi) * 8, nd.volume, 0.0, false);
            } else {
              // any other pitched noise note is a tom (e.g. Contra's C3 fills);
              // play it at its written pitch on the tuned tom voice.
              tom.trigger(midiToFreq(nd.midi), nd.volume);
            }
          }
          kick.tickFrame();    // advance each percussion voice's envelope @60hz
          snare.tickFrame();
          tom.tickFrame();
        }

        // 2. render one frame's worth of audio, which is 735 samples
        for (int i = 0; i < SAMPLES_PER_FRAME; i++) {
          if (volume < targetVolume) {
            volume = Math.min(targetVolume, volume + VOLUME_SLEW);
          } else if (volume > targetVolume) {
            volume = Math.max(targetVolume, volume - VOLUME_SLEW);
          }
          double mix = (p1.sample() * p1Mix + p2.sample() * p2Mix
                  + tri.sample() * triMix + noi.sample() * noiMix
                  + kick.sample() * kickMix + snare.sample() * snareMix
                  + tom.sample() * tomMix) * volume;
          if (mix > 1) {
            mix = 1;
          }
          if (mix < -1) {
            mix = -1;
          }
          short s = (short) (mix * 30000);
          byte lo = (byte) (s & 0xff);
          byte hi = (byte) ((s >> 8) & 0xff);
          int base = i * 4;       // 4 bytes per stereo frame: L lo/hi, R lo/hi
          buf[base]     = lo;     // left  channel, low byte
          buf[base + 1] = hi;     // left  channel, high byte
          buf[base + 2] = lo;     // right channel, low byte
          buf[base + 3] = hi;     // right channel, high byte
        }
        line.write(buf, 0, buf.length);
        chron.blockWritten(musicalFrame);
        notifyPosition(chron.audibleFrame(line.getLongFramePosition()));
      }
      line.drain();
    } catch (LineUnavailableException e) {
      throw new RuntimeException(e);
    }
  }

  // start the synth on a daemon thread. returns the thread
  public synchronized Thread start() {
    if (!running) {
      running = true;
      synthThread = new Thread(this, "ChiptuneSynth");
      synthThread.setDaemon(true);
      synthThread.start();
    }
    return synthThread;
  }

  public synchronized void stop() {
    if (!running) {
      return;
    }
    running = false;
    Thread t = synthThread;
    synthThread = null;
    // wait for thread to actually finish its current frame and exit.
    if (t != null && t != Thread.currentThread()) {
      try {
        t.join(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
  
  public synchronized void rewind() {
    if (p1Track != null) p1Track.reset();
    if (p2Track != null) p2Track.reset();
    if (triTrack != null) triTrack.reset();
    if (noiTrack != null) noiTrack.reset();
    musicalFrame = 0;
  }

  // wrap the running frame counter into the song and tell the listeners;
  // the lead track's length is the canonical loop (the aligned invariant).
  // With a loop point set, the first pass covers [0, total) and every pass
  // after cycles [loopStart, total)  the head plays once, and the readout
  // says so honestly.
  private void notifyPosition(long audibleFrame) {
    if (listeners.isEmpty() || p1Track == null) {
      return;
    }
    int total = p1Track.totalFrames();
    if (total <= 0) {
      return;
    }
    int loopStart = p1Track.loopStartFrames();
    int span = total - loopStart;
    int frame;
    if (audibleFrame < total || loopStart <= 0 || span <= 0) {
      frame = (int) (((audibleFrame % total) + total) % total);
    } else {
      frame = loopStart + (int) ((audibleFrame - total) % span);
    }
    for (ChiptuneSynthListener l : listeners) {
      l.playbackPosition(frame, total);
    }
  }

  /**
   * Jump playback to a fraction (0..1) of the song  the dev "pan" control.
   * Each track chases its own state (see Track.seek), so a note sustaining
   * across the target keeps sounding instead of dropping to silence. The
   * channels are silenced first so nothing rings over from the old position;
   * the kick/snare/tom voices are left to their natural ~10-frame tails.
   *
   * @param fraction 0.0 = start of the loop, 1.0 wraps back to the start
   */
  public synchronized void seek(double fraction) {
    fraction = Math.max(0, Math.min(1, fraction));
    seekTrack(p1Track, fraction);
    seekTrack(p2Track, fraction);
    seekTrack(triTrack, fraction);
    seekTrack(noiTrack, fraction);
    if (p1Track != null) {
      // keep the position counter in step with where the tracks now are
      musicalFrame = (int) Math.round(fraction * p1Track.totalFrames());
    }
    p1.noteOff();
    p2.noteOff();
    tri.noteOff();
    noi.noteOff();
  }

  // fraction of each track's own length: identical for an aligned song, and
  // still sane for one whose channels disagree about the total
  private static void seekTrack(Track t, double fraction) {
    if (t != null) {
      t.seek((int) Math.round(fraction * t.totalFrames()));
    }
  }

  /**
   * Wall-clock length of one loop at the current speed and tempo scale, in
   * seconds. Lets a UI translate a seek fraction into a time readout that
   * matches what a stopwatch (or a YouTube timestamp) would say.
   */
  public double getDurationSeconds() {
    int frames = 0;
    if (p1Track != null) frames = Math.max(frames, p1Track.totalFrames());
    if (p2Track != null) frames = Math.max(frames, p2Track.totalFrames());
    if (triTrack != null) frames = Math.max(frames, triTrack.totalFrames());
    if (noiTrack != null) frames = Math.max(frames, noiTrack.totalFrames());
    double rate = FRAME_RATE * speed * tempoScale;   // musical frames per sec
    return rate <= 0 ? 0 : frames / rate;
  }

  public boolean isRunning() {
    return this.running;
  }

  public void setVolume(double sliderPosition) {
    sliderPosition = Math.max(0, Math.min(1, sliderPosition));
    double gain = (sliderPosition <= 0) ? 0 : Math.pow(10, (sliderPosition - 1) * 2);
    this.targetVolume = gain;
  }

  public void setSpeed(double speed) {
    this.speed = Math.max(0, speed);
  }
  
  public double getSpeed() { return this.speed; }

  public static ChiptuneSynth getSynthesizer(ChiptuneSong s) {
    ChiptuneSynth syn = new ChiptuneSynth()
            .setSong(s)
            .setLooping(true);
    return syn;
  }
}