/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chiptunesynth.music;

import chiptunesynth.songs.AirmanSong;

/**
 *
 * @author dylan
 */
public class AirmanMusicHandler extends ChiptuneMusicHandler {

  public AirmanMusicHandler() {
    super(new AirmanSong());
  }

  public AirmanMusicHandler(boolean pb, double vol, double speed) {
    this();

    setVolume(vol);
    setSpeed(speed);
    if (pb) {
      startMusic();
    }
  }

  @Override
  public String getMusicType() {
    return "Air Man - MM2";
  }
}
