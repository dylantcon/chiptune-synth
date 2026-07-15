/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chiptunesynth;

/**
 * Noise channel using a 15 bit linear feedback shift register, the same
 * design that the Nintendo Entertainment System uses.
 *
 * in "long" mode (def) the feedback bit is XOR of bits 0 and 1, giving
 * broadband noise. in "short" mode it's bits 0 and 6, which shortens the
 * period to 93 samples and produces a metallic pitched tone.
 *
 * @author dylan
 */
class NoiseChannel {

  int lfsr = 1;
  double phaseAcc = 0;
  double clockHz = 0;
  double envelope = 0;
  double envelopeDecay = 0;
  boolean shortMode = false;
  double lastAmp = 0;          // envelope used for the most recent sample()

  // NTSC 2A03 noise-period table: the 16 values the 4-bit period register
  // selects, in CPU cycles per LFSR clock. Index 0 (period 4) is the fastest
  // clock  the brightest/highest hiss; index 15 (period 4068) is the slowest,
  // darkest rumble. This is the real hardware, so it lives with the LFSR.
  static final int[] NES_PERIODS =
      {4, 8, 16, 32, 64, 96, 128, 160, 202, 254, 380, 508, 762, 1016, 2034, 4068};
  static final double CPU_HZ = 1789773.0;   // NTSC 2A03 clock

  // The raw LFSR is full-bandwidth and hits the ear like a spray bottle. On
  // real hardware it never does: the 2A03's analog output (an RC stage, then
  // the TV speaker) rolls the top octaves off. Two cascaded one-poles model
  // that with a 12 dB/oct roll-off, which is what it takes to actually tame
  // the BRIGHT hits (the snare/kick sweep, whose LFSR clock runs past 100 kHz)
  // into a drum - a single pole barely dents them. The dark hats already sit
  // under the cutoff, so they stay crisp. Raise the cutoff for more sizzle,
  // lower it for more warmth. It only trims the wave's peaks, so amplitude()
  // still reports the true envelope for the APU mixer.
  static final double LP_CUTOFF_HZ = 6000.0;
  private static final double LP_ALPHA = lpAlpha(LP_CUTOFF_HZ);
  private double lp1 = 0;      // two-stage cascade
  private double lp2 = 0;

  private static double lpAlpha(double cutoffHz) {
    double rc = 1.0 / (2.0 * Math.PI * cutoffHz);
    double dt = 1.0 / ChiptuneSynth.SAMPLE_RATE;
    return dt / (rc + dt);
  }

  double amplitude() {
    return lastAmp;
  }

  void noteOn(double clockRate, double volume, double decayPerSec, boolean shortMode) {
    this.clockHz = clockRate;
    this.envelope = volume;
    this.envelopeDecay = decayPerSec * volume / ChiptuneSynth.SAMPLE_RATE;
    this.shortMode = shortMode;
  }

  /**
   * Strike the LFSR at authentic NES noise period {@code index} (0..15), the
   * way the cartridge did  the period selects a clock rate off
   * {@link #NES_PERIODS}, no equal-tempered pitch involved. This is the voice
   * behind the raw-noise drum path (claps, ticks, snare sweeps).
   *
   * @param index       4-bit period register value; clamped to 0..15
   * @param volume      initial envelope level
   * @param decayPerSec linear fade rate (larger = shorter/percussive)
   * @param shortMode   true for the 93-step metallic mode, false for broadband
   */
  void noteOnPeriod(int index, double volume, double decayPerSec, boolean shortMode) {
    int p = Math.max(0, Math.min(NES_PERIODS.length - 1, index));
    noteOn(CPU_HZ / NES_PERIODS[p], volume, decayPerSec, shortMode);
  }

  void noteOff() {
    this.envelope = 0;
  }

  double sample() {
    if (envelope <= 0 || clockHz <= 0) {
      lastAmp = 0;
      lp1 = 0;                     // filter clears while the voice is idle
      lp2 = 0;
      return 0;
    }
    phaseAcc += clockHz / ChiptuneSynth.SAMPLE_RATE;
    while (phaseAcc >= 1.0) {
      phaseAcc -= 1.0;
      int bit0 = lfsr & 1;
      int otherBit = shortMode ? (lfsr >> 6) & 1 : (lfsr >> 1) & 1;
      int feedback = bit0 ^ otherBit;
      lfsr = (lfsr >> 1) | (feedback << 14);
    }
    lastAmp = envelope;
    double s = ((lfsr & 1) == 0 ? 1.0 : -1.0) * envelope;
    lp1 += LP_ALPHA * (s - lp1);           // analog-output roll-off,
    lp2 += LP_ALPHA * (lp1 - lp2);         // two poles = 12 dB/oct
    envelope = Math.max(0, envelope - envelopeDecay);
    return lp2;
  }
  
}
