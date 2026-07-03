package chiptunesynth;

/**
 * Observer of the synth's playback position. Notifications are grounded on
 * the audio DEVICE's clock (see SongChronometer), so the reported frame is
 * what is audible at the speakers right now — not where the sequencer has
 * rendered ahead to.
 *
 * Called from the synth's audio thread roughly 60 times per second. Keep
 * the implementation fast and non-blocking, and marshal to the Swing EDT
 * yourself (e.g. store volatiles and repaint from a timer).
 *
 * @author dylan
 */
public interface ChiptuneSynthListener {

  /**
   * The position now audible at the speakers.
   *
   * @param frame       musical frame within the song, 0 &le; frame &lt; total
   * @param totalFrames the song's loop length in frames
   */
  void playbackPosition(int frame, int totalFrames);
}
