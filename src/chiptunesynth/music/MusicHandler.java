/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package chiptunesynth.music;

/**
 * Implemented by ChiptuneMusicHandler and its per-song subclasses
 *
 * @author dylan
 */
public interface MusicHandler {

  public final static double BASE_SPEED = 1.0;
  public final static double GAME_OVER_SPEED = 0.25;
  
  public static final Class<?>[] concretized = {
    KorobeinikiMusicHandler.class,
    FlashmanMusicHandler.class,
    BloodyTearsMusicHandler.class,
    ContraJungleMusicHandler.class,
    AirmanMusicHandler.class,
    SurfCityMusicHandler.class,
    MetalmanMusicHandler.class,
    HyruleTempleMusicHandler.class,
    QuickmanMusicHandler.class,
    DuckTalesMoonMusicHandler.class,
    WilyMusicHandler.class,
    ZeldaDungeonMusicHandler.class,
    SilverSurferMusicHandler.class
  };

  public void startMusic();

  public void stopMusic();

  public void restartMusic();

  public void setVolume(double volume);

  public void setSpeed(double speed);

  public double getSpeed();

  /** Jump playback to a fraction (0..1) of the song's loop.
   * @param fraction */
  public void seek(double fraction);

  /** Wall-clock seconds for one loop at the current speed.
   * @return  */
  public double getDurationSeconds();

  /**
   * "M:SS.D"  one decimal, matching the synth's honest 1-frame resolution.
   * A formatter, not state: seconds must be computed where speed and
   * tempoScale are known (getDurationSeconds), never stored on a Track,
   * whose only truthful unit is the frame.
   *
   * @param seconds wall-clock seconds
   * @return e.g. 114.63 -&gt; "1:54.6"
   */
  public static String formatSeconds(double seconds) {
    double r = Math.round(Math.max(0, seconds) * 10) / 10.0;
    int m = (int) (r / 60);
    return String.format("%d:%04.1f", m, r - m * 60);
  }

  /** Register for device-clock-grounded playback-position callbacks.
   * @param l */
  public void addSynthListener(chiptunesynth.ChiptuneSynthListener l);

  public abstract String getMusicType();

  public boolean doingPlayback();
}
