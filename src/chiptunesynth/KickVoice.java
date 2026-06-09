/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chiptunesynth;

/**
 * A dedicated kick-drum voice: a sine whose pitch sweeps fast from ~150 Hz
 * down to ~40 Hz while its amplitude decays just as fast. That downward
 * "boom" sweep is exactly how the NES faked a kick — Battletoads used the
 * triangle channel (or DPCM) for it. Spectral analysis of the original Surf
 * City showed ~25% of the energy is sub-150 Hz; the noise channel has no
 * tonal low end and physically cannot produce that thud, which is why our
 * drums sounded shallow.
 *
 * It is its own voice rather than borrowing the triangle so the busy bass
 * line is never interrupted.
 *
 * trigger()/tickFrame() are driven by the synth's 60 Hz sequencer; sample()
 * runs at the 44.1 kHz audio rate.
 *
 * @author dylan
 */
class KickVoice {

  private double phase = 0;
  private double freq = 0;
  private double amp = 0;

  // tuned by ear against the reference: punchy, sub-heavy, ~8-frame tail.
  private static final double START_HZ = 150.0;
  private static final double FLOOR_HZ = 40.0;
  private static final double PITCH_FALL = 0.74;  // freq *= this each frame
  private static final double AMP_FALL = 0.60;    // amp  *= this each frame

  void trigger() {
    this.freq = START_HZ;
    this.amp = 1.0;
    this.phase = 0;
  }

  // advance the pitch/amp envelopes one 60 Hz frame
  void tickFrame() {
    if (amp <= 0) {
      return;
    }
    freq = Math.max(FLOOR_HZ, freq * PITCH_FALL);
    amp *= AMP_FALL;
    if (amp < 0.01) {
      amp = 0;
    }
  }

  double sample() {
    if (amp <= 0) {
      return 0;
    }
    phase += freq / ChiptuneSynth.SAMPLE_RATE;
    if (phase >= 1.0) {
      phase -= 1.0;
    }
    return Math.sin(2 * Math.PI * phase) * amp;
  }
}
