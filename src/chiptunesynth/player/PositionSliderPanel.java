package chiptunesynth.player;

import chiptunesynth.ChiptuneSynthListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * The live position control: a custom-painted scrubber that follows
 * playback in real time and seeks on release. No JSlider — its integer
 * model and chunky thumb geometry fight both live-following and pixel-wise
 * scrubbing, so this paints its own track and handles its own mouse.
 *
 * HONEST PRECISION, by design: the readout leads with the musical FRAME —
 * the one value that is exact by construction (integer sequencer steps,
 * grounded on the audio device's sample clock by SongChronometer). Percent
 * and seconds are shown to one decimal each, at or below the true
 * resolution of one frame (~17 ms at full speed). No digit on this readout
 * claims knowledge the measurement doesn't have.
 *
 * Threading: playbackPosition() arrives ~60x/sec on the synth's audio
 * thread and only stores volatiles; a 33 ms Swing timer repaints from the
 * EDT, skipping frames the audio thread outpaces (coalescing, not queuing).
 * While the user is scrubbing, live updates keep arriving but the display
 * shows the drag target; releasing seeks and hands the display back to
 * playback.
 *
 * @author dylan
 */
public class PositionSliderPanel extends AbstractSliderPanel
        implements ChiptuneSynthListener {

  /** Renders the readout for a position. total &lt;= 0 means "no song". */
  public interface PositionFormatter {
    String format(int frame, int totalFrames);
  }

  /** Receives the seek target when a scrub is released. */
  public interface SeekHandler {
    void seekTo(double fraction);
  }

  private final PositionFormatter fmt;
  private SeekHandler onSeek;

  // written by the audio thread, read by the EDT timer. liveDirty makes
  // updates EDGE-triggered: the timer consumes each callback exactly once,
  // so when playback is paused (no callbacks) the last-known value can
  // never snap the display back after a scrub or rewind repositions it.
  private volatile int liveFrame = 0;
  private volatile int liveTotal = 0;
  private volatile boolean liveDirty = false;

  private final Scrubber scrubber = new Scrubber();
  private boolean scrubbing = false;
  private double shownFraction = 0;
  private int shownFrame = -1;          // last frame rendered, to skip no-ops

  public PositionSliderPanel(PositionFormatter fmt) {
    this.fmt = fmt;
    add(scrubber, BorderLayout.NORTH);
    refreshReadout();
    new Timer(33, e -> followPlayback()).start();
  }

  public void setSeekHandler(SeekHandler handler) {
    this.onSeek = handler;
  }

  /** External reset (e.g. the Rewind button). */
  public void setFraction(double fraction) {
    shownFraction = Math.max(0, Math.min(1, fraction));
    shownFrame = -1;
    refreshReadout();
    scrubber.repaint();
  }

  /* ---- ChiptuneSynthListener (audio thread) ---- */

  @Override
  public void playbackPosition(int frame, int totalFrames) {
    liveFrame = frame;
    liveTotal = totalFrames;
    liveDirty = true;                 // publish last, consume once
  }

  /* ---- EDT side ---- */

  private void followPlayback() {
    if (!liveDirty || scrubbing) {
      return;
    }
    liveDirty = false;                // consume: stale values never re-apply
    int total = liveTotal;
    int frame = liveFrame;
    if (total <= 0 || frame == shownFrame) {
      return;
    }
    shownFrame = frame;
    shownFraction = frame / (double) total;
    setReadoutText(fmt.format(frame, total));
    scrubber.repaint();
  }

  @Override
  public final void refreshReadout() {
    int total = liveTotal;
    setReadoutText(fmt.format(
        (int) Math.round(shownFraction * Math.max(total, 0)), total));
  }

  /** The custom-painted track: groove, progress fill, thumb line. */
  private class Scrubber extends JComponent {

    Scrubber() {
      setPreferredSize(new Dimension(120, 26));
      MouseAdapter mouse = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
          scrubbing = true;
          scrubTo(e.getX());
        }

        @Override
        public void mouseDragged(MouseEvent e) {
          scrubTo(e.getX());
        }

        @Override
        public void mouseReleased(MouseEvent e) {
          scrubbing = false;
          shownFrame = -1;               // let playback repaint immediately
          if (onSeek != null) {
            onSeek.seekTo(shownFraction);
          }
        }
      };
      addMouseListener(mouse);
      addMouseMotionListener(mouse);
    }

    private void scrubTo(int x) {
      int w = Math.max(1, getWidth() - 1);
      shownFraction = Math.max(0, Math.min(1, x / (double) w));
      refreshReadout();
      repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g;
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
              RenderingHints.VALUE_ANTIALIAS_ON);
      int w = getWidth(), h = getHeight();
      int grooveY = h / 2 - 3, grooveH = 6;
      // groove
      g2.setColor(Color.LIGHT_GRAY);
      g2.fillRoundRect(0, grooveY, w, grooveH, grooveH, grooveH);
      // progress fill
      int fill = (int) Math.round(shownFraction * w);
      g2.setColor(scrubbing ? ACCENT.brighter() : ACCENT);
      g2.fillRoundRect(0, grooveY, fill, grooveH, grooveH, grooveH);
      // thumb
      int x = Math.min(w - 2, Math.max(1, fill));
      g2.setColor(ACCENT.darker());
      g2.fillRect(x - 1, 2, 3, h - 4);
    }
  }
}
