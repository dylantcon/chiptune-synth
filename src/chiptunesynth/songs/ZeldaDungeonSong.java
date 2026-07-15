package chiptunesynth.songs;

import chiptunesynth.ChiptuneSong;
import chiptunesynth.Track;

/**
 * The Legend of Zelda - Dungeon theme, by Koji Kondo.
 *
 * Transcribed from zelda-dungeon.txt (NSFImport capture, 1 row = 1 frame,
 * cartridge NSF), trimmed to the measured loop: 1160 frames = 58 pulses of
 * 20 frames = 19.3 s. The capture held ~9.5 identical loops; the period
 * verified exact on all three channels with zero mismatches.
 *
 * THE ENGINE - two square waves rocking low-high on the 20-frame pulse,
 * interlocked half a pulse apart, both at 50% duty (V02 in the capture).
 * Each carries its own chromatic descent while its high note pedals:
 *
 *   pulse 2 (on the grid):     G  F#  F  E | Eb  D     under D5, then C5
 *   pulse 1 (half-pulse late): Bb A   Ab G | G(doubled) under Eb5, then D5
 *
 * Four rocking pairs per half-step sink (the pulse-1 G sink runs eight,
 * with the pedal dropped Eb5 -> D5). The triangle is the singer: long
 * moaning holds (F#4 for six pulses, F4, E4), chromatic trembles
 * (Eb-D-Eb, D-C#-D), and rising replies.
 *
 * TURN - the last 10 pulses (not 8: the famous lopsided turnaround). All
 * three voices climb the same diminished 7th (F# A C Eb) in canon: the
 * triangle sprints it in half-pulses, the squares stack it in pulses up
 * to A5 and C6. The C6 rings across the loop seam - its second half is
 * the lead's opening half-pulse rest.
 *
 * WHAT THE DATA SAYS TO LEAVE OUT: no noise, no DPCM (both silent in the
 * whole capture), and no vibrato - the capture's P7F/P80 marks are static
 * per-note detune from the NES pitch table, not modulation.
 *
 * KNOWN APPROXIMATION - the capture breathes each pulse 4 -> 7 -> 4
 * (volume column, a swell with a 57% floor). withSwell() is a full
 * parabola (silent at the ends) and would chop the drone, so both squares
 * sit flat at PULSE_VOL for now. If the drone feels static, this is the
 * first knob: a floored swell in Effect.
 *
 * @author dylan
 */
public class ZeldaDungeonSong implements ChiptuneSong {

  @Override
  public double getTempoScale() {
    return 1.0;                          // NTSC capture; frames are native
  }

  /* === GRID === */
  private static final int P = 20;       // the pulse: everything rides it
  private static final int H_1 = P / 2;    // half pulse (triangle run, offsets)
  private static final int LOOP = 58 * P;

  /* === VOICES / MIX === */
  // both squares sit at the capture's average level (volume column ~5/15),
  // equal partners - neither is "the lead", the interlock is the lead
  private static final double PULSE_VOL  = 0.33;
  private static final double PULSE_DUTY = 0.50;   // V02 in the capture

  private static Track square() {
    return new Track().withDefaults(PULSE_VOL, PULSE_DUTY);
  }

  // one low-high rocking pair per iteration - the hypnosis cell
  private static void rock(Track t, int low, int high, int pairs) {
    for (int i = 0; i < pairs; ++i) {
      t.addNotes(low, P, high, P);
    }
  }

  // the turnaround climb: every pitch one pulse long
  private static void climb(Track t, int... pitches) {
    for (int p : pitches) {
      t.addNotes(p, P);
    }
  }

  /* ==================== PULSE 1 ==================== */

  @Override
  public Track getLead() {
    Track t = square();
    t.addNotes(R, H_1);                    // enters half a pulse behind pulse 2
    rock(t, AS4, DS5, 4);                // Bb   } sinking chromatically
    rock(t, A4,  DS5, 4);                // A    } under the Eb5 pedal
    rock(t, GS4, DS5, 4);                // Ab   }
    rock(t, G4,  DS5, 4);                // G    }
    rock(t, G4,  D5,  8);                // G doubled, pedal drops to D5
    climb(t, FS4, C5, A4, DS5, C5, C5, FS5, FS5, FS5);
    t.addNotes(C6, H_1);                   // rings across the seam; the head
    return t;                            // rest is this note's other half
  }

  /* ==================== PULSE 2 ==================== */

  @Override
  public Track getHarmony() {
    Track t = square();
    rock(t, G4,  D5, 4);                 // G    } the even six-step sink,
    rock(t, FS4, D5, 4);                 // F#   } four pairs per half step
    rock(t, F4,  D5, 4);                 // F    }
    rock(t, E4,  D5, 4);                 // E    }
    rock(t, DS4, C5, 4);                 // Eb   } pedal drops to C5
    rock(t, D4,  C5, 4);                 // D    }
    climb(t, C4, A4, FS4, C5, A4, DS5, DS5, DS5, A5, A5);
    return t;
  }

  /* ==================== TRIANGLE ==================== */

  // the singer: transcribed literally - moans, trembles, and the dim7 sprint
  @Override
  public Track getBass() {
    Track t = new Track().withDefaults(BASS_VOL, BASS_DUTY);
    t.addNotes(G4, 80, AS4, 40, D5, 40, CS5, 40, FS4, 120);   // opening moan
    t.addNotes(F4, 110, GS4, 30, CS5, 20, C5, 40, E4, 120);   // the sigh
    t.addNotes(DS4, 10, D4, 10, DS4, 60);                     // tremble...
    t.addNotes(G4, 30, DS5, 30, D5, 20);                      // ...and reply
    t.addNotes(D4, 10, CS4, 10, D4, 60);                      // tremble...
    t.addNotes(G4, 30, D5, 30, CS5, 20);                      // ...and reply
    // the dim7 sprint - F# A C Eb up and back down, half-pulse steps
    int[] run = {D4, FS4, A4, FS4, A4, C5, A4, C5, DS5, C5,
                 DS5, FS5, A5, FS5, DS5, C5, DS5, C5, A4, FS4};
    for (int p : run) {
      t.addNotes(p, H_1);
    }
    return t;
  }

  /* ==================== DRUMS ==================== */

  // the dungeon has no kit - the dread is all voice-leading
  @Override
  public Track getDrums() {
    return new Track().addNotes(R, LOOP);
  }
}
