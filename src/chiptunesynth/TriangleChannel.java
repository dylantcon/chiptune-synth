/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chiptunesynth;

/**
 * A triangle channel.
 *
 * real NES hardware steps through 32 levels; the staircase is part of the
 * distinctive character. approximate that by quantizing a continuous triangle
 * to 8 levels per half-cycle
 *
 * @author dylan
 */
class TriangleChannel {

  double phase = 0;
  double freq = 0;
  boolean active = false;

  void noteOn(double frequency) {
    this.freq = frequency;
    this.active = true;
  }

  // mid-note pitch update (phase preserved)  lets the bass slide and
  // vibrato the same way the pulse channels do.
  void setFrequency(double frequency) {
    this.freq = frequency;
  }

  void noteOff() {
    this.active = false;
  }

  // the triangle has no volume envelope  it is either running at full scale or
  // off  so its amplitude is 1 while active, matching sample()'s [-1, 1] swing.
  double amplitude() {
    return active ? 1.0 : 0.0;
  }

  double sample() {
    if (!active || freq <= 0) {
      return 0;
    }
    phase += freq / ChiptuneSynth.SAMPLE_RATE;
    if (phase >= 1.0) {
      phase -= 1.0;
    }
    // triangle in -1..+1 phase in 0..1
    double tri = phase < 0.5 ? phase * 4 - 1 : 3 - phase * 4;
    // quantize to 8 levels for NES stair-step character
    return Math.round(tri * 7) / 7.0;
  }
  
}
