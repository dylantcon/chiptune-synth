package chiptunesynth.songs;

import chiptunesynth.ChiptuneSong;
import chiptunesynth.Track;

/**
 * Battletoads - Stage 5: Surf City, by David Wise.
 *
 * Rebuilt by hand in the HyruleTempleSong "house style" from the old MIDI rip
 * (see git history for the generated version): sections are functions, bars
 * are loops, variation rules are conditionals, and the harmonic plan is data.
 * Sounding registers were kept from the rip's octave-corrected mix.
 *
 * FORM — one pass is 29 bars, matching the original recording's 57.31 s loop:
 *
 *   HOOK   8 bars   four 2-bar statements: C, C, F, C. Bar 1 is the surf
 *                   hook (root root root b3 4 | 5 5 4 b3 4), bar 2 is two
 *                   pickup 16ths and the swelled "chime" — the lead borrows
 *                   its own dead air for the chime (one channel, two voices:
 *                   the rip's lendFifth trick, kept on purpose) while pulse 2
 *                   swells an Ab underneath and sparkles root+3rd.
 *   TURN   1 bar    Bb-Bb-Bb-F(held) / Eb Eb Eb Eb F F — tips into the chug.
 *   CHUG   8 bars   sixteen half-bar cells of the low 16th-note riff: bottom
 *                   note walks G / Ab / Bb / Ab under a fixed C-D-Eb-G-Eb-D-Eb
 *                   figure. Pass 1 plays it as the BUILD-UP into the chorus:
 *                   the bass drives straight eighths with octave pops the
 *                   whole way (it is the section's engine), the riff enters
 *                   lean (lead terraces 0.60 -> full at the halfway mark),
 *                   the kit stacks up two bars at a time, and an ascending
 *                   C-Eb-F-G bass run + fill make the chorus downbeat land
 *                   like an arrival.
 *   CNR    8 bars   call & response, four 2-bar cells: lead states the hook
 *                   bar low (C3); after a fat breath (an eighth plus a 16th)
 *                   pulse 2 answers an octave up (C5-Bb4 / G G F Eb F).
 *   CODA   4 bars   the hook figure spread wide: lead at C3-C4, pulse 2
 *                   doubling an octave up, bass dropping to whole-note
 *                   C2 C2 Bb1 Ab1 slides. Ab resolves back to the hook's C.
 *
 * The song plays TWO passes (58 bars, ~114.6 s — twice the original loop).
 * Pass 1 is the faithful arrangement; pass 2 is the same music developed the
 * way Wise developed his repeats:
 *
 *   HOOK   pulse 2 becomes a 3-sixteenth echo of the lead (Track.echoOf),
 *          the lead's duty fattens 0.205 -> 0.35 at a slightly relaxed
 *          volume (the era's "second verse, bigger instrument" switch),
 *          bass octave-pumps, drums go four-on-the-floor -> full backbeat.
 *   TURN   descending tom fill instead of the snare build.
 *   CHUG   already at full tilt (the build-up belongs to pass 1), pulse 2
 *          plays C-minor arp stabs (withArp 0,3,7) on the off-beats, and
 *          the last four cells lift the riff an octave.
 *   CNR    pulse 2 does double duty: echoes the call in bar 1, answers in
 *          bar 2. Drums move to 16th-note hats.
 *   CODA   drums drop to half-time, then a snare/tom fill turns the loop
 *          around; bass climbs Ab1-Bb1-B1-C2 back into the hook.
 *
 * TEMPO — the original runs 29 bars in 57.31 s = 121.4 BPM. The house grid
 * (96-frame bars) is 150 BPM at 60 fps, so scale = 121.4 / 150.
 *
 * @author dylan
 */
public class SurfCitySong implements ChiptuneSong {

  @Override
  public double getTempoScale() {
    return 0.8096;
  }

  /* === VOICES / MIX === */
  private static final double CHIME_VOL   = 0.30;
  // pass-2 "horn" lead: fatter than the 0.205 verse voice, but 0.499 at full
  // lead volume honked — 35% duty and a touch less level keeps the timbre
  // shift without the aggression
  private static final double P2_LEAD_VOL  = 0.63;
  private static final double P2_LEAD_DUTY = 0.35;
  private static final double STAB_DUTY   = 0.25;

