package chiptunesynth.music;

import chiptunesynth.songs.SilverSurferSong;

/**
 *
 * @author dylan
 */
public class SilverSurferMusicHandler extends ChiptuneMusicHandler {

  public SilverSurferMusicHandler() {
    super(new SilverSurferSong());
  }

  public SilverSurferMusicHandler(boolean pb, double vol, double speed) {
    this();

    setVolume(vol);
    setSpeed(speed);
    if (pb)
      startMusic();
  }

  @Override
  public String getMusicType() {
    return "Title - Silver Surfer";
  }

}
