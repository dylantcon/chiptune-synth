/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chiptunesynth.music;

import chiptunesynth.songs.SurfCitySong;

/**
 *
 * @author dylan
 */
public class SurfCityMusicHandler extends ChiptuneMusicHandler {

  public SurfCityMusicHandler() {
    super(new SurfCitySong());
  }
  
  public SurfCityMusicHandler(boolean pb, double vol, double speed) {
    this();
    
    setVolume(vol);
    setSpeed(speed);
    if (pb)
      startMusic();
  }
  
  @Override
  public String getMusicType() {
    return "Surf City - Battletoads";
  }
  
}
