/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chiptunesynth;

/**
 * An immutable, composable per-note modulation spec  the expressive
 * vocabulary the original Note model lacked. These are exactly the tricks
 * David Wise's NES drivers used to make a 2-channel square lead "sing":
 *
 *  - arpeggio  : cycle 2-3 semitone offsets every frame so the ear fuses
 *                them into a chord (the NES "fake chord"). One mono channel
 *                pretends to be a triad.
 *  - vibrato   : a pitch LFO; the late-onset wobble on held notes.
 *  - slide     : portamento; glide from the previous pitch into this one.
 *  - pitchEnv  : a one-shot pitch ramp toward 0 (scoops, zaps, drum sweeps).
 *  - detune    : a CONSTANT sub-semitone pitch offset held for the whole note.
 *                This is the FamiTracker fine-pitch (Pxx) register: NSF
 *                captures write it per note to place the pitch exactly on the
 *                recorded period, which the equal-tempered note name only
 *                approximates. Unlike vibrato it does not move; unlike slide it
 *                needs no previous pitch. One 2A03 period step is a
 *                pitch-dependent fraction of a semitone, so the converter bakes
 *                the already-resolved semitone value in here.
 *  - swell     : a parabolic AMPLITUDE contour  soft fade in to a peak and
 *                back down. This is the one non-pitch tool here; it's what a
 *                "pad" needs. A flat-topped square that just switches on at
 *                full volume sounds like a telephone tone; the swell is the
 *                "wide downward-opening parabola" envelope Wise's pads ride.
 *
 * The pitch effects sum additively in semitone space, so they stack. Slide
 * needs the previous note's pitch, which only the sequencer knows, so the
 * synth applies slide itself and this class only carries {@link #slideFrames};
 * the rest of the pitch shaping is {@link #pitchOffset(int)} and the amplitude
 * shaping is {@link #ampScale(int, int)}.
 *
 * Immutability + copy-"with" methods let a Track accumulate effects the same
 * way it accumulates withDecay(...): {@code Effect.NONE.withArp(0,4,7)
 * .withVibrato(0.4, 6, 8)}.
 *
 * @author dylan
 */
public final class Effect {

  /** The do-nothing effect. Every existing note defaults to this. */
  public static final Effect NONE =
          new Effect(null, 1, 0, 0, 0, 0, 0, 0, false, 0);

  final int[]  arpSemis;    // semitone offsets cycled for a fake chord; null=off
  final int    arpSpeed;    // frames per arp step (1 = swap every frame)
  final double vibSemis;    // vibrato depth, semitones (peak)
  final double vibHz;       // vibrato rate, Hz
  final int    vibDelay;    // frames to wait before vibrato fades in
  final int    slideFrames; // portamento: glide from prev pitch over N frames
  final double penvStart;   // pitch-env: starting offset in semitones...
  final int    penvFrames;  // ...ramping linearly to 0 across this many frames
  final boolean swell;      // parabolic amplitude envelope across the note
  final double detuneSemis; // constant pitch offset, semitones (fine pitch/Pxx)

  private Effect(int[] arp, int arpSpeed, double vibSemis, double vibHz,
                 int vibDelay, int slideFrames, double penvStart, int penvFrames,
                 boolean swell, double detuneSemis) {
    this.arpSemis = arp;
    this.arpSpeed = arpSpeed;
    this.vibSemis = vibSemis;
    this.vibHz = vibHz;
    this.vibDelay = vibDelay;
    this.slideFrames = slideFrames;
    this.penvStart = penvStart;
    this.penvFrames = penvFrames;
    this.swell = swell;
    this.detuneSemis = detuneSemis;
  }

  /** Fake-chord arpeggio: e.g. withArp(0,4,7) = a major triad on one channel. */
  public Effect withArp(int... semis) {
    return new Effect(semis, arpSpeed, vibSemis, vibHz, vibDelay,
            slideFrames, penvStart, penvFrames, swell, detuneSemis);
  }

