package chiptunesynth.songs;

import chiptunesynth.ChiptuneSong;
import chiptunesynth.Track;

/**
 * Mega Man 2 - Flash Man stage, by Takashi Tateishi (Ogeretsu).
 *
 * Rebuilt from flashman.txt (NSFImport capture, 1 row = 1 frame, cartridge
 * NSF). The old MIDI rip had the right notes and the wrong engineering;
 * this file is the capture's engineering, bar for bar:
 *
 * FORM - a 16-bar HEAD that plays once, then a 16-bar LOOP forever, both
 * native to the house grid (96-frame bars, 150 BPM, tempoScale 1.0). The
 * first song to use getLoopStartFrames(): the synth re-enters at bar 16.
 *
 *   HEAD   [run 2 + answer 2] x3 + run 2 + push 1 + roll 1. The unison
 *          run is stabbed (5+7 eighths, both squares at 50% duty, the
 *          triangle on the same octave); pulse 2 is a true echo - the run
 *          repeated one eighth late at half volume, 12.5% duty
 *          (Track.echoOf, the house Wise-echo, straight from the data).
 *          The answers are parallel thirds, same rhythm, no echo.
 *   LOOP   verse 8 (lead thins to 25% duty and goes near-legato 11+1;
 *          pulse 2 chugs the 16th inner voice at 12.5%; the bass plays
 *          the real funk cell - root eighths with a beat-2 hole and a
 *          G-B-G-E 16th climb), bridge 2 (lead FATTENS to 75% and
 *          FLUTTERS - the D/C#/C trills the MIDI flattened into whole
 *          notes; pulse 2 holds B/Bb/A thirds under it; bass walks
 *          G-F#-F straight eighths), walk 2 (50% duty G-E answer +
 *          Bb-B chromatic wiggle; the triangle DIVES - per-frame gliss
 *          cascades where the MIDI had a dead stop), then verse-tail 2 +
 *          bridge/walk again + climb 1 (E5 up to B5 into the wrap).
 *
 * DUTY ARC (the sound of this song, all from Vxx writes): head 50%,
 * verse 25%, bridge 75%, walks/climb 50%; pulse 2 lives at 12.5% except
 * the bridge's 75%. Volumes: lead vD (13/15), pulse 2 at half the lead
 * in the verse (v7), vA under the bridge. No vibrato anywhere - the
 * capture's pitch column is static per-note detune, and the motion the
 * old file faked with vibrato is really the flutters and dives.
 *
 * KIT (two noise voices in the capture, no DPCM): head = kick-sweep on
 * every beat with a soft tick on the "e", into a 13-hit roll; verse =
 * tick, snare 2, four-16th ride run-up, snare 4; bridge halves the
 * pattern; climb bar decays on fading quarter ticks.
 *
 * OCTAVES - pulses transfer from the capture at label+12, the triangle
 * at label+0 (NSFImport names triangle periods with the pulse table; the
 * verse bass at label+12 would sit in unison with the melody, which
 * settles it). The old rip's E2 bass was an octave too deep.
 *
 * @author dylan
 */
public class FlashmanSong implements ChiptuneSong {

  @Override
  public int getLoopStartFrames() {
    return HEAD_BARS * W;                // the head plays once
  }

  private static final int HEAD_BARS = 16;

  /* === VOICES / MIX (v/15 against the capture's volume column) === */
  private static final double LEAD_V   = 0.85;   // vD
  private static final double P2_RUN_V = 0.54;   // v8, as echoOf scale
  private static final double P2_VER_V = 0.45;   // v7
  private static final double P2_BRI_V = 0.64;   // vA
  private static final double BASS_V   = 1.0;    // triangle has no envelope

  // the duty arc
  private static final double RUN_DUTY    = 0.50;    // V02
  private static final double VERSE_DUTY  = 0.25;    // V01
  private static final double BRIDGE_DUTY = 0.75;    // V03
  private static final double THIN_DUTY   = 0.125;   // V00, pulse 2's home

