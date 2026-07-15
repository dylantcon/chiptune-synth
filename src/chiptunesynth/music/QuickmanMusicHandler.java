/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chiptunesynth.music;

import chiptunesynth.songs.QuickmanSong;

/**
 *
 * @author dylan
 */
public class QuickmanMusicHandler extends ChiptuneMusicHandler {

  public QuickmanMusicHandler() {
    super(new QuickmanSong());
  }

  public QuickmanMusicHandler(boolean pb, double vol, double speed) {
    this();

    setVolume(vol);
    setSpeed(speed);
    if (pb) {
      startMusic();
    }
  }

  @Override
  public String getMusicType() {
    return "Quick Man - MM2";
  }
}