  /** Frames per arp step. 1 (default) = the classic shimmer; 2-3 = chunkier. */
  public Effect withArpSpeed(int framesPerStep) {
    return new Effect(arpSemis, Math.max(1, framesPerStep), vibSemis, vibHz,
            vibDelay, slideFrames, penvStart, penvFrames, swell, detuneSemis);
  }

  /** Vibrato: depth in semitones, rate in Hz, delay in frames before onset. */
  public Effect withVibrato(double semis, double hz, int delayFrames) {
    return new Effect(arpSemis, arpSpeed, semis, hz, delayFrames,
            slideFrames, penvStart, penvFrames, swell, detuneSemis);
  }

  /** Portamento: glide from the previous note's pitch over this many frames. */
  public Effect withSlide(int frames) {
    return new Effect(arpSemis, arpSpeed, vibSemis, vibHz, vibDelay,
            Math.max(0, frames), penvStart, penvFrames, swell, detuneSemis);
  }

  /**
   * One-shot pitch ramp: start {@code startSemis} away from the note and
   * slide back to it over {@code frames}. Negative start = a downward scoop
   * (laser / drum), positive = a rising swell into the note.
   */
  public Effect withPitchEnv(double startSemis, int frames) {
    return new Effect(arpSemis, arpSpeed, vibSemis, vibHz, vibDelay,
            slideFrames, startSemis, Math.max(0, frames), swell, detuneSemis);
  }

  /**
   * Constant fine-pitch detune: shift the whole note by {@code semis}
   * semitones (fractional). This is the FamiTracker Pxx register resolved to
   * a pitch offset  positive is sharp, negative is flat. It sums with the
   * other pitch effects, so a detuned note can still arpeggiate or vibrato.
   */
  public Effect withDetune(double semis) {
    return new Effect(arpSemis, arpSpeed, vibSemis, vibHz, vibDelay,
            slideFrames, penvStart, penvFrames, swell, semis);
  }

  /**
   * Parabolic amplitude swell across the whole note: silent at both ends,
   * full at the midpoint. This is the soft "wide downward-opening parabola"
   * a pad needs so it blooms in and out instead of clicking on at full volume.
   * Pair it with SUSTAINED decay (the channel's own per-sample decay would
   * otherwise fight the contour).
   */
  public Effect withSwell() {
    return new Effect(arpSemis, arpSpeed, vibSemis, vibHz, vibDelay,
            slideFrames, penvStart, penvFrames, true, detuneSemis);
  }

  /**
   * Semitone offset to add to the note's base pitch at frame {@code f} of the
   * note (arpeggio + vibrato + pitch-env; slide is handled by the synth).
   *
   * @param f frames elapsed since this note started (0 on the first frame)
   * @return additive pitch offset in semitones
   */
  double pitchOffset(int f) {
    double off = detuneSemis;                 // constant, present for every frame
    if (arpSemis != null && arpSemis.length > 0) {
      off += arpSemis[(f / arpSpeed) % arpSemis.length];
    }
    if (vibSemis != 0 && f >= vibDelay) {
      off += vibSemis * Math.sin(
              2 * Math.PI * vibHz * (f - vibDelay) / ChiptuneSynth.FRAME_RATE);
    }
    if (penvFrames > 0) {
      off += penvStart * Math.max(0.0, 1.0 - (double) f / penvFrames);
    }
    return off;
  }

  /**
   * Amplitude multiplier (0..1) at frame {@code f} of a note {@code dur}
   * frames long. With swell off this is a flat 1.0 (no change). With swell on
   * it is the parabola 4t(1-t), t = f/(dur-1): zero at both ends, 1.0 at the
   * midpoint  the smooth bloom Wise's pads have.
   *
   * @param f   frames elapsed since the note started (0 on the first frame)
   * @param dur the note's total duration in frames
   * @return amplitude scale in [0, 1]
   */
  double ampScale(int f, int dur) {
    if (!swell || dur <= 1) {
      return 1.0;
    }
    double t = (double) f / (dur - 1);
    if (t < 0) t = 0;
    if (t > 1) t = 1;
    return 4.0 * t * (1.0 - t);
  }
}