  private static final double HAT_V   = 0.50;    // vE tick
  private static final double HAT_SOFT = 0.25;   // v6 tick after the kick
  private static final double SNARE_V = 0.85;
  private static final double ROLL_V  = 0.60;    // v9 fill hits

  /* === ARTICULATION (all measured) ===
   * The head stabs its eighths (5 tone + 7 silent); the verse sings them
   * (11 + 1). Sixteenths are 5 + 1 everywhere. Quarters ring 23 + 1. */
  private static void s16(Track t, int p) {
    t.addNotes(p, 5, R, 1);
  }

  private static void e8(Track t, int p) {   // stabbed eighth
    t.addNotes(p, 5, R, 7);
  }

  private static void el(Track t, int p) {   // legato eighth
    t.addNotes(p, 11, R, 1);
  }

  private static void q(Track t, int p) {    // quarter
    t.addNotes(p, 23, R, 1);
  }

  private static void de(Track t, int p) {   // dotted eighth, rings
    t.addNotes(p, 17, R, 1);
  }

  /* ==================== THE RUN (2 bars) ==================== */

  // the E-minor unison run every voice states; the pulses play it at vol,
  // the triangle reuses it note-for-note an octave under the old rip
  private static Track runBars(double vol, double duty) {
    Track t = new Track().withDefaults(vol, duty);
    q(t, E3);
    e8(t, G3);
    e8(t, B3);
    de(t, D4);
    de(t, CS4);
    el(t, A3);
    de(t, C4);
    de(t, B3);
    el(t, G3);
    e8(t, A3);
    e8(t, G3);
    s16(t, E3);
    e8(t, D3);
    s16(t, E3);
    return t;
  }

  /* ==================== LEAD ==================== */

  // answer bar A: the F#-F#-F#-G call twice  one eighth of breath
  // between the calls, the long rest saved for the bar's tail
  private static void ansBarA(Track t, int lo, int hi) {
    t.addNotes(R, E);
    s16(t, lo);
    s16(t, lo);
    s16(t, lo);
    s16(t, hi);
    t.addNotes(R, E);
    s16(t, lo);
    s16(t, lo);
    s16(t, lo);
    s16(t, hi);
    t.addNotes(R, Q);
  }

  // answer bar B: one call, then the B-B-A-G-F# comedown
  private static void ansBarB(Track t, int lo, int hi, int top) {
    t.addNotes(R, E);
    s16(t, lo);
    s16(t, lo);
    s16(t, lo);
    s16(t, hi);
    t.addNotes(R, S);
    e8(t, top);
    s16(t, top);
    e8(t, top - 2);
    e8(t, hi);
    el(t, lo);                           // the F# rings out, no extra rest
  }

  // head bar 15: the A pushes; bar 16: handled by the caller (the push's
  // B rings out over the drum roll)
  private static void pushBar(Track t, int push, int target) {
    s16(t, push);
    e8(t, push);
    e8(t, push);
    e8(t, push);
    s16(t, push);
    t.addNotes(target, 23, R, 25);
  }

  private static Track leadHead() {
    Track t = new Track().withDefaults(LEAD_V, RUN_DUTY).withDecay(0.35);
    for (int i = 0; i < 3; ++i) {
      t.addNotes(runBars(LEAD_V, RUN_DUTY));
      ansBarA(t, FS4, G4);
      ansBarB(t, FS4, G4, B4);
    }
    t.addNotes(runBars(LEAD_V, RUN_DUTY));
    ansBarA(t, FS4, G4);
    pushBar(t, A4, B4);
    return t;
  }

