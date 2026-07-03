package chiptunesynth.player;

import javax.swing.JComponent;

/**
 * A labeled control panel with a live value readout. The invariant contract
 * the player relies on: it is a Swing component, and its readout can be
 * asked to re-render when external state it reflects (e.g. song duration
 * after a speed change) has moved under it.
 *
 * Implementations: {@link JSliderPanel} wraps a stock JSlider (volume,
 * speed); {@link PositionSliderPanel} paints its own scrubber and follows
 * playback live. Shared readout/theme logic lives in
 * {@link AbstractSliderPanel}.
 *
 * @author dylan
 */
public interface SliderPanel {

  /** The Swing component to add to a layout. */
  JComponent getComponent();

  /** Re-render the readout from current state. */
  void refreshReadout();
}
