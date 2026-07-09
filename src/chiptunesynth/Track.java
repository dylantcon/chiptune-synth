/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chiptunesynth;

import java.util.ArrayList;
import java.util.List;

// a monophonic sequence of notes for one channel
public class Track {

  public final List<Note> notes = new ArrayList<>();
  int cursor = 0;
  int framesLeft = 0;

  // live playback state the synth's per-frame modulation tick reads/writes
  Note cur = null;                 // note currently sounding on this track
  int elapsed = 0;                 // frames since cur started (0 on frame 1)
  boolean started = false;         // true only on the frame cur (re)triggered
  double prevMidi = Double.NaN;    // last real pitch (slide source / memory)
  double glideFrom = Double.NaN;   // portamento origin for the current note
  int seekElapsed = 0;             // seek(): resume this far into the note

  private double defaultVolume = 0.7;
  private double defaultDuty = 0.25;
  private double currentDecay = 0.6;        // state for subsequent addNotes
  private Effect currentEffect = Effect.NONE;  // ditto, for pitch modulation
  private int transpose = 0;                // semitones added to every pitch

  // frame the track re-enters when it wraps  0 loops the whole track.
  // Package-private on purpose: the synth stamps it onto all four tracks
  // from ChiptuneSong.getLoopStartFrames(), so a song can never give its
  // channels different loop points (they would drift apart within one wrap).
  private int loopStartFrame = 0;
  private int loopCursorCache = -1;         // note index at loopStartFrame
  private int loopCacheSize = -1;           // notes.size() when cached

  public Track withDefaults(double vol, double duty) {
    this.defaultVolume = vol;
    this.defaultDuty = duty;
    return this;
  }

  // shift every subsequently-added pitch by this many semitones (rests are
  // untouched). used to correct a whole track that was transcribed in the
  // wrong octave  e.g. a MIDI rip an octave too high.
  public Track withTranspose(int semitones) {
    this.transpose = semitones;
    return this;
  }

  public Track withDecay(double decay) {
    this.currentDecay = decay;
    return this;
  }

  // change the volume applied to every subsequent addNotes (per-hit velocity
  // for drums, dynamic accents for melodic voices). mirrors withDecay.
  public Track withVolume(double vol) {
    this.defaultVolume = vol;
    return this;
  }

  // change the pulse duty applied to every subsequent addNotes. lets one track
  // switch timbre mid-stream (e.g. a borrowed pad voice using the fuller 0.5
  // duty instead of the thin lead duty). mirrors withVolume.
  public Track withDuty(double duty) {
    this.defaultDuty = duty;
    return this;
  }

  /* === pitch-modulation builders: apply to every subsequent addNotes(...),
     exactly like withDecay. They mutate the "current effect" the same way,
     so songs read top-to-bottom. === */

  public Track withArp(int... semis) {
    this.currentEffect = currentEffect.withArp(semis);
    return this;
  }

  public Track withArpSpeed(int framesPerStep) {
    this.currentEffect = currentEffect.withArpSpeed(framesPerStep);
    return this;
  }

  public Track withVibrato(double semis, double hz, int delayFrames) {
    this.currentEffect = currentEffect.withVibrato(semis, hz, delayFrames);
    return this;
  }

  public Track withSlide(int frames) {
    this.currentEffect = currentEffect.withSlide(frames);
    return this;
  }

  public Track withPitchEnv(double startSemis, int frames) {
    this.currentEffect = currentEffect.withPitchEnv(startSemis, frames);
    return this;
  }

  public Track withSwell() {
    this.currentEffect = currentEffect.withSwell();
    return this;
  }

  /** Drop all pitch modulation for subsequently added notes. */
  public Track withNoFx() {
    this.currentEffect = Effect.NONE;
    return this;
  }

  public Track add(Note n) {
    notes.add(n);
    return this;
  }