  // verse phrase, 4 bars; `tail` flips the second half (bars 3-4 differ
  // between the two verse statements)
  private static void versePhrase(Track t, boolean first) {
    t.addNotes(R, E);                    // the melody breathes in late
    el(t, E4);
    el(t, B4);
    el(t, A4);
    q(t, B4);
    el(t, A4);
    el(t, G4);
    el(t, A4);                           // bar 2
    el(t, B4);
    t.addNotes(R, E);
    q(t, E4);
    q(t, E4);
    el(t, G4);
    if (first) {                         // bar 3: F# F# F# E D E
      el(t, FS4);
      t.addNotes(R, E);
      el(t, FS4);
      t.addNotes(R, E);
      el(t, FS4);
      el(t, E4);
      el(t, D4);
      el(t, E4);
      t.addNotes(E4, 47, R, 13);         // bar 4: the long E breathes out
      el(t, E4);
      el(t, G4);
      el(t, A4);
    } else {                             // bar 3: F# F# G A F# E
      el(t, FS4);
      t.addNotes(R, E);
      el(t, FS4);
      t.addNotes(R, E);
      el(t, G4);
      el(t, A4);
      el(t, FS4);
      el(t, E4);
      q(t, E4);                          // bar 4: E E . E G B
      q(t, E4);
      t.addNotes(R, E);
      el(t, E4);
      el(t, G4);
      el(t, B4);
    }
  }

  // one bridge bar: half a bar rung plain, half fluttered with the lower
  // neighbor - the trills the capture shows at 2-frame flicks
  private static void flutterBar(Track t, int main, int below) {
    t.addNotes(main, 47, R, 1);
    t.addNotes(main, 3, below, 2, main, 14, below, 2, main, 14,
               below, 2, main, 11);
  }

  // bridge bar 2: two quicker flutters, C#/C then C/B
  private static void flutterBar2(Track t) {
    t.addNotes(CS5, 23, R, 1, CS5, 3, C5, 2, CS5, 14, C5, 2, CS5, 2, R, 1);
    t.addNotes(C5, 23, R, 1, C5, 3, B4, 2, C5, 14, B4, 2, C5, 2, R, 1);
  }

  // the G-E walk bar after each bridge
  private static void walkBarA(Track t) {
    el(t, G4);
    t.addNotes(E4, 33, R, 15);           // long E fades under the dives
    el(t, E4);
    el(t, G4);
    el(t, A4);
  }

  // the Bb-B chromatic wiggle bar
  private static void walkBarB(Track t) {
    el(t, AS4);
    el(t, B4);
    el(t, AS4);
    el(t, B4);
    el(t, A4);
    el(t, G4);
    el(t, E4);
    el(t, D4);
  }

  // the last loop bar: the climb into the wrap
  private static void climbBar(Track t) {
    el(t, E5);
    el(t, B4);
    el(t, B4);
    el(t, B4);
    el(t, DS5);
    el(t, FS5);
    el(t, A5);
    el(t, B5);
  }

  private static Track leadLoop() {
    Track t = new Track().withDefaults(LEAD_V, VERSE_DUTY).withDecay(0.35);
    versePhrase(t, true);
    versePhrase(t, false);
    for (int half = 0; half < 2; ++half) {
      t.withDuty(BRIDGE_DUTY);
      flutterBar(t, D5, CS5);
      flutterBar2(t);
      t.withDuty(RUN_DUTY);
      if (half == 0) {
        walkBarA(t);
        walkBarB(t);
      } else {
        walkBarA(t);
        climbBar(t);
      }
    }
    return t;
  }

  @Override
  public Track getLead() {
    Track t = new Track().withDefaults(LEAD_V, RUN_DUTY);
    t.addNotes(leadHead());
    t.addNotes(leadLoop());
    return t;
  }

  /* ==================== PULSE 2 ==================== */

  // answer bars in thirds: D-D-D-E under F#-F#-F#-G, G-G-F#-E-D under
  // the comedown - same rhythm as the lead. The run echo's last note
  // always spills into this bar, an eighth late as ever.
  private static void p2AnsBarA(Track t) {
    t.addNotes(R, S);
    s16(t, E3);                          // the echo remnant
    s16(t, D4);
    s16(t, D4);
    s16(t, D4);
    s16(t, E4);
    t.addNotes(R, E);
    s16(t, D4);
    s16(t, D4);
    s16(t, D4);
    s16(t, E4);
    t.addNotes(R, W / 4);
  }

