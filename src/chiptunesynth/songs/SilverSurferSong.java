package chiptunesynth.songs;

import chiptunesynth.ChiptuneSong;
import chiptunesynth.Track;

/**
 * Silver Surfer  title theme (Tim Follin / Geoff Follin, 1990).
 *
 * Transcribed bar-for-bar from an NSFImport capture (silver-surfer.txt,
 * 1 row = 1 NTSC frame). Measured structure: a 112-frame play-once head,
 * then a 5,376-frame loop = 48 bars of 112 frames  the native grid is a
 * 7-frame sixteenth (~128.8 BPM), so this song writes on W=112 with
 * tempoScale 1.0 and wraps via getLoopStartFrames().
 *
 * The Follin engineering, straight from the capture:
 *  - The volume column IS the instrument: cascades are gated v3/v0 every
 *    few frames (the echo shimmer), the groove pumps a dip-and-swell
 *    envelope painted in 1-frame volume steps (addSegs register rows),
 *    and P2's washes climb a v1..v6 staircase across every bar (washBar).
 *    Because the capture paints every envelope itself, all pulse tracks
 *    run withDecay(SUSTAINED)  layering our decay on top double-fades.
 *  - Channels swap roles per section. The triangle alone is a drum AND the
 *    bassline: a 5-frame pitch dive (AS2/GS3 -> E2 -> C2 -> A1 -> GS1) on
 *    every beat is the kick, with the actual bass notes woven between.
 *  - Duty is choreography, not decoration: P1 50% / P2 12.5% through the
 *    intro, thin V00 for the pump, V03 fat lead for the solo, and the
 *    bars 32-35 dialogue flips V02/V03/V00 mid-bar on both pulses with
 *    P2 trailing P1 by 21 frames.
 *  - The solo's deep vibrato is a +/-1-semitone note-table warble at ~5 Hz
 *    with fine-pitch smoothing; long holds use withVibrato(1.0, 5.0, d)
 *    (the delay models the flat start), short warbles stay literal.
 *
 * Form (48-bar loop): cascade intro (0-3), pump groove (4-11), lead solo
 * (12-31), duty-choreographed echo dialogue (32-35), comedown (36-39),
 * outro washes over half-time kit (40-47).
 */
public class SilverSurferSong implements ChiptuneSong {

  // native grid: 7-frame sixteenth, 28-frame quarter, 112-frame bar
  private static final int W = 112;

  private static final int HEAD_BARS = 1;

  // 2A03 duty register values as written by the capture's Vxx column
  private static final double D_V00 = 0.125;
  private static final double D_V01 = 0.25;
  private static final double D_V02 = 0.50;
  private static final double D_V03 = 0.75;

  // The capture's volume column (0..15) -> amplitude. One knob scales the
  // whole pulse mix so Follin's internal balance survives ear-tuning.
  private static final double V_STEP = 1.9 / 15.0;

  private static double V(int v) {
    return v * V_STEP;
  }

  @Override
  public int getLoopStartFrames() {
    return HEAD_BARS * W;
  }

  /* ==================== LEAD (pulse 1) ==================== */

  @Override
  public Track getLead() {
    Track t = new Track().withDefaults(V(3), D_V02);
    t.addNotes(p1Head());
    t.addNotes(p1Intro());                    // bars 0-3, cascade
    t.addNotes(p1Pump0());                    // bars 4-11, the pump groove
    for (int i = 0; i < 3; i++) {
      t.addNotes(p1Pump1());
      t.addNotes(p1Pump2());
    }
    t.addNotes(p1Pump1());
    t.addNotes(p1B12());                      // bars 12-31, the solo
    t.addNotes(p1B13());
    t.addNotes(p1B14());
    t.addNotes(p1B15());
    t.addNotes(p1B16());
    t.addNotes(p1B17());
    t.addNotes(p1B18());
    t.addNotes(p1B19());
    t.addNotes(p1B20());
    t.addNotes(p1B21());
    t.addNotes(p1B22());
    t.addNotes(p1B23());
    t.addNotes(p1B24());
    t.addNotes(p1B25());
    t.addNotes(p1B26());
    t.addNotes(p1B27());
    t.addNotes(p1B28());
    t.addNotes(p1B29());
    t.addNotes(p1B30());
    t.addNotes(p1B31());
    t.addNotes(p1Echo());                     // bars 32-35, duty dialogue
    t.addNotes(p1Comedown());                 // bars 36-37
    for (int i = 0; i < 10; i++) {
      t.addNotes(p1Vamp());                   // bars 38-47
    }
    return t;
  }

  // Play-once head: an accelerating four-octave chromatic rocket, launched
  // under P2's waterfall. Duty V02 from the capture's first row.
  private static Track p1Head() {
    Track t = new Track().withDefaults(V(3), D_V02).withDecay(SOSTENUTO);
    t.withVolume(V(3));
    t.addNotes(AS2, 3, B2, 6, C3, 7, CS3, 5, D3, 6, DS3, 5, 
        E3, 5, F3, 5, FS3, 4, G3, 4, GS3, 4, A3, 4, 
        AS3, 3, B3, 3, C4, 4, CS4, 2, D4, 3, DS4, 3, 
        E4, 2, F4, 3, FS4, 2, G4, 2, GS4, 2, A4, 2, 
        AS4, 1, B4, 2, C5, 2, CS5, 1, D5, 1, DS5, 2, 
        E5, 1, F5, 1, FS5, 1, G5, 1, GS5, 1, A5, 1, 
        AS5, 1, B5, 1, C6, 1, D6, 1, DS6, 1, F6, 1, 
        G6, 1, A6, 1);
    return t;
  }

  // Bars 0-3: P1's half of the twin cascade  the same four-octave fall
  // and climb as P2, on its own v3/v0 gate phase (the gates arrive here
  // as literal rests: v0 IS silence on hardware too).
  private static Track p1Intro() {
    Track t = new Track().withDefaults(V(3), D_V02).withDecay(SOSTENUTO);
    t.withVolume(V(3));
    t.addNotes(105, 2, 104, 2, R, 2, R, 1, 102, 1, 101, 2, 
        R, 3, R, 1, 99, 2, 98, 1, R, 1, R, 3, 
        96, 3, B6, 4, AS6, 3, A6, 1, R, 3, GS6, 3, 
        R, 4, G6, 1, FS6, 2, R, 2, R, 2, F6, 3, 
        E6, 5, DS6, 3, R, 2, R, 1, D6, 3, R, 1, 
        R, 3, CS6, 3, C6, 6, B5, 2, R, 3, B5, 2, 
        AS5, 2, R, 3, AS5, 2, R, 5, A5, 2, GS5, 2, 
        R, 3, GS5, 3, R, 4, G5, 3, R, 1, R, 3, 
        FS5, 6, F5, 5, R, 3, F5, 1, E5, 2, R, 4, 
        E5, 3, R, 1, R, 3, DS5, 7, D5, 4, R, 3, 
        D5, 3, R, 1, R, 3, CS5, 9, C5, 2, R, 3, 
        C5, 4, R, 3, B4, 2, R, 5, B4, 4, R, 2, 
        R, 1, AS4, 3, R, 4, AS4, 3, R, 3, R, 1, 
        A4, 7, AS2, 4, R, 2, R, 1, B2, 3, R, 4, 
        B2, 3, R, 2, R, 2, C3, 11, R, 3, CS3, 3, 
        R, 4, CS3, 1, D3, 2, R, 4, D3, 5, DS3, 6, 
        R, 3, DS3, 2, E3, 1, R, 4, E3, 5, F3, 6, 
        R, 3, FS3, 4, R, 3, FS3, 2, R, 5, G3, 3, 
        GS3, 1, R, 3, GS3, 3, R, 1, R, 3, A3, 3, 
        R, 1, R, 3, AS3, 4, B3, 6, C4, 1, R, 3, 
        C4, 3, R, 4, CS4, 1, D4, 2, R, 4, DS4, 5, 
        E4, 5, F4, 1, R, 3, F4, 1, FS4, 2, R, 2, 
        R, 2, G4, 2, GS4, 4, A4, 4, AS4, 1, R, 2, 
        R, 1, B4, 3, C5, 1, R, 2, R, 1, CS5, 2, 
        R, 2, R, 3, E5, 2, F5, 2, R, 1, R, 2, 
        G5, 2, GS5, 1, R, 1, R, 2, R, 1, B5, 2, 
        C6, 1, R, 1, R, 1, R, 1, R, 1, DS6, 1, 
        E6, 1, F6, 1, FS6, 1, G6, 1, GS6, 1, A6, 1);
    return t;
  }

  // Bars 4-11: the pump. Octave-pop on every beat with a painted
  // dip-and-swell envelope (v6..v1..v6), ghost ticks between, thin V00.
  // Register-log rows: {pitch, frames, volume 0-15}.
  private static Track p1Pump0() {
    Track t = new Track().withDefaults(V(6), D_V00).withDecay(SOSTENUTO);
    t.addSegs(V_STEP, new int[][] {
      {CS3, 1, 6}, {CS4, 1, 5}, {CS3, 1, 4}, {CS3, 1, 3}, {CS3, 1, 2}, {CS3, 1, 1},
      {CS3, 1, 2}, {CS3, 1, 3}, {CS3, 1, 4}, {CS3, 1, 5}, {CS3, 1, 6}, {R, 3, 0},
      {CS3, 1, 6}, {R, 6, 0}, {CS3, 1, 6}, {R, 6, 0}, {CS3, 1, 6}, {CS4, 1, 5},
      {CS3, 1, 4}, {CS3, 1, 3}, {CS3, 1, 2}, {CS3, 1, 1}, {CS3, 1, 2}, {CS3, 1, 3},
      {CS3, 1, 4}, {CS3, 1, 5}, {CS3, 1, 6}, {R, 3, 0}, {CS3, 1, 6}, {R, 6, 0},
      {CS3, 1, 6}, {R, 6, 0}, {B2, 1, 6}, {B3, 1, 5}, {B2, 1, 4}, {B2, 1, 3},
      {B2, 1, 2}, {B2, 1, 1}, {B2, 1, 2}, {CS3, 1, 6}, {CS4, 1, 5}, {CS3, 1, 4},
      {CS3, 1, 3}, {CS3, 1, 2}, {CS3, 1, 1}, {CS3, 1, 2}, {CS3, 1, 3}, {CS3, 1, 4},
      {CS3, 1, 5}, {CS3, 1, 6}, {R, 3, 0}, {B2, 1, 6}, {B3, 1, 5}, {B2, 1, 4},
      {B2, 1, 3}, {B2, 1, 2}, {B2, 1, 1}, {B2, 1, 2}, {E3, 1, 6}, {E4, 1, 5},
      {E3, 1, 4}, {E3, 1, 3}, {E3, 1, 2}, {E3, 1, 1}, {E3, 1, 2}, {E3, 1, 3},
      {E3, 1, 4}, {E3, 1, 5}, {E3, 1, 6}, {R, 3, 0}, {E3, 1, 6}, {E4, 1, 5},
      {E3, 1, 4}, {E3, 1, 3}, {E3, 1, 2}, {E3, 1, 1}, {E3, 1, 2}, {FS3, 1, 6},
      {FS4, 1, 5}, {FS3, 1, 4}, {FS3, 1, 3}, {FS3, 1, 2}, {FS3, 1, 1}, {FS3, 1, 2}
    });
    return t;
  }

  private static Track p1Pump1() {
    Track t = new Track().withDefaults(V(6), D_V00).withDecay(SOSTENUTO);
    t.addSegs(V_STEP, new int[][] {
      {CS3, 1, 6}, {CS4, 1, 5}, {CS3, 1, 4}, {CS3, 1, 3}, {CS3, 1, 2}, {CS3, 1, 1},
      {CS3, 1, 2}, {CS3, 1, 3}, {CS3, 1, 4}, {CS3, 1, 5}, {CS3, 1, 6}, {R, 3, 0},
      {CS3, 1, 6}, {R, 6, 0}, {CS3, 1, 6}, {R, 6, 0}, {CS3, 1, 6}, {CS4, 1, 5},
      {CS3, 1, 4}, {CS3, 1, 3}, {CS3, 1, 2}, {CS3, 1, 1}, {CS3, 1, 2}, {CS3, 1, 3},
      {CS3, 1, 4}, {CS3, 1, 5}, {CS3, 1, 6}, {R, 3, 0}, {CS3, 1, 6}, {R, 6, 0},
      {CS3, 1, 6}, {R, 6, 0}, {B2, 1, 6}, {B3, 1, 5}, {B2, 1, 4}, {B2, 1, 3},
      {B2, 1, 2}, {B2, 1, 1}, {B2, 1, 2}, {B2, 1, 3}, {B2, 1, 4}, {B2, 1, 5},
      {B2, 1, 6}, {R, 3, 0}, {B2, 1, 6}, {B3, 1, 5}, {B2, 1, 4}, {B2, 1, 3},
      {B2, 1, 2}, {B2, 1, 1}, {B2, 1, 2}, {CS3, 1, 6}, {CS4, 1, 5}, {CS3, 1, 4},
      {CS3, 1, 3}, {CS3, 1, 2}, {CS3, 1, 1}, {CS3, 1, 2}, {E3, 1, 6}, {E4, 1, 5},
      {E3, 1, 4}, {E3, 1, 3}, {E3, 1, 2}, {E3, 1, 1}, {E3, 1, 2}, {E3, 1, 3},
      {E3, 1, 4}, {E3, 1, 5}, {E3, 1, 6}, {R, 3, 0}, {E3, 1, 6}, {E4, 1, 5},
      {E3, 1, 4}, {E3, 1, 3}, {E3, 1, 2}, {E3, 1, 1}, {E3, 1, 2}, {DS3, 1, 6},
      {DS4, 1, 5}, {DS3, 1, 4}, {DS3, 1, 3}, {DS3, 1, 2}, {DS3, 1, 1}, {DS3, 1, 2}
    });
    return t;
  }