  /**
   * add notes as alternating (midi, duration) pairs. Use -1 for midi to encode
   * a rest.
   *
   * @author dylan
   * @param pitchesAndDurations alternating pitch, duration pairs
   * @return a list of Note objects in the form of a Track
   */
  public Track addNotes(int... pitchesAndDurations) {
    if (pitchesAndDurations.length % 2 != 0) {
      throw new IllegalArgumentException(
      "addNotes requires alternating pitch, duration pairs");
    }
    for (int i = 0; i < pitchesAndDurations.length; i += 2) {
      int midi = pitchesAndDurations[i];
      int dur = pitchesAndDurations[i + 1];
      if (midi >= 0) {
        midi += transpose;   // rests (-1) stay rests
      }
      notes.add(new Note(midi, dur, defaultVolume, defaultDuty,
              currentDecay, currentEffect));
    }
    return this;
  }
  
  /**
   * Add a register-log passage: each row is {pitch, frames, volume 0-15}.
   * This is the natural shape of NSF capture data, where the volume column
   * is part of the music (echo gates, painted envelopes, velocity layers) 
   * one row per register state instead of a withVolume line per change.
   * Volume 0 or a negative pitch is silence; the volume scale (volUnit,
   * typically songVolume / 15) converts hardware volume steps to amplitude.
   * The track's volume state is left at the last row's level.
   *
   * @param volUnit amplitude per hardware volume step (vol 0-15 * volUnit)
   * @param segs    rows of {pitch, frames, volume}
   * @return this track
   */
  public Track addSegs(double volUnit, int[][] segs) {
    for (int[] s : segs) {
      if (s.length != 3) {
        throw new IllegalArgumentException(
            "addSegs rows are {pitch, frames, volume}");
      }
      if (s[0] < 0 || s[2] == 0) {
        addNotes(-1, s[1]);
      } else {
        withVolume(s[2] * volUnit);
        addNotes(s[0], s[1]);
      }
    }
    return this;
  }

  /**
   * Add a chromatic run from one pitch to another (inclusive), each step
   * lasting the same number of frames  the tracker-driver "rip" that
   * sweeps into a target note. Direction follows the endpoints.
   *
   * @param from          first pitch of the run
   * @param to            last pitch of the run (inclusive)
   * @param framesPerStep duration of every step
   * @return this track
   */
  public Track addRun(int from, int to, int framesPerStep) {
    int step = (to >= from) ? 1 : -1;
    for (int p = from; p != to + step; p += step) {
      addNotes(p, framesPerStep);
    }
    return this;
  }

  /**
   * add notes from an existing track. append new notes to end of CO track
   *
   * @author dylan
   * @param anotherTrack another Track whose notes are appended to this Track
   * @return the new Track object with the parameter Track's notes at the end
   */
  public Track addNotes(Track anotherTrack) {
    for (Note note : anotherTrack.notes) {
      this.add(note);
    }
    return this;
  }

  /**
   * Total length of this track in frames  the sum of every note and rest.
   * The synth loops each channel's track independently, so all four tracks of
   * a song MUST sum to the same total or the channels drift apart from the
   * second loop onward. Songs (and the test runner) can check alignment
   * cheaply with this.
   *
   * @return total duration in frames
   */
  public int totalFrames() {
    int sum = 0;
    for (Note n : notes) {
      sum += n.durationFrames;
    }
    return sum;
  }

