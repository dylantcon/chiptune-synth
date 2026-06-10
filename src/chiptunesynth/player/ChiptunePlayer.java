/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chiptunesynth.player;

import chiptunesynth.music.MusicHandler;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.ListSelectionModel;
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

    final DefaultListModel<String> model = new DefaultListModel<String>();
    for (MusicHandler h : player.getRepertoire()) {
      model.addElement(h.getMusicType());
    }
    final JList<String> trackList = new JList<String>(model);
    trackList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    trackList.setSelectedIndex(0);

    final JLabel status = new JLabel("Select a track and press Play", JLabel.CENTER);

    // sliders use ints, so volume is in percent and speed in percent of normal
    final JSlider volSlider = new JSlider(0, 100, (int) (DEFAULT_VOLUME * 100));
    final JSlider speedSlider = new JSlider(
            (int) (MIN_SPEED * 100), (int) (MAX_SPEED * 100), 100);

    JButton playBtn = new JButton("Play");
    JButton pauseBtn = new JButton("Pause");
    JButton rewindBtn = new JButton("Rewind");

    playBtn.addActionListener(e -> {
      int i = trackList.getSelectedIndex();
      if (i < 0) {
        return;
      }
      MusicHandler h = player.getRepertoire().get(i);
      player.play(h, volSlider.getValue() / 100.0, speedSlider.getValue() / 100.0);
      status.setText("Playing: " + h.getMusicType());
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
      }
    });

    // double-click a track to start it immediately
    trackList.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          playBtn.doClick();
        }
      }
    });

    JPanel buttons = new JPanel(new FlowLayout());
    buttons.add(playBtn);
    buttons.add(pauseBtn);
    buttons.add(rewindBtn);

    JPanel sliders = new JPanel(new GridLayout(2, 2, 4, 4));
    sliders.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
    sliders.add(new JLabel("Volume"));
    sliders.add(volSlider);
    sliders.add(new JLabel("Speed (" + (int) (MIN_SPEED * 100) + "–"
            + (int) (MAX_SPEED * 100) + "%)"));
    sliders.add(speedSlider);

    JPanel south = new JPanel(new BorderLayout());
    south.add(buttons, BorderLayout.NORTH);
    south.add(sliders, BorderLayout.CENTER);
    south.add(status, BorderLayout.SOUTH);

    frame.setLayout(new BorderLayout());
    frame.add(new JScrollPane(trackList), BorderLayout.CENTER);
    frame.add(south, BorderLayout.SOUTH);
    frame.setSize(380, 420);
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
}