  private static void p2AnsBarB(Track t) {
    t.addNotes(R, E);
    s16(t, D4);
    s16(t, D4);
    s16(t, D4);
    s16(t, E4);
    t.addNotes(R, S);
    e8(t, G4);
    s16(t, G4);
    e8(t, FS4);
    e8(t, E4);
    el(t, D4);
  }

  private static void p2PushBar(Track t) {
    s16(t, FS4);
    e8(t, FS4);
    e8(t, FS4);
    e8(t, FS4);
    s16(t, FS4);
    t.addNotes(G4, 23, R, 25);
  }

  private static Track harmonyHead() {
    Track t = new Track().withDefaults(P2_VER_V, THIN_DUTY).withDecay(0.35);
    for (int i = 0; i < 3; ++i) {
      t.addNotes(Track.echoOf(runBars(LEAD_V, RUN_DUTY), E, P2_RUN_V / LEAD_V,
                              THIN_DUTY));
      p2AnsBarA(t);
      p2AnsBarB(t);
    }
    t.addNotes(Track.echoOf(runBars(LEAD_V, RUN_DUTY), E, P2_RUN_V / LEAD_V,
                            THIN_DUTY));
    p2AnsBarA(t);
    p2PushBar(t);
    return t;
  }

  // the verse's 16th inner voice - both bars verbatim from the capture,
  // 5+1 sixteenths with the holes where the original breathes
  private static void p2ChugG(Track t) {
    s16(t, G4);
    t.addNotes(A4, 5, R, 7);
    t.addNotes(G4, 5, R, 7);
    s16(t, G4);
    s16(t, A4);
    s16(t, G4);
    s16(t, FS4);
    t.addNotes(G4, 5, R, 7);
    t.addNotes(FS4, 5, R, 7);
    s16(t, FS4);
    s16(t, G4);
    s16(t, FS4);
  }

  private static void p2ChugE(Track t) {
    s16(t, D4);
    t.addNotes(E4, 5, R, 7);
    t.addNotes(E4, 5, R, 7);
    s16(t, D4);
    t.addNotes(E4, 5, R, 7);
    s16(t, D4);
    t.addNotes(E4, 5, R, 7);
    t.addNotes(E4, 5, R, 7);
    s16(t, D4);
    t.addNotes(E4, 5, R, 7);
  }

  private static void p2BridgePair(Track t) {
    t.withVolume(P2_BRI_V).withDuty(BRIDGE_DUTY);
    t.addNotes(B4, 95, R, 1);            // whole note under the D flutter
    t.addNotes(AS4, 47, R, 1, A4, 47, R, 1);
    t.withVolume(P2_BRI_V).withDuty(THIN_DUTY);
  }

  private static void p2WalkA(Track t) {
    t.addNotes(R, E);
    el(t, G4);
    el(t, A4);
    el(t, G4);
    el(t, B4);
    el(t, G4);
    t.addNotes(R, E);
    el(t, G4);
  }

  private static void p2WalkB(Track t) {
    el(t, A4);
    el(t, G4);
    el(t, B4);
    el(t, G4);
    t.addNotes(R, E);
    el(t, G4);
    el(t, FS4);
    el(t, G4);
  }

  private static Track harmonyLoop() {
    Track t = new Track().withDefaults(P2_VER_V, THIN_DUTY).withDecay(0.35);
    for (int bar = 0; bar < 8; ++bar) {
      if (bar % 2 == 0) {
        p2ChugG(t);
      } else {
        p2ChugE(t);
      }
    }
    for (int half = 0; half < 2; ++half) {
      p2BridgePair(t);
      p2WalkA(t);
      p2WalkB(t);
      t.withVolume(P2_VER_V);
    }
    return t;
  }

  @Override
  public Track getHarmony() {
    Track t = new Track().withDefaults(P2_VER_V, THIN_DUTY);
    t.addNotes(harmonyHead());
    t.addNotes(harmonyLoop());
    return t;
  }