  // the Wise echo: 3 sixteenths behind the lead, half volume, thin duty
  private static final int    ECHO_DELAY = 3 * S;
  private static final double ECHO_VOL   = 0.50;
  private static final double ECHO_DUTY  = 0.125;

  private static final double KICK_VOL  = 0.90;  // KickVoice ignores this
  private static final double SNARE_VOL = 0.60;
  private static final double HIHAT_VOL = 0.25;
  private static final double TOM_VOL   = 0.65;

  private static final int HIT = T;              // 3-frame drum hit

  /* === ARTICULATION ===
   * The rip's feel is a stab on every note: ~5 frames of tone, the rest of
   * the slot silent (a rest is the only true note-off, see PulseChannel).
   * These two helpers are that feel on the house grid. */
  private static void s16(Track t, int p) {
    t.addNotes(p, 4, R, 2);
  }

  private static void e8(Track t, int p) {
    t.addNotes(p, 5, R, 7);
  }

  private static Track lead(double vol, double duty) {
    return new Track().withDefaults(vol, duty).withVibrato(0.4, 5.5, 8);
  }

  /* ==================== LEAD ==================== */

  // bar 1 of a hook statement, root-relative: root root root b3 4 | 5 5 4 b3 4
  private static void hookBar(Track t, int root) {
    e8(t, root);
    s16(t, root);
    s16(t, root);
    s16(t, root + 3);
    e8(t, root + 5);
    e8(t, root + 7);
    s16(t, root + 7);
    e8(t, root + 5);
    s16(t, root + 3);
    t.addNotes(root + 5, 5, R, 13);      // final stab, then breathe
  }

  // bar 2: two pickup 16ths, then the lead lends its rest to the chime
  private static void hookChimeBar(Track t, int root, double leadVol,
                                   double leadDuty) {
    s16(t, root);
    s16(t, root);
    t.addNotes(R, E);
    t.withVolume(CHIME_VOL).withDuty(HARMONY_DUTY)
     .withNoFx().withSwell().withDecay(SUSTAINED)
     .addNotes(DS4, 40);                 // Eb chime, all statements (per rip)
    t.withVolume(leadVol).withDuty(leadDuty)
     .withNoFx().withVibrato(0.4, 5.5, 8).withDecay(LEGATO)
     .addNotes(R, 32);
  }

  private static final int[] HOOK_ROOTS = {C3, C3, F3, C3};

  // withChime=false yields the pure melody — the echo source for pass 2
  private static Track leadHook(boolean withChime, double vol, double duty) {
    Track t = lead(vol, duty);
    for (int root : HOOK_ROOTS) {
      hookBar(t, root);
      if (withChime) {
        hookChimeBar(t, root, vol, duty);
      } else {
        s16(t, root);
        s16(t, root);
        t.addNotes(R, 84);
      }
    }
    return t;
  }

  private static Track leadTurn() {
    Track t = lead(LEAD_VOL, LEAD_DUTY);
    e8(t, AS2);
    s16(t, AS2);
    s16(t, AS2);
    t.addNotes(F3, 14, R, 4);            // held F tips the phrase over
    e8(t, DS3);
    e8(t, DS3);
    e8(t, DS3);
    s16(t, DS3);
    s16(t, F3);
    s16(t, F3);
    return t;
  }

  // one half-bar chug cell: walking bottom note + the fixed upper figure
  private static void chugCell(Track t, int bottom, int lift) {
    s16(t, bottom);
    s16(t, C3 + lift);
    s16(t, D3 + lift);
    s16(t, DS3 + lift);
    s16(t, G3 + lift);
    s16(t, DS3 + lift);
    s16(t, D3 + lift);
    s16(t, DS3 + lift);
  }

  private static final int[] CHUG_BOTTOMS = {G2, GS2, AS2, GS2};

  private static Track leadChug(int pass) {
    Track t = lead(LEAD_VOL, LEAD_DUTY);
    if (pass == 1) {
      t.withVolume(0.60);                // enter leaner...
    }
    for (int cell = 0; cell < 16; ++cell) {
      if (pass == 1 && cell == 8) {
        t.withVolume(LEAD_VOL);          // ...terrace up at the halfway mark
      }
      // pass 2 climax: the last four cells jump the riff up an octave
      int lift = (pass == 2 && cell >= 12) ? 12 : 0;
      chugCell(t, CHUG_BOTTOMS[cell % 4] + lift, lift);
    }
    return t;
  }

