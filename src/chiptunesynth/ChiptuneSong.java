/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package chiptunesynth;


/**
 *
 * @author dylan
 */
public interface ChiptuneSong {

  /* === TEMPO / RHYTHM CONSTANTS === */
  public static final int BPM = 150;
  public static final int Q = 60 * ChiptuneSynth.FRAME_RATE / BPM;  // quarter (24)
  public static final int H = Q * 2;                                // half (48)
  public static final int W = Q * 4;                                // whole (96)
  public static final int E = Q / 2;                                // eighth (12)
  public static final int S = Q / 4;                                // sixteenth (6)
  public static final int T = Q / 8;                                // thirty-second (3)
  public static final int DQ = Q + E;                                // dotted quarter (36)
  public static final int DH = H + Q;                                // dotted half (72)
  public static final int DE = E + S;                                // dotted eighth (18)
  public static final int DS = S + T;                                // dotted sixteenth (9)
  
  public static final int SX = W / 6; // sixth (16)

  /* === PITCH CONSTANTS (MIDI note numbers) === */
  // octave 1 - extreme bass (rare, but possible on triangle)
  public static final int C1 = 24, CS1 = 25, D1 = 26, DS1 = 27;
  public static final int E1 = 28, F1 = 29, FS1 = 30, G1 = 31;
  public static final int GS1 = 32, A1 = 33, AS1 = 34, B1 = 35;

  // octave 2 - deep bass on triangle
  public static final int C2 = 36, CS2 = 37, D2 = 38, DS2 = 39;
  public static final int E2 = 40, F2 = 41, FS2 = 42, G2 = 43;
  public static final int GS2 = 44, A2 = 45, AS2 = 46, B2 = 47;

  // octave 3 - bass and low harmony
  public static final int C3 = 48, CS3 = 49, D3 = 50, DS3 = 51;
  public static final int E3 = 52, F3 = 53, FS3 = 54, G3 = 55;
  public static final int GS3 = 56, A3 = 57, AS3 = 58, B3 = 59;

  // octave 4 - harmony, lower melody
  public static final int C4 = 60, CS4 = 61, D4 = 62, DS4 = 63;
  public static final int E4 = 64, F4 = 65, FS4 = 66, G4 = 67;
  public static final int GS4 = 68, A4 = 69, AS4 = 70, B4 = 71;

  // octave 5 - main melody range
  public static final int C5 = 72, CS5 = 73, D5 = 74, DS5 = 75;
  public static final int E5 = 76, F5 = 77, FS5 = 78, G5 = 79;
  public static final int GS5 = 80, A5 = 81, AS5 = 82, B5 = 83;

  // octave 6 - high decoration
  public static final int C6 = 84, CS6 = 85, D6 = 86, DS6 = 87;
  public static final int E6 = 88, F6 = 89, FS6 = 90, G6 = 91;
  public static final int GS6 = 92, A6 = 93, AS6 = 94, B6 = 95;

  // octave 7 - extreme highs (used for cymbal noise pitch)
  public static final int C7 = 96;

  /* === DRUM NOISE PITCHES === */
  // These act as frequency selectors for the noise channel - chosen for
  // perceptual contrast rather than musical pitch. Wider spread = more
  // distinguishable kick/snare/hihat.
  public static final int KICK = 36;   // = C2  - low rumble
  public static final int SNARE = 60;   // = C4  - sharp crack
  public static final int HIHAT = 84;   // = C6  - crisp tick
  public static final int CYMBAL = 96;   // = C7  - bright wash

  /* === RAW NES NOISE PERIODS === */
  // These do NOT hit the synthesized kick/snare voices - they clock the real
  // 15-bit LFSR at one of the 2A03's 16 hardware noise periods, the authentic
  // way NES percussion was made. A note's period index is midi - NZ_BASE.
  // NZ0 is the brightest hiss (period 4), NZ15 the darkest rumble (period
  // 4068). The 128..143 range sits above every real pitch and clear of the
  // KICK/SNARE/HIHAT/CYMBAL selectors, so it can never collide.
  public static final int NZ_BASE = 128;
  public static final int NZ0 = 128, NZ1 = 129, NZ2 = 130, NZ3 = 131;
  public static final int NZ4 = 132, NZ5 = 133, NZ6 = 134, NZ7 = 135;
  public static final int NZ8 = 136, NZ9 = 137, NZ10 = 138, NZ11 = 139;
  public static final int NZ12 = 140, NZ13 = 141, NZ14 = 142, NZ15 = 143;

  /**
   * True if a noise-channel midi value encodes a raw NES noise period
   * (NZ0..NZ15) rather than a drum selector or pitched note. Keeps the
   * synth's dispatch a one-liner and keeps the 128..143 encoding owned here,
   * next to the constants that define it.
   *
   * @param midi a note's midi value
   * @return whether it decodes to a raw noise period
   */
  public static boolean isRawNoise(int midi) {
    return midi >= NZ_BASE && midi < NZ_BASE + 16;
  }

  /* === SPECIAL: REST === */
  public static final int R = -1;

  /* === VOICE MIXING DEFAULTS === */
  public static final double LEAD_VOL = 0.702;
  public static final double LEAD_DUTY = 0.203;   // classic NES square lead
  public static final double HARMONY_VOL = 0.492;
  public static final double HARMONY_DUTY = 0.491;   // fuller for inner voice
  public static final double BASS_VOL = 1.0;
  public static final double BASS_DUTY = 0.50;    // ignored by triangle
  public static final double DRUM_VOL = 0.685;
  public static final double DRUM_DUTY = 0.50;    // ignored by noise

  /* === SUSTAIN RELATED CONSTANTS === */
  public static final double SOSTENUTO = 0.0;   // hold at full volume
  public static final double TENUTO = 0.205;   // pad-like, slow decline
  public static final double LEGATO = 0.535;   // current default
  public static final double STACCATO = 1.565;   // sharp percussive cut
  public static final double PORTATO = LEGATO + STACCATO / 2;
  public static final double STACCATISSIMO = 6.0;

  // for NES-native sound channel without in-house drum kit
  public static final double NOISE_DECAY = 18.0;

  /* === METHODS EACH SONG IMPLEMENTS === */
  Track getLead();

  Track getHarmony();

  Track getBass();

  Track getDrums();

  /**
   * Per-song playback-rate correction. The synth multiplies this on top of
   * the gameplay speed (level scaling / game-over slowdown), so 1.0 means
   * "play the frame durations exactly as written" and the gameplay knob is
   * untouched. Songs whose frame data was generated from a MIDI with an
   * imperfect frames-per-second assumption override this to pull their loop
   * length back onto the original recording.
   *
   * @return rate multiplier; &gt;1.0 plays faster, &lt;1.0 slower
   */
  default double getTempoScale() {
    return 1.0;
  }

  /**
   * Frame the song re-enters when its tracks wrap around  the NSF-style
   * loop point. 0 (the default) loops the whole song. A song with a
   * play-once head (an intro) returns the head's length in frames; the
   * synth applies the same point to all four tracks, so the channels jump
   * together by construction and the aligned-totals invariant carries over
   * unchanged. The point must land on a note boundary in every track 
   * build the head from whole bars and this holds automatically (a
   * mid-note point snaps forward to the next boundary, see
   * Track.loopCursor).
   *
   * @return loop re-entry point in frames from the top of the song
   */
  default int getLoopStartFrames() {
    return 0;
  }
}