  private static Track p1Pump2() {
    Track t = new Track().withDefaults(V(6), D_V00).withDecay(SOSTENUTO);
    t.addSegs(V_STEP, new int[][] {
      {CS3, 1, 6}, {CS4, 1, 5}, {CS3, 1, 4}, {CS3, 1, 3}, {CS3, 1, 2}, {CS3, 1, 1},
      {CS3, 1, 2}, {CS3, 1, 3}, {CS3, 1, 4}, {CS3, 1, 5}, {CS3, 1, 6}, {R, 3, 0},
      {CS3, 1, 6}, {R, 6, 0}, {CS3, 1, 6}, {R, 6, 0}, {CS3, 1, 6}, {CS4, 1, 5},
      {CS3, 1, 4}, {CS3, 1, 3}, {CS3, 1, 2}, {CS3, 1, 1}, {CS3, 1, 2}, {CS3, 1, 3},
      {CS3, 1, 4}, {CS3, 1, 5}, {CS3, 1, 6}, {R, 3, 0}, {CS3, 1, 6}, {R, 6, 0},
      {CS3, 1, 6}, {R, 6, 0}, {B2, 1, 6}, {B3, 1, 5}, {B2, 1, 4}, {B2, 1, 3},
      {B2, 1, 2}, {B2, 1, 1}, {B2, 1, 2}, {CS3, 1, 6}, {CS4, 1, 5}, {CS3, 1, 4},
      {CS3, 1, 3}, {CS3, 1, 2}, {CS3, 1, 1}, {CS3, 1, 2}, {CS3, 1, 3}, {CS3, 1, 4},
      {CS3, 1, 5}, {CS3, 1, 6}, {R, 3, 0}, {B2, 1, 6}, {B3, 1, 5}, {B2, 1, 4},
      {B2, 1, 3}, {B2, 1, 2}, {B2, 1, 1}, {B2, 1, 2}, {E3, 1, 6}, {E4, 1, 5},
      {E3, 1, 4}, {E3, 1, 3}, {E3, 1, 2}, {E3, 1, 1}, {E3, 1, 2}, {E3, 1, 3},
      {E3, 1, 4}, {E3, 1, 5}, {E3, 1, 6}, {R, 3, 0}, {E3, 1, 6}, {E4, 1, 5},
      {E3, 1, 4}, {E3, 1, 3}, {E3, 1, 2}, {E3, 1, 1}, {E3, 1, 2}, {FS3, 1, 6},
      {FS4, 1, 5}, {FS3, 1, 4}, {FS3, 1, 3}, {FS3, 1, 2}, {FS3, 1, 1}, {FS3, 1, 2}
    });
    return t;
  }

  // Bar 12: the lead's entrance  a ghost pickup, a two-octave chromatic
  // rip (addRun), and the held DS5 under deep table-warble vibrato.
  private static Track p1B12() {
    Track t = new Track().withDefaults(V(1), D_V03).withDecay(SOSTENUTO);
    t.addNotes(E3, 1);
    t.withVolume(V(3));
    t.addRun(F3, D5, 1);
    t.withVibrato(1.0, 5.0, 4);
    t.addNotes(DS5, 89);
    t.withNoFx();
    return t;
  }

  // Bar 13: the DS5 rings on across the bar line, hands off through a v1
  // ghost to a second hold on E5 whose warble starts 12 frames in (the
  // vibrato delay models the capture's flat start exactly).
  private static Track p1B13() {
    Track t = new Track().withDefaults(V(3), D_V03).withDecay(SOSTENUTO);
    t.withVibrato(1.0, 5.0, 0);
    t.addNotes(DS5, 56);
    t.withNoFx();
    t.withVolume(V(1));
    t.addNotes(E5, 1);
    t.withVolume(V(3));
    t.withVibrato(1.0, 5.0, 12);
    t.addNotes(E5, 55);
    t.withNoFx();
    return t;
  }

  private static Track p1B14() {
    Track t = new Track().withDefaults(V(3), D_V03).withDecay(SOSTENUTO);
    t.withVolume(V(1));
    t.addNotes(F5, 1);
    t.withVolume(V(3));
    t.addNotes(FS5, 12, FS5, 2, G5, 1, FS5, 3, F5, 5, FS5, 3, 
        G5, 1, FS5, 3, F5, 5, FS5, 3, G5, 1, FS5, 2);
    t.withVolume(V(1));
    t.addNotes(F5, 1);
    t.withVolume(V(3));
    t.addNotes(F5, 6);
    t.withVolume(V(1));
    t.addNotes(E5, 1);
    t.withVolume(V(3));
    t.addNotes(E5, 6);
    t.withVolume(V(1));
    t.addNotes(DS5, 1);
    t.withVolume(V(3));
    t.addNotes(DS5, 12, DS5, 2, E5, 1, DS5, 4, D5, 3, DS5, 4, 
        E5, 1, DS5, 4, D5, 3, DS5, 4, E5, 1, DS5, 4, 
        D5, 3, DS5, 4, E5, 1, DS5, 4);
    return t;
  }

  private static Track p1B15() {
    Track t = new Track().withDefaults(V(3), D_V02).withDecay(SOSTENUTO);
    t.withVolume(V(3));
    t.addNotes(E5, 13, E5, 1, DS5, 1, D5, 1, CS5, 1, C5, 1, 
        B4, 1, AS4, 1, A4, 1, GS4, 1, G4, 1, FS4, 1, 
        F4, 1, E4, 2, E4, 1);
    t.withVolume(V(2));
    t.addNotes(E5, 13, E5, 1, DS5, 1, D5, 1, CS5, 1, C5, 1, 
        B4, 1, AS4, 1, A4, 1, GS4, 1, G4, 1, FS4, 1, 
        F4, 1, E4, 2, E4, 1);
    t.withVolume(V(3));
    t.addNotes(FS5, 13, FS5, 1, F5, 1, E5, 1, DS5, 1, D5, 1, 
        CS5, 1, C5, 1, B4, 1, AS4, 1, A4, 1, GS4, 1, 
        G4, 1, FS4, 2, FS4, 1);
    t.withVolume(V(2));
    t.addNotes(FS5, 13, FS5, 1, F5, 1, E5, 1, DS5, 1, D5, 1, 
        CS5, 1, C5, 1, B4, 1, AS4, 1, A4, 1, GS4, 1, 
        G4, 1, FS4, 2, FS4, 1);
    return t;
  }

  private static Track p1B16() {
    Track t = new Track().withDefaults(V(3), D_V03).withDecay(SOSTENUTO);
    t.withVolume(V(1));
    t.addNotes(GS4, 1);
    t.withVolume(V(3));
    t.addNotes(AS4, 1, C5, 1, D5, 1, E5, 1, FS5, 1, GS5, 7, 
        GS5, 2, A5, 1, GS5, 4, G5, 3, GS5, 4, A5, 1, 
        GS5, 4, G5, 3, GS5, 4, A5, 1, GS5, 4, G5, 3, 
        GS5, 4, A5, 1, GS5, 4, G5, 3, GS5, 4, A5, 1, 
        GS5, 4, G5, 3, GS5, 4, A5, 1, GS5, 4, G5, 3, 
        GS5, 4, A5, 1, GS5, 4, G5, 3, GS5, 4, A5, 1, 
        GS5, 4, G5, 3, GS5, 4, A5, 1);
    return t;
  }

  private static Track p1B17() {
    Track t = new Track().withDefaults(V(3), D_V03).withDecay(SOSTENUTO);
    t.withVolume(V(3));
    t.addNotes(GS5, 4, G5, 3, GS5, 4, A5, 1, GS5, 4, G5, 3, 
        GS5, 4, A5, 1, GS5, 4, G5, 3, GS5, 4, A5, 1, 
        GS5, 4, G5, 3, GS5, 4, A5, 1, GS5, 4, G5, 3, 
        GS5, 1);
    t.withVolume(V(1));
    t.addNotes(FS5, 1);
    t.withVolume(V(3));
    t.addNotes(E5, 1, D5, 1, C5, 1, B4, 9, B4, 8, AS4, 1, 
        B4, 11, AS4, 1, B4, 11, AS4, 1, B4, 10);
    return t;
  }

  private static Track p1B18() {
    Track t = new Track().withDefaults(V(3), D_V03).withDecay(SOSTENUTO);
    t.withVolume(V(1));
    t.addNotes(CS5, 1);
    t.withVolume(V(3));
    t.addNotes(CS5, 12, CS5, 8, C5, 1, CS5, 11, C5, 1, CS5, 8);
    t.withVolume(V(1));
    t.addNotes(DS5, 1);
    t.withVolume(V(3));
    t.addNotes(DS5, 6);
    t.withVolume(V(1));
    t.addNotes(E5, 1);
    t.withVolume(V(3));
    t.addNotes(E5, 6);
    t.withVolume(V(1));
    t.addNotes(DS5, 1);
    t.withVolume(V(3));
    t.addNotes(DS5, 12, DS5, 2, E5, 1, DS5, 5, D5, 1, DS5, 5, 
        E5, 1, DS5, 5, D5, 1, DS5, 5, E5, 1, DS5, 5, 
        D5, 1, DS5, 5, E5, 1, DS5, 4);
    return t;
  }

  private static Track p1B19() {
    Track t = new Track().withDefaults(V(3), D_V02).withDecay(SOSTENUTO);
    t.withVolume(V(3));
    t.addNotes(CS5, 13, CS5, 1, C5, 1, B4, 1, AS4, 1, A4, 1, 
        GS4, 1, G4, 1, FS4, 1, F4, 1, E4, 1, DS4, 1, 
        D4, 1, CS4, 2, CS4, 1);
    t.withVolume(V(2));
    t.addNotes(CS5, 13, CS5, 1, C5, 1, B4, 1, AS4, 1, A4, 1, 
        GS4, 1, G4, 1, FS4, 1, F4, 1, E4, 1, DS4, 1, 
        D4, 1, CS4, 2, CS4, 1);
    t.withVolume(V(3));
    t.addNotes(GS5, 13, GS5, 1, G5, 1, FS5, 1, F5, 1, E5, 1, 
        DS5, 1, D5, 1, CS5, 1, C5, 1, B4, 1, AS4, 1, 
        A4, 1, GS4, 2, GS4, 1);
    t.withVolume(V(2));
    t.addNotes(GS5, 13, GS5, 1, G5, 1, FS5, 1, F5, 1, E5, 1, 
        DS5, 1, D5, 1, CS5, 1, C5, 1, B4, 1, AS4, 1, 
        A4, 1, GS4, 2, GS4, 1);
    return t;
  }

  private static Track p1B20() {
    Track t = new Track().withDefaults(V(3), D_V03).withDecay(SOSTENUTO);
    t.withVolume(V(3));
    t.addNotes(CS6, 13, CS6, 1, C6, 1, B5, 1, AS5, 1, A5, 1, 
        GS5, 1, G5, 1, FS5, 1);
    t.withVolume(V(2));
    t.addNotes(CS6, 13, CS6, 1, C6, 1, B5, 1, AS5, 1, A5, 1, 
        GS5, 1, G5, 1, FS5, 1);
    t.withVolume(V(3));
    t.addNotes(B5, 13, B5, 1, AS5, 1, A5, 1, GS5, 1, G5, 1, 
        FS5, 1, F5, 1, E5, 1);
    t.withVolume(V(2));
    t.addNotes(B5, 13, B5, 1, AS5, 1, A5, 1, GS5, 1, G5, 1, 
        FS5, 1, F5, 1, E5, 1);
    t.withVolume(V(3));
    t.addNotes(GS5, 13, GS5, 1, G5, 1, FS5, 1, F5, 1, E5, 1, 
        DS5, 1, D5, 1, CS5, 1);
    t.withVolume(V(2));
    t.addNotes(GS5, 7);
    return t;
  }

