/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chiptunesynth.music;

import chiptunesynth.songs.MetalmanSong;

/**
 *
 * @author dylan
 */
public class MetalmanMusicHandler extends ChiptuneMusicHandler {

  public MetalmanMusicHandler() {
    super(new MetalmanSong());
  }

  public MetalmanMusicHandler(boolean pb, double vol, double speed) {
    this();

    setVolume(vol);
    setSpeed(speed);
    if (pb) {
      startMusic();
    }
  }

  @Override
  public String getMusicType() {
    return "Metal Man - MM2";
  }
}
