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

  public Track withDefaults(double vol, double duty) {
    this.defaultVolume = vol;
    this.defaultDuty = duty;
    return this;
  }

  // shift every subsequently-added pitch by this many semitones (rests are
  // untouched). used to correct a whole track that was transcribed in the
  // wrong octave — e.g. a MIDI rip an octave too high.
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
   * Total length of this track in frames — the sum of every note and rest.
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
   * The result is exactly {@code src.totalFrames()} long — the delay is
   * prepended as a rest and the tail truncated to fit — so lending it to a
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
   * sounding there — starting at the next event replaces a sustain with
   * silence. Our Notes are self-contained (volume/duty/decay/fx baked in at
   * addNotes time), so the chase is one note of lookbehind: find the note
   * spanning the target and let the sequencer re-trigger it with its
   * mid-note position intact, plus the last real pitch before it so a
   * portamento slide keeps its glide origin.
   *
   * One honest approximation: the channel's volume-decay envelope restarts
   * from full rather than partially faded (exact for SUSTAINED notes and
   * the triangle, which has no envelope). All pitch effects — vibrato, arp,
   * swell, slide, pitch-env — resume exactly, because they are computed
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

}