  private static Track p1B21() {
    Track t = new Track().withDefaults(V(3), D_V03).withDecay(SOSTENUTO);
    t.withVolume(V(2));
    t.addNotes(GS5, 6, GS5, 1, G5, 1, FS5, 1, F5, 1, E5, 1, 
        DS5, 1, D5, 1, CS5, 1);
    t.withVolume(V(3));
    t.addNotes(FS5, 13, FS5, 1, F5, 1, E5, 1, DS5, 1, D5, 1, 
        CS5, 1, C5, 1, B4, 1);
    t.withVolume(V(2));
    t.addNotes(FS5, 13, FS5, 1, F5, 1, E5, 1, DS5, 1, D5, 1, 
        CS5, 1, C5, 1, B4, 1);
    t.withVolume(V(3));
    t.addNotes(E5, 13, E5, 1, DS5, 1, D5, 1, CS5, 1, C5, 1, 
        B4, 1, AS4, 1, A4, 1);
    t.withVolume(V(2));
    t.addNotes(E5, 7);
    t.withVolume(V(3));
    t.addNotes(DS5, 13, DS5, 1, D5, 1, CS5, 1, C5, 1, B4, 1, 
        AS4, 1, A4, 1, GS4, 1);
    t.withVolume(V(2));
    t.addNotes(DS5, 7);
    return t;
  }

  private static Track p1B22() {
    Track t = new Track().withDefaults(V(3), D_V03).withDecay(SOSTENUTO);
    t.withVolume(V(3));
    t.addNotes(CS5, 13, CS5, 1, C5, 1, B4, 1, AS4, 1, A4, 1, 
        GS4, 1, G4, 1, FS4, 1);
    t.withVolume(V(2));
    t.addNotes(CS5, 13, CS5, 1, C5, 1, B4, 1, AS4, 1, A4, 1, 
        GS4, 1, G4, 1, FS4, 1);
    t.withVolume(V(3));
    t.addNotes(B4, 13, B4, 1, AS4, 1, A4, 1, GS4, 1, G4, 1, 
        FS4, 1, F4, 1, E4, 1);
    t.withVolume(V(2));
    t.addNotes(B4, 13, B4, 1, AS4, 1, A4, 1, GS4, 1, G4, 1, 
        FS4, 1, F4, 1, E4, 1);
    t.withVolume(V(3));
    t.addNotes(GS4, 13, GS4, 1, G4, 1, FS4, 1, F4, 1, E4, 1, 
        DS4, 1, D4, 1, CS4, 1);
    t.withVolume(V(2));
    t.addNotes(GS4, 7);
    return t;
  }

  private static Track p1B23() {
    Track t = new Track().withDefaults(V(3), D_V03).withDecay(SOSTENUTO);
    t.withVolume(V(2));
    t.addNotes(GS4, 6, GS4, 1, G4, 1, FS4, 1, F4, 1, E4, 1, 
        DS4, 1, D4, 1, CS4, 1);
    t.withVolume(V(3));
    t.addNotes(FS4, 13, FS4, 1, F4, 1, E4, 1, DS4, 1, D4, 1, 
        CS4, 1, C4, 1, B3, 1);
    t.withVolume(V(2));
    t.addNotes(FS4, 13, FS4, 1, F4, 1, E4, 1, DS4, 1, D4, 1, 
        CS4, 1, C4, 1, B3, 1);
    t.withVolume(V(3));
    t.addNotes(GS4, 13, GS4, 1, G4, 1, FS4, 1, F4, 1, E4, 1, 
        DS4, 1, D4, 1, CS4, 1);
    t.withVolume(V(2));
    t.addNotes(GS4, 7);
    t.withVolume(V(3));
    t.addNotes(CS5, 13, CS5, 1, C5, 1, B4, 1, AS4, 1, A4, 1, 
        GS4, 1, G4, 1, FS4, 1);
    t.withVolume(V(2));
    t.addNotes(CS5, 7);
    return t;
  }

  private static Track p1B24() {
    Track t = new Track().withDefaults(V(3), D_V03).withDecay(SOSTENUTO);
    t.withVolume(V(3));
    t.addNotes(DS5, 13, DS5, 1, D5, 1, CS5, 1, C5, 1, B4, 1, 
        AS4, 1, A4, 1, GS4, 1);
    t.withVolume(V(2));
    t.addNotes(DS5, 13, DS5, 1, D5, 1, CS5, 1, C5, 1, B4, 1, 
        AS4, 1, A4, 1, GS4, 1);
    t.withVolume(V(3));
    t.addNotes(E5, 13, E5, 1, DS5, 1, D5, 1, CS5, 1, C5, 1, 
        B4, 1, AS4, 1, A4, 1);
    t.withVolume(V(2));
    t.addNotes(E5, 13, E5, 1, DS5, 1, D5, 1, CS5, 1, C5, 1, 
        B4, 1, AS4, 1, A4, 1);
    t.withVolume(V(3));
    t.addNotes(FS5, 13, FS5, 1, F5, 1, E5, 1, DS5, 1, D5, 1, 
        CS5, 1, C5, 1, B4, 1);
    t.withVolume(V(2));
    t.addNotes(FS5, 7);
    return t;
  }

  private static Track p1B25() {
    Track t = new Track().withDefaults(V(3), D_V03).withDecay(SOSTENUTO);
    t.withVolume(V(2));
    t.addNotes(FS5, 6, FS5, 1, F5, 1, E5, 1, DS5, 1, D5, 1, 
        CS5, 1, C5, 1, B4, 1);
    t.withVolume(V(3));
    t.addNotes(E5, 13, E5, 1, DS5, 1, D5, 1, CS5, 1, C5, 1, 
        B4, 1, AS4, 1, A4, 1);
    t.withVolume(V(2));
    t.addNotes(E5, 13, E5, 1, DS5, 1, D5, 1, CS5, 1, C5, 1, 
        B4, 1, AS4, 1, A4, 1);
    t.withVolume(V(3));
    t.addNotes(DS5, 13, DS5, 1, D5, 1, CS5, 1, C5, 1, B4, 1, 
        AS4, 1, A4, 1, GS4, 1);
    t.withVolume(V(2));
    t.addNotes(DS5, 7);
    t.withVolume(V(3));
    t.addNotes(E5, 13, E5, 1, DS5, 1, D5, 1, CS5, 1, C5, 1, 
        B4, 1, AS4, 1, A4, 1);
    t.withVolume(V(2));
    t.addNotes(E5, 7);
    return t;
  }

  private static Track p1B26() {
    Track t = new Track().withDefaults(V(3), D_V03).withDecay(SOSTENUTO);
    t.withVolume(V(3));
    t.addNotes(CS5, 13, CS5, 1, C5, 1, B4, 1, AS4, 1, A4, 1, 
        GS4, 1, G4, 1, FS4, 1);
    t.withVolume(V(2));
    t.addNotes(CS5, 7);
    t.withVolume(V(3));
    t.addNotes(B4, 13, B4, 1, AS4, 1, A4, 1, GS4, 1, G4, 1, 
        FS4, 1, F4, 1, E4, 1);
    t.withVolume(V(2));
    t.addNotes(B4, 7);
    t.withVolume(V(3));
    t.addNotes(CS5, 13, CS5, 1, C5, 1, B4, 1, AS4, 1, A4, 1, 
        GS4, 1, G4, 1, FS4, 1);
    t.withVolume(V(2));
    t.addNotes(CS5, 7);
    t.withVolume(V(3));
    t.addNotes(DS5, 13, DS5, 1, D5, 1, CS5, 1, C5, 1, B4, 1, 
        AS4, 1, A4, 1, GS4, 1);
    t.withVolume(V(2));
    t.addNotes(DS5, 7);
    return t;
  }

  private static Track p1B27() {
    Track t = new Track().withDefaults(V(3), D_V03).withDecay(SOSTENUTO);
    t.withVolume(V(3));
    t.addNotes(E5, 13, E5, 1, DS5, 1, D5, 1, CS5, 1, C5, 1, 
        B4, 1, AS4, 1, A4, 1);
    t.withVolume(V(2));
    t.addNotes(E5, 7);
    t.withVolume(V(3));
    t.addNotes(FS5, 13, FS5, 1, F5, 1, E5, 1, DS5, 1, D5, 1, 
        CS5, 1, C5, 1, B4, 1);
    t.withVolume(V(2));
    t.addNotes(FS5, 7);
    t.withVolume(V(3));
    t.addNotes(GS5, 13, GS5, 1, G5, 1, FS5, 1, F5, 1, E5, 1, 
        DS5, 1, D5, 1, CS5, 1);
    t.withVolume(V(2));
    t.addNotes(GS5, 7);
    t.withVolume(V(3));
    t.addNotes(B5, 13, B5, 1, AS5, 1, A5, 1, GS5, 1, G5, 1, 
        FS5, 1, F5, 1, E5, 1);
    t.withVolume(V(2));
    t.addNotes(B5, 7);
    return t;
  }

  private static Track p1B28() {
    Track t = new Track().withDefaults(V(3), D_V00).withDecay(SOSTENUTO);
    t.withVolume(V(3));
    t.addNotes(CS6, 13, C6, 2, B5, 1, AS5, 1, A5, 1, GS5, 1, 
        G5, 1, FS5, 1, F5, 1, E5, 1, DS5, 1, D5, 1, 
        CS5, 2, CS5, 1, C5, 3, B4, 3, AS4, 3, A4, 4, 
        GS4, 1, B5, 13, AS5, 2, A5, 1, GS5, 1, G5, 1, 
        FS5, 1, F5, 1, E5, 1, DS5, 1, D5, 1, CS5, 1, 
        C5, 1, B4, 2, B4, 1, AS4, 3, A4, 4, GS4, 4, 
        G4, 3, CS6, 13, C6, 2, B5, 1, AS5, 1, A5, 1, 
        GS5, 1, G5, 1, FS5, 1, F5, 1, E5, 1, DS5, 1, 
        D5, 1, CS5, 2, CS5, 1);
    return t;
  }

  private static Track p1B29() {
    Track t = new Track().withDefaults(V(3), D_V00).withDecay(SOSTENUTO);
    t.withVolume(V(3));
    t.addNotes(C5, 3, B4, 3, AS4, 3, A4, 4, GS4, 1, DS6, 13, 
        D6, 2, CS6, 1, C6, 1, B5, 1, AS5, 1, A5, 1, 
        GS5, 1, G5, 1, FS5, 1, F5, 1, E5, 1, DS5, 2, 
        D5, 3, CS5, 3, C5, 3, B4, 3, AS4, 3, E6, 13, 
        DS6, 2, D6, 1, CS6, 1, C6, 1, B5, 1, AS5, 1, 
        A5, 1, GS5, 1, G5, 1, FS5, 1, F5, 1, E5, 2, 
        DS5, 1, DS6, 13, D6, 2, CS6, 1, C6, 1, B5, 1, 
        AS5, 1, A5, 1, GS5, 1, G5, 1, FS5, 1, F5, 1, 
        E5, 1, DS5, 2, D5, 1);
    return t;
  }

  private static Track p1B30() {
    Track t = new Track().withDefaults(V(3), D_V00).withDecay(SOSTENUTO);
    t.withVolume(V(3));
    t.addNotes(CS6, 13, C6, 2, B5, 1, AS5, 1, A5, 1, GS5, 1, 
        G5, 1, FS5, 1, F5, 1, E5, 1, DS5, 1, D5, 1, 
        CS5, 2, CS5, 1, C5, 3, B4, 3, AS4, 3, A4, 4, 
        GS4, 1, B5, 13, AS5, 2, A5, 1, GS5, 1, G5, 1, 
        FS5, 1, F5, 1, E5, 1, DS5, 1, D5, 1, CS5, 1, 
        C5, 1, B4, 2, B4, 1, AS4, 3, A4, 4, GS4, 4, 
        G4, 3, CS6, 13, C6, 2, B5, 1, AS5, 1, A5, 1, 
        GS5, 1, G5, 1, FS5, 1, F5, 1, E5, 1, DS5, 1, 
        D5, 1, CS5, 2, CS5, 1);
    return t;
  }

  private static Track p1B31() {
    Track t = new Track().withDefaults(V(3), D_V00).withDecay(SOSTENUTO);
    t.withVolume(V(3));
    t.addNotes(C5, 3, B4, 3, AS4, 3, A4, 4, GS4, 1, DS6, 13, 
        D6, 2, CS6, 1, C6, 1, B5, 1, AS5, 1, A5, 1, 
        GS5, 1, G5, 1, FS5, 1, F5, 1, E5, 1, DS5, 2, 
        D5, 3, CS5, 3, C5, 3, B4, 3, AS4, 3, E6, 13, 
        DS6, 2, D6, 1, CS6, 1, C6, 1, B5, 1, AS5, 1, 
        A5, 1, GS5, 1, G5, 1, FS5, 1, F5, 1, E5, 2, 
        DS5, 1, DS6, 13, D6, 2, CS6, 1, C6, 1, B5, 1, 
        AS5, 1, A5, 1, GS5, 1, G5, 1, FS5, 1, F5, 1, 
        E5, 1, DS5, 2, D5, 1);
    return t;
  }

