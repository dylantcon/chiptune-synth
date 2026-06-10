/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chiptunesynth.music;

import chiptunesynth.songs.BloodyTearsSong;

/**
 *
 * @author dylan
 */
public class BloodyTearsMusicHandler extends ChiptuneMusicHandler {
    public BloodyTearsMusicHandler() {
    super(new BloodyTearsSong());
  }
  
  public BloodyTearsMusicHandler(boolean pb, double vol, double speed) {
    this();
    
    setVolume(vol);
    setSpeed(speed);
    if (pb)
      startMusic();
  }
  
  public BloodyTearsMusicHandler(double vol) {
    this();
    setVolume(vol);
  }
  
  @Override
  public String getMusicType() {
    return "Day - Castlevania II";
  }
}
