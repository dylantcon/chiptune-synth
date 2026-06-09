/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chiptunesynth;

/**
 * A dedicated snare voice that LAYERS two components, the way a real drum (and a
 * DPCM snare sample) does:
 *
 *   - a short pitched "crack" (a sine that bends ~220 Hz down to ~170 Hz) for
 *     the tonal body / attack thwack, and
 *   - an LFSR noise "sizzle" for the wires rattling, rung a touch longer.
 *
 * The plain noise channel can only do the second half, which is why a one-burst
 * snare sounds thin next to Konami's sampled Contra snare. Splitting it into its
 * own voice — exactly like KickVoice — lets the snare cut through a dense
 * arrangement without stealing the noise channel from the hats.
 *
 * trigger(velocity)/tickFrame() are driven by the synth's 60 Hz sequencer;
 * sample() runs at the 44.1 kHz audio rate. Velocity scales both layers so a
 * song can accent the backbeat.
 *
 * @author dylan
 */
class SnareVoice {

  // tonal "crack" layer
  private double tonePhase = 0;
  private double toneFreq = 0;
  private double toneAmp = 0;

  // LFSR noise "sizzle" layer (own shift register so it never fights the hats)
  private int lfsr = 1;
  private double noisePhase = 0;
  private double noiseAmp = 0;

  // tuned by ear: tight, cracky, ~7-frame tail.
  private static final double TONE_START_HZ = 220.0;
  private static final double TONE_FLOOR_HZ = 170.0;
  private static final double TONE_FALL     = 0.78;  // pitch bend per frame
  private static final double TONE_AMP_FALL = 0.45;  // crack dies fast
  private static final double NOISE_AMP_FALL = 0.62; // sizzle rings a bit longer
  private static final double NOISE_CLOCK_HZ = 7000; // bright broadband hiss
  private static final double TONE_LEVEL  = 0.55;    // internal layer balance
  private static final double NOISE_LEVEL = 0.65;

  void trigger(double velocity) {
    this.toneFreq = TONE_START_HZ;
    this.toneAmp = velocity;
    this.noiseAmp = velocity;
    this.tonePhase = 0;
  }

  // advance both envelopes (and the pitch bend) one 60 Hz frame
  void tickFrame() {
    if (toneAmp > 0) {
      toneFreq = Math.max(TONE_FLOOR_HZ, toneFreq * TONE_FALL);
      toneAmp *= TONE_AMP_FALL;
      if (toneAmp < 0.01) {
        toneAmp = 0;
      }
    }
    if (noiseAmp > 0) {
      noiseAmp *= NOISE_AMP_FALL;
      if (noiseAmp < 0.01) {
        noiseAmp = 0;
      }
    }
  }

  double sample() {
    double out = 0;
    if (toneAmp > 0) {
      tonePhase += toneFreq / ChiptuneSynth.SAMPLE_RATE;
      if (tonePhase >= 1.0) {
        tonePhase -= 1.0;
      }
      out += Math.sin(2 * Math.PI * tonePhase) * toneAmp * TONE_LEVEL;
    }
    if (noiseAmp > 0) {
      noisePhase += NOISE_CLOCK_HZ / ChiptuneSynth.SAMPLE_RATE;
      while (noisePhase >= 1.0) {
        noisePhase -= 1.0;
        int feedback = (lfsr ^ (lfsr >> 1)) & 1;   // long-mode taps (bits 0,1)
        lfsr = (lfsr >> 1) | (feedback << 14);
      }
      double n = ((lfsr & 1) == 0) ? 1.0 : -1.0;
      out += n * noiseAmp * NOISE_LEVEL;
    }
    return out;
  }
}