  /* ==================== BASS (triangle) ==================== */

  // the run again, at the triangle's true octave (label+0): E3 root -
  // NOT the old rip's E2. Half-length stabs per the capture.
  private static void bassRunBars(Track t) {
    t.addNotes(E3, 12, R, 12);
    e8(t, G3);
    e8(t, B3);
    t.addNotes(D4, 12, R, 6, CS4, 12, R, 6);
    el(t, A3);
    t.addNotes(C4, 12, R, 6, B3, 12, R, 6);
    el(t, G3);
    e8(t, A3);
    e8(t, G3);
    s16(t, E3);
    e8(t, D3);
    s16(t, E3);
  }

  // the eighth-note pedal bar under answer A
  private static void bassPedalBar(Track t) {
    t.addNotes(R, E);
    for (int i = 0; i < 7; ++i) {
      t.addNotes(E3, 9, R, 3);
    }
  }

  // under answer B: E E F# F# G B D(rings)
  private static void bassPedalWalk(Track t) {
    t.addNotes(E3, 5, R, 7);
    el(t, E3);
    e8(t, FS3);
    el(t, FS3);
    t.addNotes(G3, 5, R, 7);
    t.addNotes(B3, 5, R, 7);
    t.addNotes(D3, 12, R, 12);
  }

  // head bar 15: the D pushes, then the turnaround dives - per-frame
  // gliss cascades rendered as pitch envelopes into their target notes
  private static void bassPushDives(Track t) {
    s16(t, D3);
    e8(t, D3);
    e8(t, D3);
    e8(t, D3);
    s16(t, D3);
    s16(t, E3);
    t.withPitchEnv(7, 5).addNotes(D4, 6, D4, 6);      // A4 zips down
    t.withPitchEnv(4, 4).addNotes(D3, 6);             // F3 scoop
    t.withPitchEnv(3, 4).addNotes(CS3, 6, C3, 6, C3, 6, B2, 6);
    t.withNoFx();
  }

  private static Track bassHead() {
    Track t = new Track().withDefaults(BASS_V, BASS_DUTY);
    for (int i = 0; i < 3; ++i) {
      bassRunBars(t);
      bassPedalBar(t);
      bassPedalWalk(t);
    }
    bassRunBars(t);
    bassPedalBar(t);
    bassPushDives(t);
    return t;
  }

  // the real verse funk cell (per the capture, not the tab): root eighths
  // with a hole on beat 2, then the 16th climb through beats 3-4
  private static void bassGrooveBar(Track t, int root, int third, int fifth) {
    el(t, root);
    t.addNotes(root, 11, R, 13);         // ...the beat-2 hole
    el(t, root);
    s16(t, root);
    t.addNotes(third, 5, R, 7);
    t.addNotes(fifth, 5, R, 7);
    t.addNotes(third, 5, R, 1);
    el(t, root);
  }

  // bridge: straight eighths walking G - F# - F
  private static void bassBridgeBars(Track t) {
    for (int i = 0; i < 8; ++i) {
      t.addNotes(G3, 9, R, 3);
    }
    for (int i = 0; i < 4; ++i) {
      t.addNotes(FS3, 9, R, 3);
    }
    for (int i = 0; i < 4; ++i) {
      t.addNotes(F3, 9, R, 3);
    }
  }

  // the walk bar the old rip kept (rightly), an octave up
  private static void bassWalkBar(Track t) {
    t.addNotes(R, E);
    el(t, E3);
    el(t, FS3);
    el(t, E3);
    el(t, G3);
    el(t, E3);
    t.addNotes(R, E);
    el(t, E3);
  }

  // the dead-stop bar that was never dead: dive-bomb cascades. Two
  // B4-to-G#3 dives, two A3 scoops, two C#5 zips, then three G#3 scoops.
  private static void bassDiveBar(Track t) {
    t.withPitchEnv(14, 10).addNotes(GS3, 12, GS3, 12);
    t.withPitchEnv(9, 9).addNotes(C3, 12, C3, 12);
    t.withPitchEnv(9, 4).addNotes(E4, 6, E4, 6);
    t.withPitchEnv(9, 9).addNotes(C3, 12);
    t.withPitchEnv(8, 9).addNotes(C3, 12, C3, 12);
    t.withNoFx();
  }

