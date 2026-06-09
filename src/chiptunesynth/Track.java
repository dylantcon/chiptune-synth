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

  void reset() {
    cursor = 0;
    framesLeft = 0;
    cur = null;
    elapsed = 0;
    started = false;
    prevMidi = Double.NaN;
    glideFrom = Double.NaN;
  }

}
