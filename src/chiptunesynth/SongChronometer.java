package chiptunesynth;

/**
 * The playback clock  maps the audio device's sample position onto the
 * sequencer's musical position.
 *
 * Two truths this class encodes:
 *
 * 1. WALL-CLOCK TIME INSIDE THE RENDER LOOP IS NOT THE SONG POSITION. The
 *    loop renders a 735-sample block in microseconds and then blocks in
 *    line.write() when the buffer is full, so the sequencer runs up to the
 *    buffer depth (~67 ms here) AHEAD of what the ears hear. Timing loop
 *    iterations would characterize the buffer, not the music.
 *
 * 2. THE AUDIO DEVICE IS THE ONLY CLOCK THAT MATTERS. The sound card
 *    consumes samples at exactly its rate; SourceDataLine's
 *    getLongFramePosition() reports how many it has actually played. This
 *    class keeps a small ring buffer recording which musical frame each
 *    written block corresponds to, so a device position can be looked up
 *    and answered with "this is the musical frame you are HEARING".
 *
 * All accounting is integer  samples and frames are counted, never summed
 * as fractional seconds  which is fixed-point arithmetic with the radix
 * parked at one block / one frame: zero accumulated error by construction.
 * The one irreducible float in the synth (the speed knob's fractional
 * accumulator) affects how fast frames are consumed, not this bookkeeping
 * of which frame went into which block. Precision is therefore bounded by
 * physics, not arithmetic: one block = 1/60 s.
 *
 * A pleasing consequence of device-clock grounding: right after a seek, the
 * reported position keeps walking through the OLD location for the ~4
 * blocks still queued in the buffer  because that is genuinely what is
 * still coming out of the speakers.
 *
 * One instance per audio-thread run (the line, and thus its sample counter,
 * is recreated each run).
 *
 * @author dylan
 */
final class SongChronometer {

  // ring of "musical frame sounding in block b" for the last MAP blocks;
  // far deeper than the 4-block line buffer it needs to cover
  private static final int MAP = 64;

  private final long[] frameAtBlock = new long[MAP];
  private long blocksWritten = 0;

  SongChronometer(long musicalFrameNow) {
    for (int i = 0; i < MAP; ++i) {
      frameAtBlock[i] = musicalFrameNow;
    }
  }

  /** Record that the block just written carries this musical frame. */
  void blockWritten(long musicalFrame) {
    frameAtBlock[(int) (blocksWritten % MAP)] = musicalFrame;
    blocksWritten++;
  }

  /**
   * The musical frame audible when the device has played this many samples.
   *
   * @param deviceSamplePos SourceDataLine.getLongFramePosition()
   * @return the musical frame that block of audio carried
   */
  long audibleFrame(long deviceSamplePos) {
    if (blocksWritten == 0) {
      return frameAtBlock[0];
    }
    long block = deviceSamplePos / ChiptuneSynth.SAMPLES_PER_FRAME;
    long oldest = Math.max(0, blocksWritten - MAP);
    if (block >= blocksWritten) {
      block = blocksWritten - 1;      // device can't be ahead of the writer
    } else if (block < oldest) {
      block = oldest;                 // ring no longer remembers older blocks
    }
    return frameAtBlock[(int) (block % MAP)];
  }
}
