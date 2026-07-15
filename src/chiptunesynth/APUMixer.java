package chiptunesynth;

/**
 * The NES 2A03 non-linear audio mixer.
 *
 * The real APU does NOT add its channels with a simple weighted sum. The five
 * channel DACs drive a resistor ladder whose output is non-linear: the two
 * pulses share one curve, and triangle/noise/DMC share another, and each curve
 * COMPRESSES as its inputs grow. Two pulses at full volume are noticeably less
 * than twice one pulse; a loud triangle "ducks" the noise sitting next to it.
 * A flat {@code p1*0.25 + p2*0.25 + ...} can't reproduce that, which is why a
 * dense passage can sound subtly wrong (too hot, wrong inner balance) even when
 * every note is right.
 *
 * nesdev gives the exact transfer functions and an efficient emulation using
 * two lookup tables populated once at construction
 * (https://www.nesdev.org/wiki/APU_Mixer):
 * <pre>
 *   pulse_table[n] = 95.52  / (8128.0  / n + 100)   n = 0..30  (= pulse1 + pulse2)
 *   tnd_table[n]   = 163.67 / (24329.0 / n + 100)   n = 0..202 (= 3*tri + 2*noise + dmc)
 *   output = pulse_table[pulse1 + pulse2]
 *          + tnd_table[3*triangle + 2*noise + dmc]
 * </pre>
 * This synth is FOUR channels: two pulses, triangle, noise. The DMC (the third
 * member of the tnd group) is omitted  it is unused in these tracks and rare
 * on NA carts  so the tnd index is just {@code 3*triangle + 2*noise}. The full
 * 203-entry curve is still built so the table matches the hardware exactly; we
 * simply never index its upper (DMC) reach.
 *
 * Channel inputs are DAC levels, 0..15. The output lands in roughly 0..1 and
 * carries a DC offset (the levels are unipolar), so the caller should DC-block
 * it before playback.
 *
 * Our channels emit continuous, band-limited samples rather than integer DAC
 * steps, so {@link #levelOf} recovers each one's instantaneous 0..15 level and
 * {@link #mix} interpolates the tables. The per-channel {@code gain*} fields are
 * the tuning knobs: 1.0 is NES-faithful, and nudging one re-balances that voice
 * while keeping the non-linear character intact.
 *
 * @author dylan
 */
public final class APUMixer {

  private final double[] pulseTable = new double[31];
  private final double[] tndTable = new double[203];

  // Tuning knobs: scale each channel's level going INTO the non-linear curve.
  // 1.0 = the hardware balance. >1 pushes that voice louder (and, because the
  // curve compresses, a touch more squashed); <1 pulls it back.
  public double gainP1 = 1.0;
  public double gainP2 = 1.0;
  public double gainTri = 1.0;
  public double gainNoise = 1.0;

  public APUMixer() {
    pulseTable[0] = 0.0;
    for (int n = 1; n < pulseTable.length; n++) {
      pulseTable[n] = 95.52 / (8128.0 / n + 100.0);
    }
    tndTable[0] = 0.0;
    for (int n = 1; n < tndTable.length; n++) {
      tndTable[n] = 163.67 / (24329.0 / n + 100.0);
    }
  }

  /**
   * Mix one sample from the four channel DAC levels (0..15 each). Returns the
   * raw APU output, ~0..1 with a DC offset  DC-block before playback.
   */
  public double mix(double p1, double p2, double triangle, double noise) {
    double pulseSum = clamp(p1 * gainP1 + p2 * gainP2, 0, 30);
    double tndIndex = clamp(3 * triangle * gainTri + 2 * noise * gainNoise,
        0, 202);
    return lookup(pulseTable, pulseSum) + lookup(tndTable, tndIndex);
  }

  /**
   * Recover a channel's instantaneous 0..15 DAC level from its band-limited
   * bipolar sample and the amplitude (envelope) that produced it. The sample is
   * {@code amp * wave} with wave in [-1, 1], so the unipolar level is
   * {@code 15 * amp * (wave + 1) / 2 = 7.5 * (sample + amp)}. Band-limiting can
   * overshoot the edges slightly, so the result is clamped.
   */
  public static double levelOf(double sample, double amplitude) {
    double level = 7.5 * (sample + amplitude);
    return level < 0 ? 0 : level > 15 ? 15 : level;
  }

  // linear-interpolate a table at a fractional index (the levels are continuous)
  private static double lookup(double[] table, double index) {
    int i = (int) index;
    if (i >= table.length - 1) {
      return table[table.length - 1];
    }
    return table[i] + (table[i + 1] - table[i]) * (index - i);
  }

  private static double clamp(double v, double lo, double hi) {
    return v < lo ? lo : v > hi ? hi : v;
  }
}