  // Bars 32-35: the duty dialogue  P1 and P2 trade the same phrase with
  // the timbre register (V02/V03/V00) choreographed mid-bar.
  private static Track p1Echo() {
    Track t = new Track().withDefaults(V(3), D_V02).withDecay(SOSTENUTO);
    t.withDuty(D_V02);
    t.withVolume(V(3));
    t.addNotes(CS6, 13, C6, 2, B5, 1, AS5, 1, A5, 1, GS5, 1, 
        G5, 1, FS5, 1, F5, 1, E5, 1, DS5, 1, D5, 1, 
        CS5, 2, CS5, 1, C5, 3, B4, 3, AS4, 3, A4, 4, 
        GS4, 1, B5, 13, AS5, 2, A5, 1, GS5, 1, G5, 1, 
        FS5, 1, F5, 1, E5, 1, DS5, 1, D5, 1, CS5, 1, 
        C5, 1, B4, 2, B4, 1, AS4, 3, A4, 4, GS4, 4, 
        G4, 3);
    t.withDuty(D_V03);
    t.withVolume(V(3));
    t.addNotes(CS6, 13, C6, 2, B5, 1, AS5, 1, A5, 1, GS5, 1, 
        G5, 1, FS5, 1, F5, 1, E5, 1, DS5, 1, D5, 1, 
        CS5, 2, CS5, 1, C5, 3, B4, 3, AS4, 3, A4, 4, 
        GS4, 1, DS6, 13, D6, 2, CS6, 1, C6, 1, B5, 1, 
        AS5, 1, A5, 1, GS5, 1, G5, 1, FS5, 1, F5, 1, 
        E5, 1, DS5, 2, D5, 3, CS5, 3, C5, 3, B4, 3, 
        AS4, 3);
    t.withDuty(D_V00);
    t.withVolume(V(3));
    t.addNotes(E6, 13, DS6, 2, D6, 1, CS6, 1, C6, 1, B5, 1, 
        AS5, 1, A5, 1, GS5, 1, G5, 1, FS5, 1, F5, 1, 
        E5, 2, DS5, 1);
    t.withDuty(D_V03);
    t.withVolume(V(3));
    t.addNotes(DS6, 13, D6, 2, CS6, 1, C6, 1, B5, 1, AS5, 1, 
        A5, 1, GS5, 1, G5, 1, FS5, 1, F5, 1, E5, 1, 
        DS5, 2, D5, 1);
    t.withDuty(D_V02);
    t.withVolume(V(3));
    t.addNotes(CS6, 13, C6, 2, B5, 1, AS5, 1, A5, 1, GS5, 1, 
        G5, 1, FS5, 1, F5, 1, E5, 1, DS5, 1, D5, 1, 
        CS5, 2, CS5, 1, C5, 3, B4, 3, AS4, 3, A4, 4, 
        GS4, 1, B5, 13, AS5, 2, A5, 1, GS5, 1, G5, 1, 
        FS5, 1, F5, 1, E5, 1, DS5, 1, D5, 1, CS5, 1, 
        C5, 1, B4, 2, B4, 1, AS4, 3, A4, 4, GS4, 4, 
        G4, 3);
    t.withDuty(D_V03);
    t.withVolume(V(3));
    t.addNotes(CS6, 13, C6, 2, B5, 1, AS5, 1, A5, 1, GS5, 1, 
        G5, 1, FS5, 1, F5, 1, E5, 1, DS5, 1, D5, 1, 
        CS5, 2, CS5, 1, C5, 3, B4, 3, AS4, 3, A4, 4, 
        GS4, 1, DS6, 13, D6, 2, CS6, 1, C6, 1, B5, 1, 
        AS5, 1, A5, 1, GS5, 1, G5, 1, FS5, 1, F5, 1, 
        E5, 1, DS5, 2, D5, 3, CS5, 3, C5, 3, B4, 3, 
        AS4, 3);
    t.withDuty(D_V00);
    t.withVolume(V(3));
    t.addNotes(E6, 13, DS6, 2, D6, 1, CS6, 1, C6, 1, B5, 1, 
        AS5, 1, A5, 1, GS5, 1, G5, 1, FS5, 1, F5, 1, 
        E5, 2, DS5, 1);
    t.withDuty(D_V03);
    t.withVolume(V(3));
    t.addNotes(DS6, 13, D6, 2, CS6, 1, C6, 1, B5, 1, AS5, 1, 
        A5, 1, GS5, 1, G5, 1, FS5, 1, F5, 1, E5, 1, 
        DS5, 2, D5, 1);
    return t;
  }

  private static Track p1Comedown() {
    Track t = new Track().withDefaults(V(3), D_V02).withDecay(SOSTENUTO);
    t.addSegs(V_STEP, new int[][] {
      {CS3, 4, 3}, {CS3, 1, 2}, {CS3, 1, 1}, {R, 1, 0}, {B2, 4, 3}, {B2, 1, 2},
      {B2, 1, 1}, {R, 1, 0}, {CS3, 4, 3}, {CS3, 1, 2}, {CS3, 1, 1}, {R, 1, 0},
      {E3, 4, 3}, {E3, 1, 2}, {E3, 1, 1}, {R, 1, 0}, {FS3, 4, 3}, {FS3, 1, 2},
      {FS3, 1, 1}, {R, 1, 0}, {GS3, 4, 3}, {GS3, 1, 2}, {GS3, 1, 1}, {R, 1, 0},
      {B3, 4, 3}, {B3, 1, 2}, {B3, 1, 1}, {R, 1, 0}, {CS4, 4, 3}, {CS4, 1, 2},
      {CS4, 1, 1}, {R, 1, 0}, {E4, 4, 3}, {E4, 1, 2}, {E4, 1, 1}, {R, 1, 0},
      {B3, 4, 3}, {B3, 1, 2}, {B3, 1, 1}, {R, 1, 0}, {AS3, 4, 3}, {AS3, 1, 2},
      {AS3, 1, 1}, {R, 1, 0}, {CS4, 4, 3}, {CS4, 1, 2}, {CS4, 1, 1}, {R, 1, 0},
      {GS3, 4, 3}, {GS3, 1, 2}, {GS3, 1, 1}, {R, 1, 0}, {E3, 4, 3}, {E3, 1, 2},
      {E3, 1, 1}, {R, 1, 0}, {DS3, 4, 3}, {DS3, 1, 2}, {DS3, 1, 1}, {R, 1, 0},
      {FS3, 4, 3}, {FS3, 1, 2}, {FS3, 1, 1}, {R, 1, 0}, {CS3, 4, 3}, {CS3, 1, 2},
      {CS3, 1, 1}, {R, 1, 0}, {B2, 4, 3}, {B2, 1, 2}, {B2, 1, 1}, {R, 1, 0},
      {CS3, 4, 3}, {CS3, 1, 2}, {CS3, 1, 1}, {R, 1, 0}, {E3, 4, 3}, {E3, 1, 2},
      {E3, 1, 1}, {R, 1, 0}, {FS3, 4, 3}, {FS3, 1, 2}, {FS3, 1, 1}, {R, 1, 0},
      {GS3, 4, 3}, {GS3, 1, 2}, {GS3, 1, 1}, {R, 1, 0}, {B3, 4, 3}, {B3, 1, 2},
      {B3, 1, 1}, {R, 1, 0}, {CS4, 4, 3}, {CS4, 1, 2}, {CS4, 1, 1}, {R, 1, 0},
      {E4, 4, 3}, {E4, 1, 2}, {E4, 1, 1}, {R, 1, 0}, {B3, 4, 3}, {B3, 1, 2},
      {B3, 1, 1}, {R, 1, 0}, {AS3, 4, 3}, {AS3, 1, 2}, {AS3, 1, 1}, {R, 1, 0},
      {CS4, 4, 3}, {CS4, 1, 2}, {CS4, 1, 1}, {R, 1, 0}, {GS3, 4, 3}, {GS3, 1, 2},
      {GS3, 1, 1}, {R, 1, 0}, {E3, 4, 3}, {E3, 1, 2}, {E3, 1, 1}, {R, 1, 0},
      {DS3, 4, 3}, {DS3, 1, 2}, {DS3, 1, 1}, {R, 1, 0}, {FS3, 4, 3}, {FS3, 1, 2},
      {FS3, 1, 1}, {R, 1, 0}
    });
    return t;
  }

  // Bars 38-47: one vamp bar, hammered ten times under the outro washes.
  private static Track p1Vamp() {
    Track t = new Track().withDefaults(V(3), D_V02).withDecay(SOSTENUTO);
    t.addSegs(V_STEP, new int[][] {
      {CS3, 1, 3}, {GS4, 1, 3}, {CS3, 2, 3}, {CS3, 1, 2}, {CS3, 1, 1}, {R, 1, 0},
      {B2, 1, 3}, {FS4, 1, 3}, {B2, 2, 3}, {B2, 1, 2}, {B2, 1, 1}, {R, 1, 0},
      {CS3, 1, 3}, {GS4, 1, 3}, {CS3, 2, 3}, {CS3, 1, 2}, {CS3, 1, 1}, {R, 1, 0},
      {E3, 1, 3}, {B4, 1, 3}, {E3, 2, 3}, {E3, 1, 2}, {E3, 1, 1}, {R, 1, 0},
      {FS3, 1, 3}, {CS5, 1, 3}, {FS3, 2, 3}, {FS3, 1, 2}, {FS3, 1, 1}, {R, 1, 0},
      {GS3, 1, 3}, {DS5, 1, 3}, {GS3, 2, 3}, {GS3, 1, 2}, {GS3, 1, 1}, {R, 1, 0},
      {B3, 1, 3}, {FS5, 1, 3}, {B3, 2, 3}, {B3, 1, 2}, {B3, 1, 1}, {R, 1, 0},
      {CS4, 1, 3}, {GS5, 1, 3}, {CS4, 2, 3}, {CS4, 1, 2}, {CS4, 1, 1}, {R, 1, 0},
      {E4, 1, 3}, {B5, 1, 3}, {E4, 2, 3}, {E4, 1, 2}, {E4, 1, 1}, {R, 1, 0},
      {B3, 1, 3}, {FS5, 1, 3}, {B3, 2, 3}, {B3, 1, 2}, {B3, 1, 1}, {R, 1, 0},
      {AS3, 1, 3}, {F5, 1, 3}, {AS3, 2, 3}, {AS3, 1, 2}, {AS3, 1, 1}, {R, 1, 0},
      {CS4, 1, 3}, {GS5, 1, 3}, {CS4, 2, 3}, {CS4, 1, 2}, {CS4, 1, 1}, {R, 1, 0},
      {GS3, 1, 3}, {DS5, 1, 3}, {GS3, 2, 3}, {GS3, 1, 2}, {GS3, 1, 1}, {R, 1, 0},
      {E3, 1, 3}, {B4, 1, 3}, {E3, 2, 3}, {E3, 1, 2}, {E3, 1, 1}, {R, 1, 0},
      {DS3, 1, 3}, {AS4, 1, 3}, {DS3, 2, 3}, {DS3, 1, 2}, {DS3, 1, 1}, {R, 1, 0},
      {FS3, 1, 3}, {CS5, 1, 3}, {FS3, 2, 3}, {FS3, 1, 2}, {FS3, 1, 1}, {R, 1, 0}
    });
    return t;
  }

  /* ==================== HARMONY (pulse 2) ==================== */

  @Override
  public Track getHarmony() {
    Track t = new Track().withDefaults(V(5), D_V00);
    t.addNotes(p2Head());
    t.addNotes(p2Intro());                    // bars 0-3, gated cascade
    t.addNotes(p2B4());                       // bars 4-8, P2's pump double
    t.addNotes(p2B5());
    t.addNotes(p2B6());
    t.addNotes(p2B7());
    t.addNotes(p2B8());
    for (int i = 0; i < 4; i++) {             // bars 9-24: I J K L x4
      t.addNotes(washBar(GS4, E4, CS4, A3));
      t.addNotes(washBar(FS4, DS4, B3, FS3));
      t.addNotes(washBar(E4, CS4, A3, E3));
      t.addNotes(washBar(GS4, E4, B3, GS3));
    }
    t.addNotes(washBar(GS4, E4, CS4, A3));    // bars 25-27: I J K
    t.addNotes(washBar(FS4, DS4, B3, FS3));
    t.addNotes(washBar(E4, CS4, A3, E3));
    t.addNotes(p2S2829());                    // comet falls
    t.addNotes(p2S3031());
    t.addNotes(p2Echo());                     // bars 32-35 + duty sliver
    t.addNotes(p2Comedown());                 // rest of 36 + 37
    t.addNotes(p2S3839());
    t.addNotes(p2V());                        // bars 40-47: V V W W X X Y Y
    t.addNotes(p2V());
    t.addNotes(p2W());
    t.addNotes(p2W());
    t.addNotes(p2X());
    t.addNotes(p2X());
    t.addNotes(p2Y());
    t.addNotes(p2Y());
    return t;
  }