  /**
   * The David Wise echo: a copy of {@code src} delayed by {@code delayFrames},
   * with every note's volume scaled by {@code volScale} and re-voiced at
   * {@code duty} (thinner reads as "reflection" rather than "double"). This is
   * the second-pulse trick Wise (and the Follins) leaned on constantly: the
   * ear hears one melody with studio delay instead of two square waves.
   *
   * The result is exactly {@code src.totalFrames()} long  the delay is
   * prepended as a rest and the tail truncated to fit  so lending it to a
   * channel never breaks bar alignment. If the source's last phrase must echo
   * past its section boundary, the caller owns that splice.
   *
   * @param src         the track to echo (typically a lead section)
   * @param delayFrames echo delay; 2-3 sixteenths is the classic feel
   * @param volScale    multiplier applied to each note's volume
   * @param duty        pulse duty for the echo voice
   * @return a new same-length Track containing the delayed copy
   */
  public static Track echoOf(Track src, int delayFrames, double volScale,
                             double duty) {
    Track t = new Track();
    int budget = src.totalFrames();
    if (delayFrames > 0) {
      int d = Math.min(delayFrames, budget);
      t.add(Note.rest(d));
      budget -= d;
    }
    for (Note n : src.notes) {
      if (budget <= 0) {
        break;
      }
      int d = Math.min(n.durationFrames, budget);
      t.add(new Note(n.midi, d, n.volume * volScale, duty, n.decay, n.fx));
      budget -= d;
    }
    return t;
  }

  /**
   * Position this track as if it had just played {@code frame} frames from
   * the top, so playback resumes there seamlessly (the "pan" control). The
   * frame is wrapped into the track's length.
   *
   * The classic seek problem is state chase: you cannot simply start at the
   * next event, because a note that BEGAN before the target may still be
   * sounding there  starting at the next event replaces a sustain with
   * silence. Our Notes are self-contained (volume/duty/decay/fx baked in at
   * addNotes time), so the chase is one note of lookbehind: find the note
   * spanning the target and let the sequencer re-trigger it with its
   * mid-note position intact, plus the last real pitch before it so a
   * portamento slide keeps its glide origin.
   *
   * One honest approximation: the channel's volume-decay envelope restarts
   * from full rather than partially faded (exact for SUSTAINED notes and
   * the triangle, which has no envelope). All pitch effects  vibrato, arp,
   * swell, slide, pitch-env  resume exactly, because they are computed
   * from the elapsed counter this method restores.
   *
   * @param frame target position in frames, wrapped into [0, totalFrames)
   */
  public void seek(int frame) {
    reset();
    int total = totalFrames();
    if (total <= 0) {
      return;
    }
    frame = ((frame % total) + total) % total;
    int at = 0;
    double before = Double.NaN;      // last real pitch before the target
    for (int i = 0; i < notes.size(); ++i) {
      Note n = notes.get(i);
      int end = at + n.durationFrames;
      if (frame < end) {
        cursor = i;                  // next advance re-triggers this note...
        seekElapsed = frame - at;    // ...resuming this far into it
        prevMidi = before;           // chased slide origin
        return;
      }
      if (n.midi >= 0) {
        before = n.midi;
      }
      at = end;
    }
  }

  void reset() {
    cursor = 0;
    framesLeft = 0;
    cur = null;
    elapsed = 0;
    started = false;
    prevMidi = Double.NaN;
    glideFrom = Double.NaN;
    seekElapsed = 0;
  }

  void setLoopStart(int frame) {
    this.loopStartFrame = Math.max(0, frame);
    this.loopCursorCache = -1;
  }

  /** Loop re-entry point in frames; 0 = the whole track loops. */
  public int loopStartFrames() {
    return loopStartFrame;
  }

  /**
   * Note index the sequencer jumps to when the track wraps. Resolved from
   * {@link #loopStartFrame} by walking note boundaries; a point that lands
   * inside a note snaps FORWARD to the next boundary (build the head from
   * whole bars and the point is a boundary by construction). Cached  the
   * walk is O(notes) and a wrap happens once per loop.
   */
  int loopCursor() {
    if (loopStartFrame <= 0) {
      return 0;
    }
    if (loopCursorCache >= 0 && loopCacheSize == notes.size()) {
      return loopCursorCache;
    }
    int at = 0;
    int idx = 0;
    for (int i = 0; i < notes.size(); ++i) {
      if (at >= loopStartFrame) {
        idx = i;
        break;
      }
      at += notes.get(i).durationFrames;
    }
    loopCursorCache = idx;
    loopCacheSize = notes.size();
    return idx;
  }

}
