/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chiptunesynth;

// channels

/**
 * a pulse/square channel with variable duty cycle and a linear volume-decay
 * envelope
 *
 * the duty cycle controls timbre. 0.125 sounds nasal and thin (the Mario coin
 * sound), 0.25 is the classic NES lead, 0.5 is fuller and more clarinet-like
 *
 * @author dylan
 */
class PulseChannel {

  double phase = 0;
  double freq = 0;
  double duty = 0.5;
  double envelope = 0;
  double envelopeDecay = 0;
  double release = 0;          // decay-per-sec applied after note-off
  boolean releasing = false;   // true once a release fade has begun
  double lastAmp = 0;          // envelope used for the most recent sample()

  // current amplitude (0..1) driving the waveform  the APU mixer needs it,
  // together with sample(), to recover the channel's unipolar 0..15 DAC level.
  double amplitude() {
    return lastAmp;
  }

  // decayPerSec=0 holds the note flat; larger = faster fade
  void noteOn(double frequency, double volume, double dutyCycle, double decayPerSec) {
    noteOn(frequency, volume, dutyCycle, decayPerSec, 0.0);
  }

  // releasePerSec>0 lets the note ring out over ~60/releasePerSec frames when
  // it is released, instead of snapping to silence  the missing release phase
  // that made every note-into-rest sound chopped.
  void noteOn(double frequency, double volume, double dutyCycle,
              double decayPerSec, double releasePerSec) {
    this.freq = frequency;
    this.duty = dutyCycle;
    this.envelope = volume;
    this.envelopeDecay = decayPerSec * volume / ChiptuneSynth.SAMPLE_RATE;
    this.release = releasePerSec;
    this.releasing = false;
  }

  // update pitch mid-note WITHOUT touching phase or envelope. this is what
  // makes vibrato/arpeggio/slide click-free: the waveform keeps running, only
  // its rate changes between frames.
  void setFrequency(double frequency) {
    this.freq = frequency;
  }

  // set the output level directly, for per-frame amplitude shaping (the swell
  // envelope). the sequencer drives this every frame; intra-frame decay still
  // applies but is negligible when the note uses SUSTAINED decay.
  void setLevel(double level) {
    this.envelope = level;
  }

  // With no release set this is the old hard cut. With a release, the first
  // note-off frame arms a linear fade from the current level (so the note rings
  // out through the rest); later note-off frames leave it fading rather than
  // re-arming, giving one smooth ~60/release-frame tail.
  void noteOff() {
    if (release > 0) {
      if (!releasing && envelope > 0) {
        this.envelopeDecay = release * envelope / ChiptuneSynth.SAMPLE_RATE;
        this.releasing = true;
      }
    } else {
      this.envelope = 0;
    }
  }

  double sample() {
    if (envelope <= 0 || freq <= 0) {
      lastAmp = 0;
      return 0;
    }
    double dt = freq / ChiptuneSynth.SAMPLE_RATE;
    phase += dt;
    if (phase >= 1.0) {
      phase -= 1.0;
    }
    // band-limited pulse via polyBLEP: a naive square's edges spray
    // harmonics past Nyquist that fold back as inharmonic aliases  at the
    // 4-6 kHz fundamentals of a Follin cascade that garbage reads as a
    // shrill "telephone" ring the hardware (1.79 MHz clock + analog path)
    // never produces. Smoothing each edge over one sample kills it.
    double s = (phase < duty ? 1.0 : -1.0);
    s += polyBlep(phase, dt);                              // rising edge @ 0
    double tf = phase - duty;
    if (tf < 0) {
      tf += 1.0;
    }
    s -= polyBlep(tf, dt);                                 // falling @ duty
    lastAmp = envelope;                                    // amp for this sample
    s *= envelope;
    envelope = Math.max(0, envelope - envelopeDecay);
    return s;
  }

  // two-sample polynomial band-limited step residual (unit step of 2)
  private static double polyBlep(double t, double dt) {
    if (t < dt) {
      t /= dt;
      return t + t - t * t - 1.0;
    }
    if (t > 1.0 - dt) {
      t = (t - 1.0) / dt;
      return t * t + t + t + 1.0;
    }
    return 0.0;
  }

}