  // Bars 9-27: the wash. A four-note chord arpeggio at 2 frames per note
  // (56 notes a bar) climbing a v1..v6 staircase that steps every 21
  // frames  each bar swells from whisper to full. Frame-exact to the
  // capture, including the volume steps that land mid-note.
  private static Track washBar(int a, int b, int c, int d) {
    Track t = new Track().withDefaults(V(1), D_V00).withDecay(SOSTENUTO);
    int[] cyc = {a, b, c, d};
    for (int f = 0; f < W; f++) {
      t.withVolume(V(1 + f / 21));
      t.addNotes(cyc[(f / 2) % 4], 1);
    }
    return t;
  }

  // Play-once head: the seven-note chord waterfall  C8 F#7 D7 B6 G#6 F#6
  // E6 at one note PER FRAME, cycling, then the widened variant. Raw MIDI
  // numbers are notes above the named-constant range (C8 = 108).
  private static Track p2Head() {
    Track t = new Track().withDefaults(V(5), D_V00).withDecay(SOSTENUTO);
    t.withVolume(V(5));
    t.addNotes(108, 1, 102, 1, 98, 1, B6, 1, GS6, 1, FS6, 1, 
        E6, 1, 108, 1, 102, 1, 98, 1, B6, 1, GS6, 1, 
        FS6, 1, E6, 1, 108, 1, 102, 1, 98, 1, B6, 1, 
        GS6, 1, FS6, 1, E6, 1, 108, 1, 102, 1, 98, 1, 
        B6, 1, GS6, 1, FS6, 1, E6, 1, 111, 1, 108, 1, 
        105, 1, 102, 1, 100, 1, 98, 1, 96, 1, B6, 1, 
        A6, 1, GS6, 1, G6, 1, FS6, 1, F6, 1, E6, 1, 
        DS6, 1, D6, 1, CS6, 1, C6, 1, B5, 1, AS5, 2, 
        A5, 1, GS5, 2, G5, 1, FS5, 2, F5, 1, 111, 1, 
        108, 1, 105, 1, 102, 1, 100, 1, 98, 1, 96, 1, 
        111, 1, 108, 1, 105, 1, 102, 1, 100, 1, 98, 1, 
        96, 1, 111, 1, 108, 1, 105, 1, 102, 1, 100, 1, 
        98, 1, 96, 1, B6, 1, A6, 1, GS6, 1, G6, 1, 
        FS6, 1, F6, 1, E6, 1, 111, 1, 108, 1, 105, 1, 
        102, 1, 100, 1, 98, 1, 96, 1, B6, 1, A6, 1, 
        GS6, 1, G6, 1, FS6, 1, F6, 1, E6, 1, DS6, 1, 
        D6, 1, CS6, 1, C6, 1, B5, 1, AS5, 2, A5, 1, 
        GS5, 2, G5, 1, FS5, 2, F5, 1);
    return t;
  }

  private static Track p2Intro() {
    Track t = new Track().withDefaults(V(3), D_V00).withDecay(SOSTENUTO);
    t.withVolume(V(3));
    t.addNotes(115, 1, 114, 1, 113, 1, 112, 1, R, 1, R, 2, 
        109, 1, 108, 2, R, 1, R, 2, R, 1, 105, 1, 
        104, 2, R, 2, R, 2, 101, 2, 100, 3, 99, 3, 
        98, 2, 97, 1, R, 2, R, 1, 96, 2, B6, 1, 
        R, 3, R, 1, AS6, 2, A6, 1, R, 3, R, 1, 
        GS6, 2, G6, 5, FS6, 4, R, 3, F6, 2, E6, 1, 
        R, 4, DS6, 5, D6, 5, CS6, 1, R, 3, CS6, 2, 
        C6, 2, R, 3, C6, 1, B5, 1, R, 5, B5, 1, 
        AS5, 3, R, 3, AS5, 1, A5, 2, R, 4, A5, 1, 
        GS5, 2, R, 4, GS5, 2, G5, 8, FS5, 1, R, 3, 
        FS5, 3, R, 2, R, 2, F5, 3, R, 4, E5, 10, 
        DS5, 1, R, 3, DS5, 3, R, 3, R, 1, D5, 10, 
        CS5, 1, R, 3, CS5, 4, R, 3, CS5, 1, C5, 1, 
        R, 5, C5, 4, R, 2, R, 1, B4, 3, R, 4, 
        B4, 3, R, 2, R, 2, AS4, 7, A4, 4, R, 3, 
        A4, 1, AS4, 2, R, 4, AS4, 3, R, 4, AS4, 1, 
        B4, 10, R, 3, C5, 3, R, 4, C5, 3, R, 2, 
        R, 2, CS5, 10, D5, 1, R, 3, D5, 3, R, 4, 
        DS5, 10, E5, 1, R, 3, E5, 4, R, 2, R, 1, 
        F5, 2, R, 5, F5, 1, FS5, 3, R, 3, FS5, 3, 
        R, 4, G5, 3, R, 1, R, 3, GS5, 5, A5, 6, 
        R, 1, R, 2, AS5, 3, R, 2, R, 2, B5, 3, 
        R, 2, R, 2, C6, 4, CS6, 6, D6, 1, R, 3, 
        D6, 1, DS6, 2, R, 3, R, 1, E6, 4, F6, 5, 
        FS6, 2, R, 2, R, 1, G6, 4, R, 3, A6, 2, 
        R, 2, R, 3, B6, 4, R, 3, 97, 3, R, 2, 
        R, 2, 99, 1, 100, 2, R, 1, R, 2, R, 1, 
        102, 1, 103, 2, 104, 2, 105, 2);
    return t;
  }

  private static Track p2B4() {
    Track t = new Track().withDefaults(V(3), D_V00).withDecay(SOSTENUTO);
    t.addSegs(V_STEP, new int[][] {
      {CS3, 1, 6}, {CS3, 1, 5}, {CS3, 1, 4}, {CS3, 1, 3}, {CS3, 1, 2}, {CS3, 1, 1},
      {CS3, 1, 2}, {CS3, 1, 3}, {CS3, 1, 4}, {CS3, 1, 5}, {CS3, 1, 6}, {R, 3, 0},
      {CS3, 1, 6}, {R, 6, 0}, {CS3, 1, 6}, {R, 6, 0}, {CS3, 1, 6}, {CS3, 1, 5},
      {CS3, 1, 4}, {CS3, 1, 3}, {CS3, 1, 2}, {CS3, 1, 1}, {CS3, 1, 2}, {CS3, 1, 3},
      {CS3, 1, 4}, {CS3, 1, 5}, {CS3, 1, 6}, {R, 3, 0}, {CS3, 1, 6}, {R, 6, 0},
      {CS3, 1, 6}, {R, 6, 0}, {B2, 1, 6}, {B2, 1, 5}, {B2, 1, 4}, {B2, 1, 3},
      {B2, 1, 2}, {B2, 1, 1}, {B2, 1, 2}, {CS3, 1, 6}, {CS3, 1, 5}, {CS3, 1, 4},
      {CS3, 1, 3}, {CS3, 1, 2}, {CS3, 1, 1}, {CS3, 1, 2}, {CS3, 1, 3}, {CS3, 1, 4},
      {CS3, 1, 5}, {CS3, 1, 6}, {R, 3, 0}, {B2, 1, 6}, {B2, 1, 5}, {B2, 1, 4},
      {B2, 1, 3}, {B2, 1, 2}, {B2, 1, 1}, {B2, 1, 2}, {E3, 1, 6}, {E3, 1, 5},
      {E3, 1, 4}, {E3, 1, 3}, {E3, 1, 2}, {E3, 1, 1}, {E3, 1, 2}, {E3, 1, 3},
      {E3, 1, 4}, {E3, 1, 5}, {E3, 1, 6}, {R, 3, 0}, {E3, 1, 6}, {E3, 1, 5},
      {E3, 1, 4}, {E3, 1, 3}, {E3, 1, 2}, {E3, 1, 1}, {E3, 1, 2}, {FS3, 1, 6},
      {FS3, 1, 5}, {FS3, 1, 4}, {FS3, 1, 3}, {FS3, 1, 2}, {FS3, 1, 1}, {FS3, 1, 2}
    });
    return t;
  }

  private static Track p2B5() {
    Track t = new Track().withDefaults(V(3), D_V00).withDecay(SOSTENUTO);
    t.addSegs(V_STEP, new int[][] {
      {CS3, 1, 6}, {CS3, 1, 5}, {CS3, 1, 4}, {CS3, 1, 3}, {CS3, 1, 2}, {CS3, 1, 1},
      {CS3, 1, 2}, {CS3, 1, 3}, {CS3, 1, 4}, {CS3, 1, 5}, {CS3, 1, 6}, {R, 3, 0},
      {CS3, 1, 6}, {R, 6, 0}, {CS3, 1, 6}, {R, 6, 0}, {CS3, 1, 6}, {CS3, 1, 5},
      {CS3, 1, 4}, {CS3, 1, 3}, {CS3, 1, 2}, {CS3, 1, 1}, {CS3, 1, 2}, {CS3, 1, 3},
      {CS3, 1, 4}, {CS3, 1, 5}, {CS3, 1, 6}, {R, 3, 0}, {CS3, 1, 6}, {R, 6, 0},
      {CS3, 1, 6}, {R, 6, 0}, {B2, 1, 6}, {B2, 1, 5}, {B2, 1, 4}, {B2, 1, 3},
      {B2, 1, 2}, {B2, 1, 1}, {B2, 1, 2}, {B2, 1, 3}, {B2, 1, 4}, {B2, 1, 5},
      {B2, 1, 6}, {R, 3, 0}, {B2, 1, 6}, {B2, 1, 5}, {B2, 1, 4}, {B2, 1, 3},
      {B2, 1, 2}, {B2, 1, 1}, {B2, 1, 2}, {CS3, 1, 6}, {CS3, 1, 5}, {CS3, 1, 4},
      {CS3, 1, 3}, {CS3, 1, 2}, {CS3, 1, 1}, {CS3, 1, 2}, {E3, 1, 6}, {E3, 1, 5},
      {E3, 1, 4}, {E3, 1, 3}, {E3, 1, 2}, {E3, 1, 1}, {E3, 1, 2}, {E3, 1, 3},
      {E3, 1, 4}, {E3, 1, 5}, {E3, 1, 6}, {R, 3, 0}, {E3, 1, 6}, {E3, 1, 5},
      {E3, 1, 4}, {E3, 1, 3}, {E3, 1, 2}, {E3, 1, 1}, {E3, 1, 2}, {DS3, 1, 6},
      {DS3, 1, 5}, {DS3, 1, 4}, {DS3, 1, 3}, {DS3, 1, 2}, {DS3, 1, 1}, {DS3, 1, 2}
    });
    return t;
  }

  private static Track p2B6() {
    Track t = new Track().withDefaults(V(3), D_V02).withDecay(SOSTENUTO);
    t.addSegs(V_STEP, new int[][] {
      {CS3, 1, 6}, {CS3, 1, 5}, {CS3, 1, 4}, {CS3, 1, 3}, {CS3, 1, 2}, {CS3, 1, 1},
      {CS3, 1, 2}, {CS3, 1, 3}, {CS3, 1, 4}, {CS3, 1, 5}, {CS3, 1, 6}, {R, 3, 0},
      {CS3, 1, 6}, {R, 6, 0}, {CS3, 1, 6}, {R, 6, 0}, {CS3, 1, 6}, {CS3, 1, 5},
      {CS3, 1, 4}, {CS3, 1, 3}, {CS3, 1, 2}, {CS3, 1, 1}, {CS3, 1, 2}, {CS3, 1, 3},
      {CS3, 1, 4}, {CS3, 1, 5}, {CS3, 1, 6}, {R, 3, 0}, {CS3, 1, 6}, {R, 6, 0},
      {CS3, 1, 6}, {R, 6, 0}, {B2, 1, 6}, {B2, 1, 5}, {B2, 1, 4}, {B2, 1, 3},
      {B2, 1, 2}, {B2, 1, 1}, {B2, 1, 2}, {CS3, 1, 6}, {CS3, 1, 5}, {CS3, 1, 4},
      {CS3, 1, 3}, {CS3, 1, 2}, {CS3, 1, 1}, {CS3, 1, 2}, {CS3, 1, 3}, {CS3, 1, 4},
      {CS3, 1, 5}, {CS3, 1, 6}, {R, 3, 0}, {B2, 1, 6}, {B2, 1, 5}, {B2, 1, 4},
      {B2, 1, 3}, {B2, 1, 2}, {B2, 1, 1}, {B2, 1, 2}, {E3, 1, 6}, {E3, 1, 5},
      {E3, 1, 4}, {E3, 1, 3}, {E3, 1, 2}, {E3, 1, 1}, {E3, 1, 2}, {E3, 1, 3},
      {E3, 1, 4}, {E3, 1, 5}, {E3, 1, 6}, {R, 3, 0}, {E3, 1, 6}, {E3, 1, 5},
      {E3, 1, 4}, {E3, 1, 3}, {E3, 1, 2}, {E3, 1, 1}, {E3, 1, 2}, {FS3, 1, 6},
      {FS3, 1, 5}, {FS3, 1, 4}, {FS3, 1, 3}, {FS3, 1, 2}, {FS3, 1, 1}, {FS3, 1, 2}
    });
    return t;
  }