  private static Track bassLoop() {
    Track t = new Track().withDefaults(BASS_V, BASS_DUTY);
    for (int i = 0; i < 2; ++i) {
      bassGrooveBar(t, E3, G3, B3);
      bassGrooveBar(t, E3, G3, B3);
      bassGrooveBar(t, D3, FS3, A3);
      bassGrooveBar(t, E3, G3, B3);
    }
    for (int half = 0; half < 2; ++half) {
      bassBridgeBars(t);
      bassWalkBar(t);
      bassDiveBar(t);
    }
    return t;
  }

  @Override
  public Track getBass() {
    Track t = new Track().withDefaults(BASS_V, BASS_DUTY);
    t.addNotes(bassHead());
    t.addNotes(bassLoop());
    return t;
  }

  /* ==================== DRUMS ==================== */

  private static void hit(Track t, int voice, double vol, int slot) {
    t.withVolume(vol).addNotes(voice, T, R, slot - T);
  }

  // head kit: kick-sweep on every beat, soft tick on the "e"
  private static void headKitBar(Track t) {
    for (int beat = 0; beat < 4; ++beat) {
      hit(t, KICK, 1.0, S);
      hit(t, HIHAT, HAT_SOFT, E + S);
    }
  }

  // head bar 16: the 13-hit roll under the rung-out B
  private static void rollBar(Track t) {
    int[] slots = {S, E, E, E, S, S, S, S, S, S, S, S, S};
    for (int len : slots) {
      hit(t, SNARE, ROLL_V, len);
    }
  }

  // verse groove: tick 1, snare 2, ride run-up, snare 4
  private static void verseKitBar(Track t) {
    hit(t, HIHAT, HAT_V, Q);
    hit(t, SNARE, SNARE_V, E + S);
    hit(t, HIHAT, HAT_V, S);
    hit(t, HIHAT, HAT_V, S);
    hit(t, HIHAT, HAT_V, S);
    hit(t, HIHAT, HAT_V, E);
    hit(t, SNARE, SNARE_V, Q);
  }

  // bridge groove: hats in eighths, snare on 4 (first bar) or 2 and 4
  private static void bridgeKitBarA(Track t) {
    for (int i = 0; i < 6; ++i) {
      hit(t, HIHAT, HAT_V, E);
    }
    hit(t, SNARE, SNARE_V, Q);
  }

  private static void bridgeKitBarB(Track t) {
    hit(t, HIHAT, HAT_V, E);
    hit(t, HIHAT, HAT_V, E);
    hit(t, SNARE, SNARE_V, Q);
    hit(t, HIHAT, HAT_V, E);
    hit(t, HIHAT, HAT_V, E);
    hit(t, SNARE, SNARE_V, Q);
  }

  // the climb bar winds down on fading quarter ticks
  private static void climbKitBar(Track t) {
    hit(t, HIHAT, 0.50, Q);
    hit(t, HIHAT, 0.42, Q);
    hit(t, HIHAT, 0.34, Q);
    hit(t, HIHAT, 0.26, Q);
  }

  @Override
  public Track getDrums() {
    Track t = new Track().withDefaults(DRUM_VOL, DRUM_DUTY);
    for (int bar = 0; bar < 15; ++bar) {         // head
      headKitBar(t);
    }
    rollBar(t);
    for (int bar = 0; bar < 8; ++bar) {          // verse
      verseKitBar(t);
    }
    for (int half = 0; half < 2; ++half) {       // bridge + walks
      bridgeKitBarA(t);
      bridgeKitBarB(t);
      bridgeKitBarB(t);
      if (half == 0) {
        bridgeKitBarB(t);
      } else {
        climbKitBar(t);
      }
    }
    return t;
  }
}

