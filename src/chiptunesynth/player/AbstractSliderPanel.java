package chiptunesynth.player;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * The invariant half of a SliderPanel: bordered panel, accent theme, and a
 * monospaced live readout along the south edge. Subclasses provide the
 * control surface (a stock JSlider, a custom-painted scrubber) and decide
 * what the readout says.
 *
 * @author dylan
 */
public abstract class AbstractSliderPanel extends JPanel implements SliderPanel {

  protected static final Color ACCENT = new Color(0, 130, 130);

  private final JLabel readout;

  protected AbstractSliderPanel() {
    setLayout(new BorderLayout());
    readout = new JLabel("", JLabel.CENTER);
    readout.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
    readout.setForeground(ACCENT.darker());
    setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(ACCENT, 1),
        BorderFactory.createEmptyBorder(3, 3, 3, 3)));
    add(readout, BorderLayout.SOUTH);
  }

  protected final void setReadoutText(String text) {
    readout.setText(text);
  }

  @Override
  public final JComponent getComponent() {
    return this;
  }
}