  private static Track p2B7() {
    Track t = new Track().withDefaults(V(3), D_V02).withDecay(SOSTENUTO);
    t.addSegs(V_STEP, new int[][] {
      {CS3, 1, 6}, {CS3, 1, 5}, {CS3, 1, 4}, {CS3, 1, 3}, {CS3, 1, 2}, {CS3, 1, 1},
      {CS3, 1, 2}, {CS3, 1, 3}, {CS3, 1, 4}, {CS3, 1, 5}, {CS3, 1, 6}, {R, 3, 0},
      {CS3, 1, 6}, {R, 6, 0}, {CS3, 1, 6}, {R, 6, 0}, {CS3, 1, 6}, {CS3, 1, 5},
      {CS3, 1, 4}, {CS3, 1, 3}, {CS3, 1, 2}, {CS3, 1, 1}, {CS3, 1, 2}, {CS3, 1, 3},
      {CS3, 1, 4}, {CS3, 1, 5}, {CS3, 1, 6}, {R, 3, 0}, {CS3, 1, 6}, {R, 6, 0},
      {CS3, 1, 6}, {R, 6, 0}, {B2, 1, 6}, {B2, 1, 5}, {B2, 1, 4}, {B2, 1, 3},
      {B2, 1, 2}, {B2, 1, 1}, {B2, 1, 2}, {B2, 1, 3}, {B2, 1, 4}, {B2, 1, 5},
      {B2, 1, 6}, {R, 3, 0}, {B2, 1, 6}, {B2, 1, 5}, {B2, 1, 4}, {B2, 1, 3},
      {B2, 1, 2}, {B2, 1, 1}, {B2, 1, 2}, {CS3, 1, 6}, {CS3, 1, 5}, {CS3, 1, 4},
      {CS3, 1, 3}, {CS3, 1, 2}, {CS3, 1, 1}, {CS3, 1, 2}, {E3, 1, 6}, {E3, 1, 5},
      {E3, 1, 4}, {E3, 1, 3}, {E3, 1, 2}, {E3, 1, 1}, {E3, 1, 2}, {E3, 1, 3},
      {E3, 1, 4}, {E3, 1, 5}, {E3, 1, 6}, {R, 3, 0}, {E3, 1, 6}, {E3, 1, 5},
      {E3, 1, 4}, {E3, 1, 3}, {E3, 1, 2}, {E3, 1, 1}, {E3, 1, 2}, {DS3, 1, 6},
      {DS3, 1, 5}, {DS3, 1, 4}, {DS3, 1, 3}, {DS3, 1, 2}, {DS3, 1, 1}, {DS3, 1, 2}
    });
    return t;
  }

  private static Track p2B8() {
    Track t = new Track().withDefaults(V(3), D_V00).withDecay(SOSTENUTO);
    t.addSegs(V_STEP, new int[][] {
      {GS4, 2, 1}, {E4, 2, 1}, {B3, 2, 1}, {GS3, 2, 1}, {GS4, 2, 1}, {E4, 2, 1},
      {B3, 2, 1}, {GS3, 2, 1}, {GS4, 2, 1}, {E4, 2, 1}, {B3, 1, 1}, {B3, 1, 2},
      {GS3, 2, 2}, {GS4, 2, 2}, {E4, 2, 2}, {B3, 2, 2}, {GS3, 2, 2}, {GS4, 2, 2},
      {E4, 2, 2}, {B3, 2, 2}, {GS3, 2, 2}, {GS4, 2, 2}, {E4, 2, 3}, {B3, 2, 3},
      {GS3, 2, 3}, {GS4, 2, 3}, {E4, 2, 3}, {B3, 2, 3}, {GS3, 2, 3}, {GS4, 2, 3},
      {E4, 2, 3}, {B3, 2, 3}, {GS3, 1, 3}, {GS3, 1, 4}, {GS4, 2, 4}, {E4, 2, 4},
      {B3, 2, 4}, {GS3, 2, 4}, {GS4, 2, 4}, {E4, 2, 4}, {B3, 2, 4}, {GS3, 2, 4},
      {GS4, 2, 4}, {E4, 2, 4}, {B3, 2, 5}, {GS3, 2, 5}, {GS4, 2, 5}, {E4, 2, 5},
      {B3, 2, 5}, {GS3, 2, 5}, {GS4, 2, 5}, {E4, 2, 5}, {B3, 2, 5}, {GS3, 2, 5},
      {GS4, 1, 5}, {GS4, 1, 6}, {E4, 2, 6}, {B3, 2, 6}, {GS3, 2, 6}
    });
    return t;
  }

  private static Track p2S2829() {
    Track t = new Track().withDefaults(V(1), D_V00).withDecay(SOSTENUTO);
    t.addNotes(R, 13, R, 1);
    t.withVolume(V(2));
    t.addNotes(118, 1, 114, 1, 109, 1, 105, 1, 102, 1, 100, 1, 
        97, 1, CS6, 13, C6, 2, B5, 1, AS5, 1, A5, 1, 
        GS5, 1, G5, 1, FS5, 1, F5, 1, E5, 1, DS5, 1, 
        D5, 1, CS5, 2, C5, 2, B4, 2, AS4, 3, A4, 2, 
        GS4, 3, G4, 3, B5, 13, AS5, 2, A5, 1, GS5, 1, 
        G5, 1, FS5, 1, F5, 1, E5, 1, DS5, 1, D5, 1, 
        CS5, 1, C5, 1, B4, 2, AS4, 3, A4, 2, GS4, 3, 
        G4, 2, FS4, 3, F4, 2, CS6, 13, C6, 2, B5, 1, 
        AS5, 1, A5, 1, GS5, 1, G5, 1, FS5, 1, F5, 1, 
        E5, 1, DS5, 1, D5, 1, CS5, 2, C5, 2, B4, 2, 
        AS4, 3, A4, 2, GS4, 3, G4, 3, DS6, 13, D6, 2, 
        CS6, 1, C6, 1, B5, 1, AS5, 1, A5, 1, GS5, 1, 
        G5, 1, FS5, 1, F5, 1, E5, 1, DS5, 2, D5, 2, 
        CS5, 2, C5, 2, B4, 2, AS4, 2, A4, 3, GS4, 2, 
        E6, 13, DS6, 2, D6, 1, CS6, 1, C6, 1, B5, 1, 
        AS5, 1, A5, 1, GS5, 1, G5, 1, FS5, 1, F5, 1, 
        E5, 2, DS5, 1, DS6, 7);
    return t;
  }

  private static Track p2S3031() {
    Track t = new Track().withDefaults(V(1), D_V00).withDecay(SOSTENUTO);
    t.withVolume(V(2));
    t.addNotes(DS6, 6, D6, 2, CS6, 1, C6, 1, B5, 1, AS5, 1, 
        A5, 1, GS5, 1, G5, 1, FS5, 1, F5, 1, E5, 1, 
        DS5, 2, D5, 1, CS6, 13, C6, 2, B5, 1, AS5, 1, 
        A5, 1, GS5, 1, G5, 1, FS5, 1, F5, 1, E5, 1, 
        DS5, 1, D5, 1, CS5, 2, C5, 2, B4, 2, AS4, 3, 
        A4, 2, GS4, 3, G4, 3, B5, 13, AS5, 2, A5, 1, 
        GS5, 1, G5, 1, FS5, 1, F5, 1, E5, 1, DS5, 1, 
        D5, 1, CS5, 1, C5, 1, B4, 2, AS4, 3, A4, 2, 
        GS4, 3, G4, 2, FS4, 3, F4, 2, CS6, 13, C6, 2, 
        B5, 1, AS5, 1, A5, 1, GS5, 1, G5, 1, FS5, 1, 
        F5, 1, E5, 1, DS5, 1, D5, 1, CS5, 2, C5, 2, 
        B4, 2, AS4, 3, A4, 2, GS4, 3, G4, 3, DS6, 13, 
        D6, 2, CS6, 1, C6, 1, B5, 1, AS5, 1, A5, 1, 
        GS5, 1, G5, 1, FS5, 1, F5, 1, E5, 1, DS5, 2, 
        D5, 2, CS5, 2, C5, 2, B4, 2, AS4, 2, A4, 3, 
        GS4, 2, E6, 13, DS6, 2, D6, 1, CS6, 1, C6, 1, 
        B5, 1, AS5, 1, A5, 1, GS5, 1, G5, 1, FS5, 1, 
        F5, 1, E5, 2, DS5, 1, DS6, 7);
    return t;
  }

  // Bars 32-35: P2's half of the duty dialogue, trailing P1 by 21 frames
  // (three sixteenths) with the same V02/V03/V00 choreography.
  private static Track p2Echo() {
    Track t = new Track().withDefaults(V(3), D_V00).withDecay(SOSTENUTO);
    t.withDuty(D_V00);
    t.withVolume(V(2));
    t.addNotes(DS6, 6, D6, 2, CS6, 1, C6, 1, B5, 1, AS5, 1, 
        A5, 1, GS5, 1, G5, 1, FS5, 1, F5, 1, E5, 1, 
        DS5, 2, D5, 1);
    t.withDuty(D_V02);
    t.withVolume(V(2));
    t.addNotes(CS6, 13, C6, 2, B5, 1, AS5, 1, A5, 1, GS5, 1, 
        G5, 1, FS5, 1, F5, 1, E5, 1, DS5, 1, D5, 1, 
        CS5, 2, C5, 2, B4, 2, AS4, 3, A4, 2, GS4, 3, 
        G4, 3, B5, 13, AS5, 2, A5, 1, GS5, 1, G5, 1, 
        FS5, 1, F5, 1, E5, 1, DS5, 1, D5, 1, CS5, 1, 
        C5, 1, B4, 2, AS4, 3, A4, 2, GS4, 3, G4, 2, 
        FS4, 3, F4, 2);
    t.withDuty(D_V03);
    t.withVolume(V(2));
    t.addNotes(CS6, 13, C6, 2, B5, 1, AS5, 1, A5, 1, GS5, 1, 
        G5, 1, FS5, 1, F5, 1, E5, 1, DS5, 1, D5, 1, 
        CS5, 2, C5, 2, B4, 2, AS4, 3, A4, 2, GS4, 3, 
        G4, 3, DS6, 13, D6, 2, CS6, 1, C6, 1, B5, 1, 
        AS5, 1, A5, 1, GS5, 1, G5, 1, FS5, 1, F5, 1, 
        E5, 1, DS5, 2, D5, 2, CS5, 2, C5, 2, B4, 2, 
        AS4, 2, A4, 3, GS4, 2);
    t.withDuty(D_V00);
    t.withVolume(V(2));
    t.addNotes(E6, 13, DS6, 2, D6, 1, CS6, 1, C6, 1, B5, 1, 
        AS5, 1, A5, 1, GS5, 1, G5, 1, FS5, 1, F5, 1, 
        E5, 2, DS5, 1);
    t.withDuty(D_V03);
    t.withVolume(V(2));
    t.addNotes(DS6, 13, D6, 2, CS6, 1, C6, 1, B5, 1, AS5, 1, 
        A5, 1, GS5, 1, G5, 1, FS5, 1, F5, 1, E5, 1, 
        DS5, 2, D5, 1);
    t.withDuty(D_V02);
    t.withVolume(V(2));
    t.addNotes(CS6, 13, C6, 2, B5, 1, AS5, 1, A5, 1, GS5, 1, 
        G5, 1, FS5, 1, F5, 1, E5, 1, DS5, 1, D5, 1, 
        CS5, 2, C5, 2, B4, 2, AS4, 3, A4, 2, GS4, 3, 
        G4, 3, B5, 13, AS5, 2, A5, 1, GS5, 1, G5, 1, 
        FS5, 1, F5, 1, E5, 1, DS5, 1, D5, 1, CS5, 1, 
        C5, 1, B4, 2, AS4, 3, A4, 2, GS4, 3, G4, 2, 
        FS4, 3, F4, 2);
    t.withDuty(D_V03);
    t.withVolume(V(2));
    t.addNotes(CS6, 13, C6, 2, B5, 1, AS5, 1, A5, 1, GS5, 1, 
        G5, 1, FS5, 1, F5, 1, E5, 1, DS5, 1, D5, 1, 
        CS5, 2, C5, 2, B4, 2, AS4, 3, A4, 2, GS4, 3, 
        G4, 3, DS6, 13, D6, 2, CS6, 1, C6, 1, B5, 1, 
        AS5, 1, A5, 1, GS5, 1, G5, 1, FS5, 1, F5, 1, 
        E5, 1, DS5, 2, D5, 2, CS5, 2, C5, 2, B4, 2, 
        AS4, 2, A4, 3, GS4, 2);
    t.withDuty(D_V00);
    t.withVolume(V(2));
    t.addNotes(E6, 13, DS6, 2, D6, 1, CS6, 1, C6, 1, B5, 1, 
        AS5, 1, A5, 1, GS5, 1, G5, 1, FS5, 1, F5, 1, 
        E5, 2, DS5, 1);
    t.withDuty(D_V03);
    t.withVolume(V(2));
    t.addNotes(DS6, 13, D6, 2, CS6, 1, C6, 1, B5, 1, AS5, 1, 
        A5, 1, GS5, 1, G5, 1, FS5, 1, F5, 1, E5, 1, 
        DS5, 2, D5, 1);
    return t;
  }

