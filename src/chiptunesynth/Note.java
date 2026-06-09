/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chiptunesynth;

/**
 * a note event. use midi=-1 for a rest. duration is in frames (60th of a
 * second), same as NES. so, at 60fps, a 24 frame note is 0.4 seconds, which
 * is a quarter note at 150 BPM.
 *
 * @author dylan
 */
public class Note {
    public final int midi;
    public final int durationFrames;
    public final double volume;
    public final double duty;
    public final double decay;          // per-note envelope decay rate
    public final Effect fx;             // per-note pitch modulation (never null)

    public Note(int midi, int duration, double volume, double duty) {
        this(midi, duration, volume, duty, DEFAULT_DECAY, Effect.NONE);
    }

    public Note(int midi, int duration, double volume, double duty, double decay) {
        this(midi, duration, volume, duty, decay, Effect.NONE);
    }

    public Note(int midi, int duration, double volume, double duty,
                double decay, Effect fx) {
        this.midi = midi;
        this.durationFrames = duration;
        this.volume = volume;
        this.duty = duty;
        this.decay = decay;
        this.fx = (fx == null) ? Effect.NONE : fx;
    }

    public static Note rest(int frames) {
        return new Note(-1, frames, 0, 0.5, 0, Effect.NONE);
    }
    
    private static final double DEFAULT_DECAY = 0.6;
}
