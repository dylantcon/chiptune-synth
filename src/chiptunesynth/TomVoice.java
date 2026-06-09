/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chiptunesynth;

/**
 * A pitched tom voice. A drum tom is a tuned membrane, so this is a sine that
 * STARTS a fifth or so above the target pitch and bends down onto it (the
 * characteristic "dewww"), plus a very short LFSR noise burst for the stick
 * attack. The body rings noticeably longer than the snare.
 *
 * Unlike the snare/kick this one is pitched: trigger() takes the frequency of
 * the drum note, so Contra's C3 tom-roll fills land at the written pitch instead
 * of the flat, untuned hiss the bare noise channel gave them.
 *
 * trigger(freq, velocity)/tickFrame() run at 60 Hz; sample() at 44.1 kHz.
 *
 * @author dylan
 */
class TomVoice {

  private double phase = 0;
  private double freq = 0;
  private double target = 0;   // pitch the bend settles onto
  private double amp = 0;

  private int lfsr = 1;
  private double noisePhase = 0;
  private double noiseAmp = 0;

  private static final double START_RATIO    = 1.5;  // begin this far above pitch
  private static final double FREQ_FALL      = 0.80; // glide toward target/frame
  private static final double AMP_FALL       = 0.72; // resonant boom, ~9 frames
  private static final double NOISE_AMP_FALL = 0.40; // stick attack, ~3 frames
  private static final double NOISE_CLOCK_HZ = 6000;
  private static final double TONE_LEVEL  = 0.85;
  private static final double NOISE_LEVEL = 0.35;

  void trigger(double freqHz, double velocity) {
    this.target = freqHz;
    this.freq = freqHz * START_RATIO;
    this.amp = velocity;
    this.noiseAmp = velocity;
    this.phase = 0;
  }

  void tickFrame() {
    if (amp > 0) {
      // exponential glide of the pitch down onto the target
      freq = target + (freq - target) * FREQ_FALL;
      amp *= AMP_FALL;
      if (amp < 0.01) {
        amp = 0;
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
    if (amp > 0) {
      phase += freq / ChiptuneSynth.SAMPLE_RATE;
      if (phase >= 1.0) {
        phase -= 1.0;
      }
      out += Math.sin(2 * Math.PI * phase) * amp * TONE_LEVEL;
    }
    if (noiseAmp > 0) {
      noisePhase += NOISE_CLOCK_HZ / ChiptuneSynth.SAMPLE_RATE;
      while (noisePhase >= 1.0) {
        noisePhase -= 1.0;
        int feedback = (lfsr ^ (lfsr >> 1)) & 1;
        lfsr = (lfsr >> 1) | (feedback << 14);
      }
      out += (((lfsr & 1) == 0) ? 1.0 : -1.0) * noiseAmp * NOISE_LEVEL;
    }
    return out;
  }
}