  private static Track p2Comedown() {
    Track t = new Track().withDefaults(V(3), D_V02).withDecay(SOSTENUTO);
    t.addSegs(V_STEP, new int[][] {
      {CS3, 5, 2}, {CS3, 1, 1}, {R, 1, 0}, {B2, 5, 2}, {B2, 1, 1}, {R, 1, 0},
      {CS3, 5, 2}, {CS3, 1, 1}, {R, 1, 0}, {E3, 5, 2}, {E3, 1, 1}, {R, 1, 0},
      {FS3, 5, 2}, {FS3, 1, 1}, {R, 1, 0}, {GS3, 5, 2}, {GS3, 1, 1}, {R, 1, 0},
      {B3, 5, 2}, {B3, 1, 1}, {R, 1, 0}, {CS4, 5, 2}, {CS4, 1, 1}, {R, 1, 0},
      {E4, 5, 2}, {E4, 1, 1}, {R, 1, 0}, {B3, 5, 2}, {B3, 1, 1}, {R, 1, 0},
      {AS3, 5, 2}, {AS3, 1, 1}, {R, 1, 0}, {CS4, 5, 2}, {CS4, 1, 1}, {R, 1, 0},
      {GS3, 5, 2}, {GS3, 1, 1}, {R, 1, 0}, {E3, 5, 2}, {E3, 1, 1}, {R, 1, 0},
      {DS3, 5, 2}, {DS3, 1, 1}, {R, 1, 0}, {FS3, 5, 2}, {FS3, 1, 1}, {R, 1, 0},
      {CS3, 5, 2}, {CS3, 1, 1}, {R, 1, 0}, {B2, 5, 2}, {B2, 1, 1}, {R, 1, 0},
      {CS3, 5, 2}, {CS3, 1, 1}, {R, 1, 0}, {E3, 5, 2}, {E3, 1, 1}, {R, 1, 0},
      {FS3, 5, 2}, {FS3, 1, 1}, {R, 1, 0}, {GS3, 5, 2}, {GS3, 1, 1}, {R, 1, 0},
      {B3, 5, 2}, {B3, 1, 1}, {R, 1, 0}, {CS4, 5, 2}, {CS4, 1, 1}, {R, 1, 0},
      {E4, 5, 2}, {E4, 1, 1}, {R, 1, 0}, {B3, 5, 2}, {B3, 1, 1}, {R, 1, 0},
      {AS3, 5, 2}, {AS3, 1, 1}, {R, 1, 0}, {CS4, 5, 2}, {CS4, 1, 1}, {R, 1, 0},
      {GS3, 5, 2}, {GS3, 1, 1}, {R, 1, 0}
    });
    return t;
  }

  private static Track p2S3839() {
    Track t = new Track().withDefaults(V(3), D_V02).withDecay(SOSTENUTO);
    t.addSegs(V_STEP, new int[][] {
      {E3, 5, 2}, {E3, 1, 1}, {R, 1, 0}, {DS3, 5, 2}, {DS3, 1, 1}, {R, 1, 0},
      {FS3, 5, 2}, {FS3, 1, 1}, {R, 1, 0}, {CS3, 1, 2}, {GS4, 1, 2}, {CS3, 3, 2},
      {CS3, 1, 1}, {R, 1, 0}, {B2, 1, 2}, {FS4, 1, 2}, {B2, 3, 2}, {B2, 1, 1},
      {R, 1, 0}, {CS3, 1, 2}, {GS4, 1, 2}, {CS3, 3, 2}, {CS3, 1, 1}, {R, 1, 0},
      {E3, 1, 2}, {B4, 1, 2}, {E3, 3, 2}, {E3, 1, 1}, {R, 1, 0}, {FS3, 1, 2},
      {CS5, 1, 2}, {FS3, 3, 2}, {FS3, 1, 1}, {R, 1, 0}, {GS3, 1, 2}, {DS5, 1, 2},
      {GS3, 3, 2}, {GS3, 1, 1}, {R, 1, 0}, {B3, 1, 2}, {FS5, 1, 2}, {B3, 3, 2},
      {B3, 1, 1}, {R, 1, 0}, {CS4, 1, 2}, {GS5, 1, 2}, {CS4, 3, 2}, {CS4, 1, 1},
      {R, 1, 0}, {E4, 1, 2}, {B5, 1, 2}, {E4, 3, 2}, {E4, 1, 1}, {R, 1, 0},
      {B3, 1, 2}, {FS5, 1, 2}, {B3, 3, 2}, {B3, 1, 1}, {R, 1, 0}, {AS3, 1, 2},
      {F5, 1, 2}, {AS3, 3, 2}, {AS3, 1, 1}, {R, 1, 0}, {CS4, 1, 2}, {GS5, 1, 2},
      {CS4, 3, 2}, {CS4, 1, 1}, {R, 1, 0}, {GS3, 1, 2}, {DS5, 1, 2}, {GS3, 3, 2},
      {GS3, 1, 1}, {R, 1, 0}, {E3, 1, 2}, {B4, 1, 2}, {E3, 3, 2}, {E3, 1, 1},
      {R, 1, 0}, {DS3, 1, 2}, {AS4, 1, 2}, {DS3, 3, 2}, {DS3, 1, 1}, {R, 1, 0},
      {FS3, 1, 2}, {CS5, 1, 2}, {FS3, 3, 2}, {FS3, 1, 1}, {R, 1, 0}, {CS3, 1, 2},
      {GS4, 1, 2}, {CS3, 3, 2}, {CS3, 1, 1}, {R, 1, 0}, {B2, 1, 2}, {FS4, 1, 2},
      {B2, 3, 2}, {B2, 1, 1}, {R, 1, 0}, {CS3, 1, 2}, {GS4, 1, 2}, {CS3, 3, 2},
      {CS3, 1, 1}, {R, 1, 0}, {E3, 1, 2}, {B4, 1, 2}, {E3, 3, 2}, {E3, 1, 1},
      {R, 1, 0}, {FS3, 1, 2}, {CS5, 1, 2}, {FS3, 3, 2}, {FS3, 1, 1}, {R, 1, 0},
      {GS3, 1, 2}, {DS5, 1, 2}, {GS3, 3, 2}, {GS3, 1, 1}, {R, 1, 0}, {B3, 1, 2},
      {FS5, 1, 2}, {B3, 3, 2}, {B3, 1, 1}, {R, 1, 0}, {CS4, 1, 2}, {GS5, 1, 2},
      {CS4, 3, 2}, {CS4, 1, 1}, {R, 1, 0}, {E4, 1, 2}, {B5, 1, 2}, {E4, 3, 2},
      {E4, 1, 1}, {R, 1, 0}, {B3, 1, 2}, {FS5, 1, 2}, {B3, 3, 2}, {B3, 1, 1},
      {R, 1, 0}, {AS3, 1, 2}, {F5, 1, 2}, {AS3, 3, 2}, {AS3, 1, 1}, {R, 1, 0},
      {CS4, 1, 2}, {GS5, 1, 2}, {CS4, 3, 2}, {CS4, 1, 1}, {R, 1, 0}, {GS3, 1, 2},
      {DS5, 1, 2}, {GS3, 3, 2}, {GS3, 1, 1}, {R, 1, 0}
    });
    return t;
  }

  private static Track p2V() {
    Track t = new Track().withDefaults(V(3), D_V02).withDecay(SOSTENUTO);
    t.withVolume(V(2));
    t.addNotes(GS4, 7, FS4, 7, GS4, 7, B4, 7, CS5, 7, DS5, 7, 
        FS5, 7, GS5, 7, B5, 7, FS5, 7, F5, 7, GS5, 7, 
        DS5, 7, B4, 7, AS4, 7, CS5, 7);
    return t;
  }

  private static Track p2W() {
    Track t = new Track().withDefaults(V(3), D_V02).withDecay(SOSTENUTO);
    t.addSegs(V_STEP, new int[][] {
      {CS5, 1, 2}, {CS6, 1, 2}, {CS5, 1, 2}, {CS6, 1, 2}, {CS5, 1, 2}, {CS6, 1, 1},
      {R, 1, 0}, {B4, 1, 2}, {B5, 1, 2}, {B4, 1, 2}, {B5, 1, 2}, {B4, 1, 2},
      {B5, 1, 1}, {R, 1, 0}, {CS5, 1, 2}, {CS6, 1, 2}, {CS5, 1, 2}, {CS6, 1, 2},
      {CS5, 1, 2}, {CS6, 1, 1}, {R, 1, 0}, {E5, 1, 2}, {E6, 1, 2}, {E5, 1, 2},
      {E6, 1, 2}, {E5, 1, 2}, {E6, 1, 1}, {R, 1, 0}, {FS5, 1, 2}, {FS6, 1, 2},
      {FS5, 1, 2}, {FS6, 1, 2}, {FS5, 1, 2}, {FS6, 1, 1}, {R, 1, 0}, {GS5, 1, 2},
      {GS6, 1, 2}, {GS5, 1, 2}, {GS6, 1, 2}, {GS5, 1, 2}, {GS6, 1, 1}, {R, 1, 0},
      {B5, 1, 2}, {B6, 1, 2}, {B5, 1, 2}, {B6, 1, 2}, {B5, 1, 2}, {B6, 1, 1},
      {R, 1, 0}, {CS6, 1, 2}, {97, 1, 2}, {CS6, 1, 2}, {97, 1, 2}, {CS6, 1, 2},
      {97, 1, 1}, {R, 1, 0}, {E6, 1, 2}, {100, 1, 2}, {E6, 1, 2}, {100, 1, 2},
      {E6, 1, 2}, {100, 1, 1}, {R, 1, 0}, {B5, 1, 2}, {B6, 1, 2}, {B5, 1, 2},
      {B6, 1, 2}, {B5, 1, 2}, {B6, 1, 1}, {R, 1, 0}, {AS5, 1, 2}, {AS6, 1, 2},
      {AS5, 1, 2}, {AS6, 1, 2}, {AS5, 1, 2}, {AS6, 1, 1}, {R, 1, 0}, {CS6, 1, 2},
      {97, 1, 2}, {CS6, 1, 2}, {97, 1, 2}, {CS6, 1, 2}, {97, 1, 1}, {R, 1, 0},
      {GS5, 1, 2}, {GS6, 1, 2}, {GS5, 1, 2}, {GS6, 1, 2}, {GS5, 1, 2}, {GS6, 1, 1},
      {R, 1, 0}, {E5, 1, 2}, {E6, 1, 2}, {E5, 1, 2}, {E6, 1, 2}, {E5, 1, 2},
      {E6, 1, 1}, {R, 1, 0}, {DS5, 1, 2}, {DS6, 1, 2}, {DS5, 1, 2}, {DS6, 1, 2},
      {DS5, 1, 2}, {DS6, 1, 1}, {R, 1, 0}, {FS5, 1, 2}, {FS6, 1, 2}, {FS5, 1, 2},
      {FS6, 1, 2}, {FS5, 1, 2}, {FS6, 1, 1}, {R, 1, 0}
    });
    return t;
  }

