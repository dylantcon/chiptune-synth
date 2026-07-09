package chiptunesynth.music;

import chiptunesynth.songs.ZeldaDungeonSong;

/**
 *
 * @author dylan
 */
public class ZeldaDungeonMusicHandler extends ChiptuneMusicHandler {

  public ZeldaDungeonMusicHandler() {
    super(new ZeldaDungeonSong());
  }

  public ZeldaDungeonMusicHandler(boolean pb, double vol, double speed) {
    this();

    setVolume(vol);
    setSpeed(speed);
    if (pb)
      startMusic();
  }

  @Override
  public String getMusicType() {
    return "Dungeon - The Legend of Zelda";
  }

}