  // the call is hook bar 1 alone — used by the lead and echoed by pulse 2
  private static Track callBar() {
    Track t = lead(LEAD_VOL, LEAD_DUTY);
    hookBar(t, C3);
    return t;
  }

  private static Track leadCnr(double vol, double duty) {
    Track t = lead(vol, duty);
    for (int cell = 0; cell < 4; ++cell) {
      hookBar(t, C3);                    // call...
      t.addNotes(R, W);                  // ...pulse 2 owns the answer bar
    }
    return t;
  }

  // the hook figure spread wide; `up` lets pulse 2 reuse it an octave higher
  private static void codaBar(Track t, int up) {
    e8(t, C3 + up);
    s16(t, C3 + up);
    s16(t, C3 + up);
    s16(t, G3 + up);
    e8(t, AS3 + up);
    e8(t, C4 + up);
    s16(t, C4 + up);
    e8(t, AS3 + up);
    s16(t, F3 + up);
    s16(t, G3 + up);
    s16(t, DS3 + up);
    s16(t, F3 + up);
  }

  private static Track leadCoda(double vol, double duty) {
    Track t = lead(vol, duty);
    for (int bar = 0; bar < 4; ++bar) {
      codaBar(t, 0);
    }
    return t;
  }

  @Override
  public Track getLead() {
    Track t = new Track().withDefaults(LEAD_VOL, LEAD_DUTY);
    for (int pass = 1; pass <= 2; ++pass) {
      // pass 2 fattens the melody's duty — the classic second-verse timbre
      // switch on real 2A03 drivers — at a slightly relaxed level so the
      // fuller voice doesn't read as a blaring horn
      double vol = (pass == 1) ? LEAD_VOL : P2_LEAD_VOL;
      double duty = (pass == 1) ? LEAD_DUTY : P2_LEAD_DUTY;
      t.addNotes(leadHook(true, vol, duty))
       .addNotes(leadTurn())
       .addNotes(leadChug(pass))
       .addNotes(leadCnr(vol, duty))
       .addNotes(leadCoda(vol, duty));
    }
    return t;
  }

  /* ==================== HARMONY (pulse 2) ==================== */

  private static Track harmony() {
    return new Track().withDefaults(HARMONY_VOL, HARMONY_DUTY)
                      .withVibrato(0.3, 5.5, 10);
  }

  private static Track restBars(int bars) {
    return new Track().addNotes(R, bars * W);
  }

  // pass 1 hook answer: Ab swell under the lead's Eb chime (an Ab-Eb dyad
  // across the two channels), then a root+3rd sparkle on the last beat
  private static Track harmonyHookAnswers() {
    Track t = harmony();
    int[] sparkleRoots = {C4, C4, F4, C4};
    for (int root : sparkleRoots) {
      t.addNotes(R, W)                   // silent under the hook bar
       .addNotes(R, Q)
       .withSwell().withDecay(SUSTAINED).addNotes(GS3, 40)
       .withNoFx().withDecay(LEGATO);
      s16(t, root);
      s16(t, root + 3);
      s16(t, root + 3);
      t.addNotes(R, 14);
    }
    return t;
  }

  // pass 2 chug: C-minor arp stabs on the off-beats — one channel, three notes
  private static Track harmonyChugStabs() {
    Track t = new Track().withDefaults(HARMONY_VOL, STAB_DUTY)
                         .withArp(0, 3, 7).withArpSpeed(2)
                         .withDecay(PORTATO);
    for (int bar = 0; bar < 8; ++bar) {
      for (int beat = 0; beat < 4; ++beat) {
        t.addNotes(R, E, C4, E);
      }
    }
    return t;
  }

