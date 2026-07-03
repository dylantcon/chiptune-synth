/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chiptunesynth.player;

import chiptunesynth.music.MusicHandler;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

/**
 * Standalone test player for the ChiptuneSynth song repertoire.
 *
 * The track list is discovered through {@link MusicHandler#concretized}, and
 * all playback goes through the MusicHandler interface — so running this jar
 * exercises exactly the surface a game integrating the library would use.
 *
 * Run with no arguments for the Swing UI, or:
 *   --list          print the repertoire and exit
 *   --play &lt;n&gt;      play track n (0-based) in the console until Enter
 *
 * @author dylan
 */
public class ChiptunePlayer {

  // playback knobs shared between UI and CLI
  private static final double MIN_SPEED = 0.25;
  private static final double MAX_SPEED = 2.0;
  private static final double DEFAULT_VOLUME = 0.7;

  private final List<MusicHandler> repertoire;

  // the handler currently routed to the speakers; only one plays at a time
  private MusicHandler current = null;

  public ChiptunePlayer() {
    repertoire = buildRepertoire();
  }

  /**
   * Instantiate one handler per concrete class registered on the interface.
   * Reflection keeps this list in sync with MusicHandler.concretized — adding
   * a song to the library means adding it there, and the player picks it up.
   */
  private static List<MusicHandler> buildRepertoire() {
    List<MusicHandler> handlers = new ArrayList<MusicHandler>();
    for (Class<?> cls : MusicHandler.concretized) {
      try {
        handlers.add((MusicHandler) cls.getDeclaredConstructor().newInstance());
      } catch (ReflectiveOperationException e) {
        System.err.println("Could not instantiate " + cls.getName() + ": " + e);
      }
    }
    return handlers;
  }

  public List<MusicHandler> getRepertoire() {
    return repertoire;
  }

  /** Switch playback to the given handler, silencing whichever came before. */
  public synchronized void play(MusicHandler h, double volume, double speed) {
    if (current != null && current != h) {
      current.stopMusic();
    }
    current = h;
    h.setVolume(volume);
    h.setSpeed(speed);
    h.startMusic();
  }

  /** Stop without losing position; startMusic resumes where it left off. */
  public synchronized void pause() {
    if (current != null) {
      current.stopMusic();
    }
  }

  public synchronized void rewind(double speed) {
    if (current != null) {
      // restartMusic resets the speed to BASE_SPEED, so re-apply the knob
      current.restartMusic();
      current.setSpeed(speed);
    }
  }

  public synchronized MusicHandler getCurrent() {
    return current;
  }

  /* === SWING UI === */

