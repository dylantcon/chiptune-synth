package chiptunesynth.player;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.Hashtable;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;

/**
 * A SliderPanel backed by a stock JSlider — tick marks, min/mid/max scale
 * labels, and the readout from AbstractSliderPanel. Serves the controls
 * whose values are exact integers (volume %, speed %), where a JSlider's
 * granularity is the true granularity.
 *
 * @author dylan
 */
public class JSliderPanel extends AbstractSliderPanel {

  /** Renders the readout text for the slider's current value. */
  public interface Formatter {
    String format(int value);
  }

  private final JSlider slider;
  private final Formatter fmt;

  public JSliderPanel(int min, int max, int initial, Formatter fmt) {
    this.fmt = fmt;

    slider = new JSlider(JSlider.HORIZONTAL, min, max, initial);
    slider.setMajorTickSpacing(Math.max(1, (max - min) / 2));
    slider.setMinorTickSpacing(Math.max(1, (max - min) / 20));
    slider.setPaintTicks(true);
    slider.setPaintLabels(true);
    slider.setLabelTable(scaleLabels(min, max));
    slider.addChangeListener(e -> refreshReadout());
    add(slider, BorderLayout.NORTH);

    refreshReadout();
  }

  private static Hashtable<Integer, JLabel> scaleLabels(int min, int max) {
    Hashtable<Integer, JLabel> table = new Hashtable<Integer, JLabel>();
    Font small = new Font(Font.MONOSPACED, Font.PLAIN, 10);
    for (int v : new int[]{min, (min + max) / 2, max}) {
      JLabel l = new JLabel(Integer.toString(v), JLabel.CENTER);
      l.setFont(small);
      table.put(v, l);
    }
    return table;
  }

  @Override
  public final void refreshReadout() {
    setReadoutText(fmt.format(slider.getValue()));
  }

  public JSlider getSlider() {
    return slider;
  }

  public int getValue() {
    return slider.getValue();
  }

  public void setValue(int value) {
    slider.setValue(value);
  }

  public void addChangeListener(ChangeListener l) {
    slider.addChangeListener(l);
  }
}