  // the answer: the hook's back half, an octave up, prefixed C5-Bb4 (the
  // rip's written octave). It enters on the "and" of 1 — the eighth-note
  // breath between call and answer is part of the original's phrasing
  // (a quarter was too long; ear-tuned).
  //
  // The two rests are deliberately LOPSIDED, ~2.4:1: call ends 13 frames
  // before the bar + 18 more before the answer enters (~31 frames of air),
  // but the next call re-enters right on the downbeat (~13 frames of air).
  // Both phrases end with the same stab-plus-breath tail; only the delayed
  // entrance skews them. The extra 16th of lateness comes out of the Bb's
  // slot, so from the G4 on, the phrase sits at the same groove position —
  // only the entrance saunters. Keeping it uneven is what makes the beat
  // lively instead of rigid — don't "fix" the asymmetry.
  private static void respBar(Track t) {
    t.addNotes(R, E + S);
    e8(t, C5);
    t.addNotes(AS4, 12);
    e8(t, G4);
    s16(t, G4);
    e8(t, F4);
    s16(t, DS4);
    t.addNotes(F4, 5, R, 13);
  }

  private static Track harmonyCnr(int pass) {
    Track t = harmony();
    for (int cell = 0; cell < 4; ++cell) {
      if (pass == 1) {
        t.addNotes(R, W);
      } else {
        // double duty: shadow the call, then still make the answer in time
        t.addNotes(Track.echoOf(callBar(), ECHO_DELAY, ECHO_VOL, ECHO_DUTY));
      }
      respBar(t);
    }
    return t;
  }

  private static Track harmonyCoda() {
    Track t = harmony();
    for (int bar = 0; bar < 4; ++bar) {
      codaBar(t, 12);                    // parallel octaves with the lead
    }
    return t;
  }

  @Override
  public Track getHarmony() {
    Track t = new Track().withDefaults(HARMONY_VOL, HARMONY_DUTY);
    for (int pass = 1; pass <= 2; ++pass) {
      if (pass == 1) {
        t.addNotes(harmonyHookAnswers())
         .addNotes(restBars(1))          // turn
         .addNotes(restBars(8));         // chug (breakdown pass)
      } else {
        t.addNotes(Track.echoOf(leadHook(false, LEAD_VOL, LEAD_DUTY),
                                ECHO_DELAY, ECHO_VOL, ECHO_DUTY))
         .addNotes(Track.echoOf(leadTurn(), ECHO_DELAY, ECHO_VOL, ECHO_DUTY))
         .addNotes(harmonyChugStabs());
      }
      t.addNotes(harmonyCnr(pass))
       .addNotes(harmonyCoda());
    }
    return t;
  }

  /* ==================== BASS (triangle) ==================== */

  private static Track bass() {
    return new Track().withDefaults(BASS_VOL, BASS_DUTY);
  }

  // driving straight eighths; the 3-frame gap is the triangle's only way to
  // re-articulate a repeated pitch (no volume envelope on that channel)
  private static void pumpBar(Track t, int root) {
    for (int i = 0; i < 8; ++i) {
      t.addNotes(root, DS, R, T);
    }
  }

  private static void octavePumpBar(Track t, int root) {
    for (int i = 0; i < 4; ++i) {
      t.addNotes(root - 12, DS, R, T, root, DS, R, T);
    }
  }

  // the held root under the chime bar; pass 2 walks b3-4 up into the next
  // statement (C: Eb-F, F: Ab-Bb — Bb leads the ear back to C)
  private static void sustainBar(Track t, int root, int pass) {
    if (pass == 1) {
      t.addNotes(root, 40, R, 56);
    } else {
      t.addNotes(root, 40, R, 8);
      e8(t, root + 3);
      e8(t, root + 5);
      t.addNotes(R, Q);
    }
  }

  private static Track bassHook(int pass) {
    Track t = bass();
    for (int root : HOOK_ROOTS) {
      if (pass == 1) {
        pumpBar(t, root);
      } else {
        octavePumpBar(t, root);
      }
      sustainBar(t, root, pass);
    }
    return t;
  }

  private static Track bassTurn() {
    Track t = bass();
    for (int i = 0; i < 4; ++i) {
      t.addNotes(AS2, DS, R, T);
    }
    t.addNotes(F2, 40, R, 8);
    return t;
  }