  private static void launchGui(final ChiptunePlayer player) {
    JFrame frame = new JFrame("ChiptuneSynth Player");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    final SongListPanel trackList = new SongListPanel(player.getRepertoire());

    final JLabel status = new JLabel("Select a track and press Play", JLabel.CENTER);

    // sliders use ints, so volume is in percent and speed in percent of normal
    final JSliderPanel volPanel = new JSliderPanel(
            0, 100, (int) (DEFAULT_VOLUME * 100),
            v -> "Volume: " + v + "%");
    final JSliderPanel speedPanel = new JSliderPanel(
            (int) (MIN_SPEED * 100), (int) (MAX_SPEED * 100), 100,
            v -> "Speed: " + v + "%");
    // the live position scrubber. Readout leads with the FRAME (exact by
    // construction); percent and seconds show one decimal each, at or below
    // the true 1-frame resolution — no fake precision.
    final PositionSliderPanel posPanel = new PositionSliderPanel((pos, total) -> {
      if (total <= 0) {
        return "Position: --";
      }
      MusicHandler h = player.getCurrent();
      String time = "";
      if (h != null) {
        double dur = h.getDurationSeconds();
        time = "  " + MusicHandler.formatSeconds(pos / (double) total * dur)
             + " (" + MusicHandler.formatSeconds(dur) + ")";
      }
      return String.format("frame %d/%d  %.1f%%%s",
              pos, total, 100.0 * pos / total, time);
    });
    posPanel.setSeekHandler(fraction -> {
      MusicHandler h = player.getCurrent();
      if (h != null) {
        h.seek(fraction);
      }
    });
    // the scrubber follows whichever synth is audible; only the running
    // synth emits callbacks, so registering with the whole repertoire is safe
    for (MusicHandler h : player.getRepertoire()) {
      h.addSynthListener(posPanel);
    }
    final JSlider volSlider = volPanel.getSlider();
    final JSlider speedSlider = speedPanel.getSlider();

    JButton playBtn = new JButton("Play");
    JButton pauseBtn = new JButton("Pause");
    JButton rewindBtn = new JButton("Rewind");

    // monospace across the player — commit to the retro look
    Font mono = new Font(Font.MONOSPACED, Font.BOLD, 12);
    playBtn.setFont(mono);
    pauseBtn.setFont(mono);
    rewindBtn.setFont(mono);
    status.setFont(mono);

    playBtn.addActionListener(e -> {
      int i = trackList.getSelectedIndex();
      if (i < 0) {
        return;
      }
      MusicHandler h = player.getRepertoire().get(i);
      player.play(h, volSlider.getValue() / 100.0, speedSlider.getValue() / 100.0);
      status.setText("Playing: " + h.getMusicType());
      posPanel.refreshReadout();   // now that a song (duration) is current
    });
    pauseBtn.addActionListener(e -> {
      player.pause();
      MusicHandler h = player.getCurrent();
      if (h != null) {
        status.setText("Paused: " + h.getMusicType());
      }
    });
    rewindBtn.addActionListener(e -> {
      player.rewind(speedSlider.getValue() / 100.0);
      posPanel.setFraction(0);
      MusicHandler h = player.getCurrent();
      if (h != null) {
        status.setText("Rewound: " + h.getMusicType());
      }
    });

    // live knobs: adjust the playing handler as the sliders move
    volSlider.addChangeListener(e -> {
      MusicHandler h = player.getCurrent();
      if (h != null) {
        h.setVolume(volSlider.getValue() / 100.0);
      }
    });
    speedSlider.addChangeListener(e -> {
      MusicHandler h = player.getCurrent();
      if (h != null) {
        h.setSpeed(speedSlider.getValue() / 100.0);
        posPanel.refreshReadout();   // duration (m:ss) depends on speed
      }
    });

    // double-click a track to start it immediately
    trackList.setActivationHandler(() -> playBtn.doClick());

    JPanel buttons = new JPanel(new FlowLayout());
    buttons.add(playBtn);
    buttons.add(pauseBtn);
    buttons.add(rewindBtn);

    JPanel sliders = new JPanel(new GridLayout(3, 1, 4, 4));
    sliders.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
    sliders.add(volPanel);
    sliders.add(speedPanel);
    sliders.add(posPanel);

    JPanel south = new JPanel(new BorderLayout());
    south.add(buttons, BorderLayout.NORTH);
    south.add(sliders, BorderLayout.CENTER);
    south.add(status, BorderLayout.SOUTH);

    frame.setLayout(new BorderLayout());
    // north-anchor the grid so scrollpane space beyond N rows stays empty
    // instead of stretching the rows
    JPanel listAnchor = new JPanel(new BorderLayout());
    listAnchor.add(trackList, BorderLayout.NORTH);
    frame.add(new JScrollPane(listAnchor), BorderLayout.CENTER);
    frame.add(south, BorderLayout.SOUTH);
    frame.setSize(380, 560);   // room for the three readout slider panels
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  /* === CLI MODE === */

  private static void printList(ChiptunePlayer player) {
    List<MusicHandler> reps = player.getRepertoire();
    for (int i = 0; i < reps.size(); i++) {
      System.out.println("  [" + i + "] " + reps.get(i).getMusicType());
    }
  }

  private static void playInConsole(ChiptunePlayer player, int index) throws Exception {
    List<MusicHandler> reps = player.getRepertoire();
    if (index < 0 || index >= reps.size()) {
      System.err.println("Track index out of range (0-" + (reps.size() - 1) + ")");
      return;
    }
    MusicHandler h = reps.get(index);
    System.out.println("Playing: " + h.getMusicType() + " - press Enter to stop");
    player.play(h, DEFAULT_VOLUME, 1.0);
    // the synth runs on a daemon thread, so block here until the user is done
    new BufferedReader(new InputStreamReader(System.in)).readLine();
    player.pause();
  }

  public static void main(String[] args) throws Exception {
    final ChiptunePlayer player = new ChiptunePlayer();

    if (args.length > 0) {
      if ("--list".equals(args[0])) {
        printList(player);
      } else if ("--play".equals(args[0]) && args.length > 1) {
        playInConsole(player, Integer.parseInt(args[1]));
      } else {
        System.out.println("Usage: java -jar ChiptuneSynth.jar [--list | --play <n>]");
      }
      return;
    }

    SwingUtilities.invokeLater(() -> launchGui(player));
  }

  /**
   * The repertoire view: an Nx2 grid of rows — title cell flowing left,
   * runtime cell flowing right, one JLabel per cell. Runtimes (at 100%
   * speed) sit in placeholder grey as reference info. Click selects a row,
   * double-click activates it via the handler the caller installs.
   */
  private static class SongListPanel extends JPanel {

    private static final Font TITLE_FONT =
        new Font(Font.MONOSPACED, Font.BOLD, 13);
    private static final Font TIME_FONT =
        new Font(Font.MONOSPACED, Font.PLAIN, 13);
    private static final Color SELECT_BG = new Color(205, 233, 233);

    private final JPanel[][] cells;
    private final Color baseBg;
    private int selected = 0;
    private Runnable onActivate;

    SongListPanel(List<MusicHandler> repertoire) {
      super(new GridLayout(repertoire.size(), 2, 0, 2));
      cells = new JPanel[repertoire.size()][2];
      baseBg = getBackground();

      for (int i = 0; i < repertoire.size(); i++) {
        MusicHandler h = repertoire.get(i);

        JLabel title = new JLabel(h.getMusicType());
        title.setFont(TITLE_FONT);
        JLabel runtime = new JLabel(
            "(" + MusicHandler.formatSeconds(h.getDurationSeconds()) + ")");
        runtime.setFont(TIME_FONT);
        runtime.setForeground(Color.GRAY);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
        left.add(title);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.add(runtime);

        final int row = i;
        MouseAdapter click = new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            setSelected(row);
            if (e.getClickCount() == 2 && onActivate != null) {
              onActivate.run();
            }
          }
        };
        left.addMouseListener(click);
        right.addMouseListener(click);

        cells[i][0] = left;
        cells[i][1] = right;
        add(left);
        add(right);
      }
      setSelected(0);
    }

    final void setSelected(int row) {
      selected = row;
      for (int i = 0; i < cells.length; i++) {
        Color bg = (i == selected) ? SELECT_BG : baseBg;
        cells[i][0].setBackground(bg);
        cells[i][1].setBackground(bg);
      }
      repaint();
    }

    int getSelectedIndex() {
      return selected;
    }

    void setActivationHandler(Runnable handler) {
      onActivate = handler;
    }
  }
}