  private static Track p2X() {
    Track t = new Track().withDefaults(V(3), D_V02).withDecay(SOSTENUTO);
    t.addSegs(V_STEP, new int[][] {
      {GS4, 1, 2}, {GS5, 1, 2}, {GS4, 1, 2}, {GS5, 1, 2}, {GS4, 1, 2}, {GS5, 1, 1},
      {R, 1, 0}, {FS4, 1, 2}, {FS5, 1, 2}, {FS4, 1, 2}, {FS5, 1, 2}, {FS4, 1, 2},
      {FS5, 1, 1}, {R, 1, 0}, {GS4, 1, 2}, {GS5, 1, 2}, {GS4, 1, 2}, {GS5, 1, 2},
      {GS4, 1, 2}, {GS5, 1, 1}, {R, 1, 0}, {B4, 1, 2}, {B5, 1, 2}, {B4, 1, 2},
      {B5, 1, 2}, {B4, 1, 2}, {B5, 1, 1}, {R, 1, 0}, {CS5, 1, 2}, {CS6, 1, 2},
      {CS5, 1, 2}, {CS6, 1, 2}, {CS5, 1, 2}, {CS6, 1, 1}, {R, 1, 0}, {DS5, 1, 2},
      {DS6, 1, 2}, {DS5, 1, 2}, {DS6, 1, 2}, {DS5, 1, 2}, {DS6, 1, 1}, {R, 1, 0},
      {FS5, 1, 2}, {FS6, 1, 2}, {FS5, 1, 2}, {FS6, 1, 2}, {FS5, 1, 2}, {FS6, 1, 1},
      {R, 1, 0}, {GS5, 1, 2}, {GS6, 1, 2}, {GS5, 1, 2}, {GS6, 1, 2}, {GS5, 1, 2},
      {GS6, 1, 1}, {R, 1, 0}, {B5, 1, 2}, {B6, 1, 2}, {B5, 1, 2}, {B6, 1, 2},
      {B5, 1, 2}, {B6, 1, 1}, {R, 1, 0}, {FS5, 1, 2}, {FS6, 1, 2}, {FS5, 1, 2},
      {FS6, 1, 2}, {FS5, 1, 2}, {FS6, 1, 1}, {R, 1, 0}, {F5, 1, 2}, {F6, 1, 2},
      {F5, 1, 2}, {F6, 1, 2}, {F5, 1, 2}, {F6, 1, 1}, {R, 1, 0}, {GS5, 1, 2},
      {GS6, 1, 2}, {GS5, 1, 2}, {GS6, 1, 2}, {GS5, 1, 2}, {GS6, 1, 1}, {R, 1, 0},
      {DS5, 1, 2}, {DS6, 1, 2}, {DS5, 1, 2}, {DS6, 1, 2}, {DS5, 1, 2}, {DS6, 1, 1},
      {R, 1, 0}, {B4, 1, 2}, {B5, 1, 2}, {B4, 1, 2}, {B5, 1, 2}, {B4, 1, 2},
      {B5, 1, 1}, {R, 1, 0}, {AS4, 1, 2}, {AS5, 1, 2}, {AS4, 1, 2}, {AS5, 1, 2},
      {AS4, 1, 2}, {AS5, 1, 1}, {R, 1, 0}, {CS5, 1, 2}, {CS6, 1, 2}, {CS5, 1, 2},
      {CS6, 1, 2}, {CS5, 1, 2}, {CS6, 1, 1}, {R, 1, 0}
    });
    return t;
  }

  private static Track p2Y() {
    Track t = new Track().withDefaults(V(3), D_V02).withDecay(SOSTENUTO);
    t.withVolume(V(2));
    t.addNotes(GS4, 2, FS4, 1, E4, 1, DS4, 1, D4, 1, C4, 1, 
        FS4, 2, E4, 1, DS4, 1, CS4, 1, C4, 1, B3, 1, 
        GS4, 2, FS4, 1, E4, 1, DS4, 1, D4, 1, C4, 1, 
        B4, 2, A4, 1, G4, 1, F4, 1, E4, 1, D4, 1, 
        CS5, 2, B4, 1, GS4, 1, G4, 1, F4, 1, DS4, 1, 
        DS5, 2, C5, 1, AS4, 1, GS4, 1, FS4, 1, E4, 1, 
        FS5, 2, DS5, 1, C5, 1, AS4, 1, GS4, 1, FS4, 1, 
        GS5, 2, E5, 1, D5, 1, B4, 1, A4, 1, G4, 1, 
        B5, 2, G5, 1, E5, 1, CS5, 1, AS4, 1, GS4, 1, 
        FS5, 2, DS5, 1, C5, 1, AS4, 1, GS4, 1, FS4, 1, 
        F5, 2, D5, 1, B4, 1, A4, 1, G4, 1, F4, 1, 
        GS5, 2, E5, 1, D5, 1, B4, 1, A4, 1, G4, 1, 
        DS5, 2, C5, 1, AS4, 1, GS4, 1, FS4, 1, E4, 1, 
        B4, 2, A4, 1, G4, 1, F4, 1, E4, 1, D4, 1, 
        AS4, 2, GS4, 1, FS4, 1, E4, 1, DS4, 1, D4, 1, 
        CS5, 2, B4, 1, GS4, 1, G4, 1, F4, 1, DS4, 1);
    return t;
  }

  /* ==================== BASS (triangle) ==================== */
  // NSFImport names triangle periods with the pulse table, so the triangle
  // transfers at label+0 (the Flash Man proof)  these pitches are the
  // capture's labels verbatim. The 5-frame dives ARE the kick drum; the
  // bass notes live between them. One channel, two instruments.

  @Override
  public Track getBass() {
    Track t = new Track().withDefaults(BASS_VOL, 0.5);
    t.addNotes(R, W);                         // head: triangle waits
    t.addNotes(trIntroA());                   // bars 0-3: kick + air
    t.addNotes(trIntroB());
    t.addNotes(trIntroA());
    t.addNotes(trIntroB());
    for (int i = 0; i < 18; i++) {
      t.addNotes(trGrooveC());                // bars 4-39: kick + bassline
      t.addNotes(trGrooveD());
    }
    for (int i = 0; i < 4; i++) {
      t.addNotes(trCloseE());                 // bars 40-43
    }
    for (int i = 0; i < 4; i++) {
      t.addNotes(trCloseF());                 // bars 44-47
    }
    return t;
  }

  private static Track trIntroA() {
    Track t = new Track().withDefaults(BASS_VOL, 0.5);
    t.addNotes(AS2, 1, E2, 1, C2, 1, A1, 1, GS1, 1, R, 9, 
        DS3, 1, R, 13, GS3, 1, E2, 1, C2, 1, A1, 1, 
        GS1, 1, R, 16, DS3, 1, R, 6, AS2, 1, E2, 1, 
        C2, 1, A1, 1, GS1, 1, R, 2, DS3, 1, R, 13, 
        DS3, 1, R, 6, GS3, 1, E2, 1, C2, 1, A1, 1, 
        GS1, 1, R, 9, DS3, 1, R, 6, GS3, 1, R, 6);
    return t;
  }

  private static Track trIntroB() {
    Track t = new Track().withDefaults(BASS_VOL, 0.5);
    t.addNotes(AS2, 1, E2, 1, C2, 1, A1, 1, GS1, 1, R, 2, 
        A3, 1, R, 6, DS3, 1, R, 13, GS3, 1, E2, 1, 
        C2, 1, A1, 1, GS1, 1, R, 16, GS3, 1, R, 6, 
        AS2, 1, E2, 1, C2, 1, A1, 1, GS1, 1, R, 9, 
        DS3, 1, R, 13, GS3, 1, E2, 1, C2, 1, A1, 1, 
        GS1, 1, R, 9, GS3, 1, R, 6, GS3, 1, R, 6);
    return t;
  }

  private static Track trGrooveC() {
    Track t = new Track().withDefaults(BASS_VOL, 0.5);
    t.addNotes(AS2, 1, E2, 1, C2, 1, A1, 1, GS1, 1, CS2, 9, 
        DS3, 1, R, 13, GS3, 1, E2, 1, C2, 1, A1, 1, 
        GS1, 1, CS2, 9, R, 7, DS3, 1, R, 6, AS2, 1, 
        E2, 1, C2, 1, A1, 1, GS1, 1, B1, 2, DS3, 1, 
        CS2, 13, DS3, 1, B1, 6, GS3, 1, E2, 1, C2, 1, 
        A1, 1, GS1, 1, E2, 9, DS3, 1, E2, 6, GS3, 1, 
        FS2, 6);
    return t;
  }

  private static Track trGrooveD() {
    Track t = new Track().withDefaults(BASS_VOL, 0.5);
    t.addNotes(AS2, 1, E2, 1, C2, 1, A1, 1, GS1, 1, CS2, 2, 
        A3, 1, CS2, 6, DS3, 1, R, 13, GS3, 1, E2, 1, 
        C2, 1, A1, 1, GS1, 1, CS2, 9, R, 7, GS3, 1, 
        R, 6, AS2, 1, E2, 1, C2, 1, A1, 1, GS1, 1, 
        B1, 9, DS3, 1, B1, 6, CS2, 7, GS3, 1, E2, 1, 
        C2, 1, A1, 1, GS1, 1, E2, 9, GS3, 1, E2, 6, 
        GS3, 1, DS2, 6);
    return t;
  }

  private static Track trCloseE() {
    Track t = new Track().withDefaults(BASS_VOL, 0.5);
    t.addNotes(AS2, 1, E2, 1, C2, 1, A1, 1, GS1, 1, CS2, 9, 
        R, 14, GS3, 1, E2, 1, C2, 1, A1, 1, GS1, 1, 
        CS2, 9, R, 14, AS2, 1, E2, 1, C2, 1, A1, 1, 
        GS1, 1, CS2, 9, R, 14, GS3, 1, E2, 1, C2, 1, 
        A1, 1, GS1, 1, CS2, 9, R, 14);
    return t;
  }

  private static Track trCloseF() {
    Track t = new Track().withDefaults(BASS_VOL, 0.5);
    t.addNotes(AS2, 1, E2, 1, C2, 1, A1, 1, GS1, 1, CS2, 9, 
        R, 14, GS3, 1, E2, 1, C2, 1, A1, 1, GS1, 1, 
        CS2, 9, R, 7, GS3, 1, R, 6, AS2, 1, E2, 1, 
        C2, 1, A1, 1, GS1, 1, CS2, 9, R, 14, GS3, 1, 
        E2, 1, C2, 1, A1, 1, GS1, 1, CS2, 9, R, 7, 
        GS3, 1, R, 6);
    return t;
  }

  /* ==================== DRUMS (noise) ==================== */
  // Kit from the capture's noise periods and velocity layers: a 1-frame
  // period-9 tick on each beat (it lands ON the triangle dive  together
  // they are the kick), C/D-period "chik" pairs on the off-16ths, and an
  // A/9 flam snare on beats 2 and 4. Painted decay tails ride our voices'
  // own envelopes. {slot, voice, volumeStep, frames} on the 112 grid.

  private static final int[][] KIT_A = {
    {0, HIHAT, 5, 1}, {7, HIHAT, 4, 5}, {14, HIHAT, 4, 12},
    {28, SNARE, 5, 11}, {42, HIHAT, 4, 12}, {56, HIHAT, 5, 1},
    {63, HIHAT, 4, 5}, {70, HIHAT, 4, 12}, {84, SNARE, 5, 11},
    {98, HIHAT, 4, 7}, {105, SNARE, 3, 7}
  };

  private static final int[][] KIT_B = {
    {0, HIHAT, 5, 1}, {7, SNARE, 3, 7}, {14, HIHAT, 4, 12},
    {28, SNARE, 5, 11}, {42, HIHAT, 4, 7}, {49, SNARE, 3, 7},
    {56, HIHAT, 5, 1}, {63, HIHAT, 4, 5}, {70, SNARE, 3, 10},
    {84, SNARE, 3, 10}, {98, SNARE, 3, 7}, {105, SNARE, 3, 7}
  };

  private static final int[][] KIT_D = {
    {0, HIHAT, 5, 1}, {28, SNARE, 3, 10},
    {56, HIHAT, 5, 1}, {84, SNARE, 3, 10}
  };

  private static final int[][] KIT_E = {
    {0, HIHAT, 5, 3}, {14, HIHAT, 4, 12}, {28, SNARE, 3, 10},
    {42, HIHAT, 4, 7}, {49, SNARE, 3, 7}, {56, HIHAT, 5, 3},
    {70, HIHAT, 4, 12}, {84, SNARE, 3, 10}, {98, HIHAT, 4, 7},
    {105, SNARE, 3, 7}
  };

  @Override
  public Track getDrums() {
    Track t = new Track().withDefaults(V(4), 0.5);
    t.addNotes(kitHead());
    t.addNotes(kitBar(KIT_A));                // bar 0
    for (int b = 1; b <= 39; b++) {
      t.addNotes(kitBar((b % 2 == 1) ? KIT_B : KIT_A));
    }
    for (int i = 0; i < 4; i++) {
      t.addNotes(kitBar(KIT_D));              // bars 40-43, half-time
    }
    for (int i = 0; i < 4; i++) {
      t.addNotes(kitBar(KIT_E));              // bars 44-47
    }
    return t;
  }

  // Head: a 15-step noise-period waterfall falling with the pulses. The
  // capture holds v4 flat the whole way down  this is ONE continuous
  // noise roar stepping in pitch, not fifteen drum hits, so it plays as
  // sustained pitched noise (withDecay(SUSTAINED) routes off-selector
  // pitches to the raw LFSR instead of the percussive tom voice).
  private static Track kitHead() {
    Track t = new Track().withDefaults(V(4), 0.5);
    t.withDecay(SOSTENUTO);
    for (int p = 14; p >= 0; p--) {
      t.addNotes(37 + p * 4, 7);
    }
    t.withDecay(LEGATO);
    t.addNotes(R, 7);
    return t;
  }

  private static Track kitBar(int[][] hits) {
    Track t = new Track().withDefaults(V(4), 0.5);
    int at = 0;
    for (int[] h : hits) {
      if (h[0] > at) {
        t.addNotes(R, h[0] - at);
      }
      t.withVolume(V(h[2]));
      t.addNotes(h[1], h[3]);
      at = h[0] + h[3];
    }
    if (at < W) {
      t.addNotes(R, W - at);
    }
    return t;
  }
}