  // syncopated pedal with a Bb neighbor and the Eb-F walk in the tail —
  // the cell the rip used under both the chug and the call & response
  private static void chugBassBar(Track t, int root) {
    e8(t, root);
    t.addNotes(R, S);
    s16(t, root);
    e8(t, root);
    t.addNotes(R, S);
    s16(t, root - 2);
    e8(t, root);
    t.addNotes(R, S);
    s16(t, root);
    e8(t, root);
    s16(t, root + 3);
    s16(t, root + 5);
  }

  private static Track bassChug() {
    Track t = bass();
    for (int bar = 0; bar < 7; ++bar) {
      chugBassDriveBar(t, C3);
    }
    ascentBar(t);
    return t;
  }

  // the build-up's engine: no silent off-beats — a driving eighth-note pump
  // with an octave pop, the Bb dip, and the Eb-F walk. The sparser
  // chugBassBar stays under the call & response, where the dialogue needs
  // the room; here the bass is the hype.
  private static void chugBassDriveBar(Track t, int root) {
    int[] line = {root, root, root + 12, root,
                  root - 2, root, root + 3, root + 5};
    for (int pitch : line) {
      t.addNotes(pitch, DS, R, T);
    }
  }

  // the ramp out of the build-up: pumped eighths climbing C-Eb-F-G so the
  // chorus downbeat lands like an arrival instead of just another bar
  private static void ascentBar(Track t) {
    int[] steps = {C3, C3, DS3, DS3, F3, F3, G3, G3};
    for (int step : steps) {
      t.addNotes(step, DS, R, T);
    }
  }

  private static Track bassCnr() {
    Track t = bass();
    for (int bar = 0; bar < 8; ++bar) {
      chugBassBar(t, C3);
    }
    return t;
  }

  private static Track bassCoda(int pass) {
    // whole notes gliding down C2 C2 Bb1 Ab1 — the slide makes the triangle
    // swoop between roots instead of stepping
    Track t = bass().withSlide(10);
    t.addNotes(C2, W, C2, W, AS1, W);
    if (pass == 1) {
      t.addNotes(GS1, W);
    } else {
      t.addNotes(GS1, 48, AS1, E, B1, E, C2, Q);   // climb back to the hook
    }
    return t;
  }

  @Override
  public Track getBass() {
    Track t = new Track().withDefaults(BASS_VOL, BASS_DUTY);
    for (int pass = 1; pass <= 2; ++pass) {
      t.addNotes(bassHook(pass))
       .addNotes(bassTurn())
       .addNotes(bassChug())
       .addNotes(bassCnr())
       .addNotes(bassCoda(pass));
    }
    return t;
  }

  /* ==================== DRUMS (noise + drum voices) ==================== */

  private static Track drums() {
    return new Track().withDefaults(DRUM_VOL, DRUM_DUTY);
  }

  private static void hit(Track t, int voice, double vol, int slot) {
    t.withVolume(vol).addNotes(voice, HIT, R, slot - HIT);
  }

  private static void fourFloorBar(Track t) {
    hit(t, KICK, KICK_VOL, Q);
    hit(t, HIHAT, HIHAT_VOL, Q);
    hit(t, KICK, KICK_VOL, Q);
    hit(t, HIHAT, HIHAT_VOL, Q);
  }

  // K . S . | hat 8ths, kick pushing on the "e of 3" and 3.5
  private static void backbeatBar(Track t) {
    hit(t, KICK, KICK_VOL, E);
    hit(t, HIHAT, HIHAT_VOL, E);
    hit(t, SNARE, SNARE_VOL, E);
    hit(t, HIHAT, HIHAT_VOL, E);
    hit(t, HIHAT, HIHAT_VOL, S);
    hit(t, KICK, KICK_VOL, S);
    hit(t, KICK, KICK_VOL, E);
    hit(t, SNARE, SNARE_VOL, E);
    hit(t, HIHAT, HIHAT_VOL, E);
  }

  // pass-2 chug variant: the double-kick moves to beat 2-and
  private static void backbeat2Bar(Track t) {
    hit(t, KICK, KICK_VOL, E);
    hit(t, HIHAT, HIHAT_VOL, E);
    hit(t, SNARE, SNARE_VOL, E);
    hit(t, HIHAT, HIHAT_VOL, S);
    hit(t, KICK, KICK_VOL, S);
    hit(t, KICK, KICK_VOL, E);
    hit(t, HIHAT, HIHAT_VOL, E);
    hit(t, SNARE, SNARE_VOL, E);
    hit(t, HIHAT, HIHAT_VOL, E);
  }

  private static void hats16Bar(Track t) {
    for (int i = 0; i < 16; ++i) {
      hit(t, HIHAT, (i % 2 == 0) ? 0.28 : 0.16, S);   // accent the 8ths
    }
  }

  // lean build-up opener: kick on 1 and 3 under straight 8th hats
  private static void kickHatsBar(Track t) {
    for (int i = 0; i < 8; ++i) {
      if (i == 0 || i == 4) {
        hit(t, KICK, KICK_VOL, E);
      } else {
        hit(t, HIHAT, (i % 2 == 0) ? 0.28 : 0.18, E);
      }
    }
  }

  // the pass-1 turnaround: a 16th snare roll that swells from nothing —
  // per-hit withVolume as a crescendo, then the floor drops out (breakdown)
  private static void buildBar(Track t) {
    for (int i = 0; i < 16; ++i) {
      hit(t, SNARE, 0.20 + i * 0.03, S);
    }
  }

  // any pitched noise note routes to the TomVoice, so fills are just notes
  private static void tomFillBar(Track t) {
    for (int i = 0; i < 4; ++i) {
      hit(t, HIHAT, HIHAT_VOL, E);
    }
    hit(t, G3, TOM_VOL, E);
    hit(t, DS3, TOM_VOL, E);
    hit(t, C3, TOM_VOL, E);
    hit(t, AS2, TOM_VOL, E);
  }

  private static void groove16Bar(Track t) {
    int[] voices = {KICK, HIHAT, HIHAT, HIHAT, SNARE, HIHAT, HIHAT, HIHAT,
                    KICK, HIHAT, KICK, HIHAT, SNARE, HIHAT, HIHAT, HIHAT};
    for (int v : voices) {
      double vol = (v == KICK) ? KICK_VOL : (v == SNARE) ? SNARE_VOL : 0.20;
      hit(t, v, vol, S);
    }
  }

  private static void halfTimeBar(Track t) {
    hit(t, KICK, KICK_VOL, Q);
    hit(t, HIHAT, HIHAT_VOL, Q);
    hit(t, SNARE, SNARE_VOL, Q);
    hit(t, HIHAT, HIHAT_VOL, Q);
  }

  // pass-2 loop turnaround: snare swell into a falling tom run
  private static void fill16Bar(Track t) {
    for (int i = 0; i < 8; ++i) {
      hit(t, SNARE, 0.30 + i * 0.05, S);
    }
    int[] toms = {G3, F3, DS3, D3, C3, AS2, GS2, G2};
    for (int tom : toms) {
      hit(t, tom, TOM_VOL, S);
    }
  }

  @Override
  public Track getDrums() {
    Track t = drums();
    for (int pass = 1; pass <= 2; ++pass) {
      // HOOK
      for (int bar = 0; bar < 8; ++bar) {
        if (pass == 1) {
          fourFloorBar(t);
        } else {
          backbeatBar(t);
        }
      }
      // TURN
      if (pass == 1) {
        buildBar(t);
      } else {
        tomFillBar(t);
      }
      // CHUG — pass 1 is the build-up: kick+hats, snare enters, double-kick,
      // 16ths, then a fill hurls it into the chorus. Pass 2 stays at full tilt.
      if (pass == 1) {
        kickHatsBar(t);
        kickHatsBar(t);
        backbeatBar(t);
        backbeatBar(t);
        backbeat2Bar(t);
        backbeat2Bar(t);
        groove16Bar(t);
        fill16Bar(t);
      } else {
        for (int bar = 0; bar < 8; ++bar) {
          backbeat2Bar(t);
        }
      }
      // CNR
      for (int bar = 0; bar < 8; ++bar) {
        if (pass == 1) {
          backbeatBar(t);
        } else {
          groove16Bar(t);
        }
      }
      // CODA
      for (int bar = 0; bar < 3; ++bar) {
        if (pass == 1) {
          backbeatBar(t);
        } else {
          halfTimeBar(t);
        }
      }
      if (pass == 1) {
        tomFillBar(t);
      } else {
        fill16Bar(t);
      }
    }
    return t;
  }
}
