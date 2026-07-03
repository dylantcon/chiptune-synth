/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chiptunesynth.songs;

import chiptunesynth.ChiptuneSong;
import chiptunesynth.Note;
import chiptunesynth.Track;

/**
 * Generated from Flashman.mid (Mega Man 2, by Takashi Tateishi) MIDI arranged
 * by Steven C. King; converted to ChiptuneSynth format. Uses constants from
 * ChiptuneSong interface. Non-standard durations (humanization
 * micro-articulations from the source MIDI) are expressed as arithmetic against
 * the named constants - e.g. H - 1 means "half note minus 1 frame" which gives
 * a tiny breath before the next note.
 *
 * The source MIDI's rhythm section was "humanized" into mush: the bass lost
 * the NES original's relentless eighth-note pedal and its syncopated chorus
 * cell, and every drum hit landed at the same flat velocity. The bass was
 * rebuilt against auriplane's Gametabs bass tab of the original
 * (https://gametabs.net/tabs/mega-man-2/flashmans-stage), and getDrums()
 * applies per-voice velocity layers. Lead/harmony note data is untouched.
 *
 * @author dylan
 */
public class FlashmanSong implements ChiptuneSong {
  
  @Override
  public Track getLead() {
    Track t = new Track().withDefaults(LEAD_VOL, LEAD_DUTY);
    // The lead's phrases land on sustained targets (E4/H, D5/W-2, CS5/H,
    // C5/H) — vibrato gives those the singing wobble while the
    // sixteenth-and-eighth filigree stays clean under the 12-frame delay.
    // DEFAULT_DECAY (0.6) emptied those targets out before they resolved (a
    // whole note faded to ~4% by its end); 0.35 keeps them singing the way
    // the original's full-volume square does, while the short runs are over
    // before the envelope matters.
    t.withVibrato(0.3, 5.5, 12).withDecay(0.35);
    t.addNotes(
            R, H + E, E3, Q, G3, S, R, S, B3, S, R, S,
            D4, DE, CS4, DE, A3, E, C4, DE, B3, DE, G3, E,
            A3, S, R, S, G3, S, R, S, E3, S, D3, E,
            E3, S, FS4, DE, G4, S, R, E, FS4, DE, G4, S,
            R, DQ, FS4, DE, G4, S, R, S, B4, S, R, S,
            B4, S, A4, S, R, S, G4, S, R, S, FS4, E,
            R, E, E3, Q, G3, S, R, S, B3, S, R, S,
            D4, DE, CS4, DE, A3, E, C4, DE, B3, DE, G3, E,
            A3, S, R, S, G3, S, R, S, E3, S, D3, E,
            E3, S, FS4, DE, G4, S, R, E, FS4, DE, G4, S,
            R, DQ, FS4, DE, G4, S, R, S, B4, S, R, S,
            B4, S, A4, S, R, S, G4, S, R, S, FS4, E,
            R, E, E3, Q, G3, S, R, S, B3, S, R, S,
            D4, DE, CS4, DE, A3, E, C4, DE, B3, DE, G3, E,
            A3, S, R, S, G3, S, R, S, E3, S, D3, E,
            E3, S, FS4, DE, G4, S, R, E, FS4, DE, G4, S,
            R, DQ, FS4, DE, G4, S, R, S, B4, S, R, S,
            B4, S, A4, S, R, S, G4, S, R, S, FS4, E,
            R, E, E3, Q, G3, S, R, S, B3, S, R, S,
            D4, DE, CS4, DE, A3, E, C4, DE, B3, DE, G3, E,
            A3, S, R, S, G3, S, R, S, E3, S, D3, E,
            E3, S, FS4, DE, G4, S, R, E, FS4, DE, G4, S,
            R, Q, A4, E, R, S, A4, S, R, S, A4, S,
            R, S, A4, S, B4, Q, R, DQ, E4, E, B4, E,
            A4, E, B4, Q, A4, E, G4, E, A4, E, B4, E,
            R, E, E4, H - 1, R, 1, G4, E, FS4, E, R, E,
            FS4, E, R, E, FS4, E, E4, E, D4, E, E4, H + E - 1,
            R, E + 1, E4, E, G4, E, A4, E, R, E, E4, E,
            B4, E, A4, E, B4, Q, A4, E, G4, E, A4, E,
            B4, E, R, E, E4, H - 1, R, 1, G4, E, FS4, E,
            R, E, FS4, E, R, E, G4, E, A4, E, FS4, E,
            E4, H + E - 1, R, E + 1, E4, E, G4, E, B4, E, D5, W - 2,
            R, 2, CS5, H - 1, R, 1, C5, H - 1, R, 1, G4, E,
            E4, DQ - 1, R, E + 1, E4, E, G4, E, A4, E, AS4, E,
            B4, E, AS4, E, B4, E, A4, E, G4, E, E4, E,
            D4, E, D5, W - 2, R, 2, CS5, H - 1, R, 1, C5, H - 1,
            R, 1, G4, E, E4, DQ - 1, R, E + 1, E4, E, G4, E,
            A4, E, E5, E, B4, DQ, DS5, E, FS5, E, A5, E,
            B5, E, R, E, E4, E, B4, E, A4, E, B4, Q,
            A4, E, G4, E, A4, E, B4, E, R, E, E4, H - 1,
            R, 1, G4, E, FS4, E, R, E, FS4, E, R, E,
            FS4, E, E4, E, D4, E, E4, H + E - 1, R, E + 1, E4, E,
            G4, E, A4, E, R, E, E4, E, B4, E, A4, E,
            B4, Q, A4, E, G4, E, A4, E, B4, E, R, E,
            E4, H - 1, R, 1, G4, E, FS4, E, R, E, FS4, E,
            R, E, G4, E, A4, E, FS4, E, E4, H + E - 1, R, E + 1,
            E4, E, G4, E, B4, E, D5, W - 2, R, 2, CS5, H - 1,
            R, 1, C5, H - 1, R, 1, G4, E, E4, DQ - 1, R, E + 1,
            E4, E, G4, E, A4, E, AS4, E, B4, E, AS4, E,
            B4, E, A4, E, G4, E, E4, E, D4, E, D5, W - 2,
            R, 2, CS5, H - 1, R, 1, C5, H - 1, R, 1, G4, E,
            E4, DQ - 1, R, E + 1, E4, E, G4, E, A4, E, E5, E,
            B4, DQ, DS5, E, FS5, E, A5, E, B5, E, R, E,
            E4, E, B4, E, A4, E, B4, Q, A4, E, G4, E,
            A4, E, B4, E, R, E, E4, H - 1, R, 1, G4, E,
            FS4, E, R, E, FS4, E, R, E, FS4, E, E4, E,
            D4, E, E4, H + E - 1, R, E + 1, E4, E, G4, E, A4, E
    );
    return t;
  }

  @Override
  public Track getHarmony() {
    Track t = new Track().withDefaults(HARMONY_VOL, HARMONY_DUTY);
    t.addNotes(
            R, H, E3, Q - 1, R, 1, G3, S, R, S, B3, S,
            R, S, D4, DE - 1, R, 1, CS4, DE - 1, R, 1, A3, E,
            C4, DE - 1, R, 1, B3, DE - 1, R, 1, G3, E, A3, S,
            R, S, G3, S, R, S, E3, S, D3, E, E3, S,
            R, E, D4, DE, E4, S, R, E, D4, DE, E4, S,
            R, DQ, D4, DE, E4, S, R, S, G4, S, R, S,
            G4, S, FS4, S, R, S, E4, S, R, S, D4, E,
            E3, Q - 1, R, 1, G3, S, R, S, B3, S, R, S,
            D4, DE - 1, R, 1, CS4, DE - 1, R, 1, A3, E, C4, DE - 1,
            R, 1, B3, DE - 1, R, 1, G3, E, A3, S, R, S,
            G3, S, R, S, E3, S, D3, E, E3, S, R, E,
            D4, DE, E4, S, R, E, D4, DE, E4, S, R, DQ,
            D4, DE, E4, S, R, S, G4, S, R, S, G4, S,
            FS4, S, R, S, E4, S, R, S, D4, E, E3, Q - 1,
            R, 1, G3, S, R, S, B3, S, R, S, D4, DE - 1,
            R, 1, CS4, DE - 1, R, 1, A3, E, C4, DE - 1, R, 1,
            B3, DE - 1, R, 1, G3, E, A3, S, R, S, G3, S,
            R, S, E3, S, D3, E, E3, S, R, E, D4, DE,
            E4, S, R, E, D4, DE, E4, S, R, DQ, D4, DE,
            E4, S, R, S, G4, S, R, S, G4, S, FS4, S,
            R, S, E4, S, R, S, D4, E, E3, Q - 1, R, 1,
            G3, S, R, S, B3, S, R, S, D4, DE - 1, R, 1,
            CS4, DE - 1, R, 1, A3, E, C4, DE - 1, R, 1, B3, DE - 1,
            R, 1, G3, E, A3, S, R, S, G3, S, R, S,
            E3, S, D3, E, E3, S, R, E, D4, DE, E4, S,
            R, E, D4, DE, E4, S, R, Q, FS4, E, R, S,
            FS4, S, R, S, FS4, S, R, S, FS4, S, G4, Q - 1,
            R, Q + 1, G4, S, A4, S, R, S, G4, S, R, S,
            G4, S, A4, S, G4, S, FS4, S, G4, S, R, S,
            FS4, S, R, S, FS4, S, G4, S, FS4, S, D4, S,
            E4, S, R, S, E4, S, R, S, D4, S, E4, E,
            D4, S, E4, S, R, S, E4, S, R, S, D4, S,
            E4, E, G4, S, A4, S, R, S, G4, S, R, S,
            G4, S, A4, S, G4, S, FS4, S, G4, S, R, S,
            FS4, S, R, S, FS4, S, G4, S, FS4, S, D4, S,
            E4, S, R, S, E4, S, R, S, D4, S, E4, E,
            D4, S, E4, S, R, S, E4, S, R, S, D4, S,
            E4, E, G4, S, A4, S, R, S, G4, S, R, S,
            G4, S, A4, S, G4, S, FS4, S, G4, S, R, S,
            FS4, S, R, S, FS4, S, G4, S, FS4, S, D4, S,
            E4, S, R, S, E4, S, R, S, D4, S, E4, E,
            D4, S, E4, S, R, S, E4, S, R, S, D4, S,
            E4, E, G4, S, A4, S, R, S, G4, S, R, S,
            G4, S, A4, S, G4, S, FS4, S, G4, S, R, S,
            FS4, S, R, S, FS4, S, G4, S, FS4, S, D4, S,
            E4, S, R, S, E4, S, R, S, D4, S, E4, E,
            D4, S, E4, S, R, S, E4, S, R, S, D4, S,
            E4, E, B4, W - T, R, T, AS4, H - 2, R, 2, A4, H - 2,
            R, E + 2, G4, E, A4, E, G4, E, B4, E, G4, E,
            R, E, G4, E, A4, E, G4, E, B4, E, G4, E,
            R, E, G4, E, FS4, E, G4, E, B4, W - T, R, T,
            AS4, H - 2, R, 2, A4, H - 2, R, E + 2, G4, E, A4, E,
            G4, E, B4, E, G4, E, R, E, G4, E, A4, E,
            G4, E, B4, E, G4, E, R, E, G4, E, FS4, E,
            G4, DE, A4, S, R, S, G4, S, R, S, G4, S,
            A4, S, G4, S, FS4, S, G4, S, R, S, FS4, S,
            R, S, FS4, S, G4, S, FS4, S, D4, S, E4, S,
            R, S, E4, S, R, S, D4, S, E4, E, D4, S,
            E4, S, R, S, E4, S, R, S, D4, S, E4, E,
            G4, S, A4, S, R, S, G4, S, R, S, G4, S,
            A4, S, G4, S, FS4, S, G4, S, R, S, FS4, S,
            R, S, FS4, S, G4, S, FS4, S, D4, S, E4, S,
            R, S, E4, S, R, S, D4, S, E4, E, D4, S,
            E4, S, R, S, E4, S, R, S, D4, S, E4, E,
            G4, S, A4, S, R, S, G4, S, R, S, G4, S,
            A4, S, G4, S, FS4, S, G4, S, R, S, FS4, S,
            R, S, FS4, S, G4, S, FS4, S, D4, S, E4, S,
            R, S, E4, S, R, S, D4, S, E4, E, D4, S,
            E4, S, R, S, E4, S, R, S, D4, S, E4, E,
            G4, S, A4, S, R, S, G4, S, R, S, G4, S,
            A4, S, G4, S, FS4, S, G4, S, R, S, FS4, S,
            R, S, FS4, S, G4, S, FS4, S, D4, S, E4, S,
            R, S, E4, S, R, S, D4, S, E4, E, D4, S,
            E4, S, R, S, E4, S, R, S, D4, S, E4, E,
            B4, W - T, R, T, AS4, H - 2, R, 2, A4, H - 2, R, E + 2,
            G4, E, A4, E, G4, E, B4, E, G4, E, R, E,
            G4, E, A4, E, G4, E, B4, E, G4, E, R, E,
            G4, E, FS4, E, G4, E, B4, W - T, R, T, AS4, H - 2,
            R, 2, A4, H - 2, R, E + 2, G4, E, A4, E, G4, E,
            B4, E, G4, E, R, E, G4, E, A4, E, G4, E,
            B4, E, G4, E, R, E, G4, E, FS4, E, G4, DE,
            A4, S, R, S, G4, S, R, S, G4, S, A4, S,
            G4, S, FS4, S, G4, S, R, S, FS4, S, R, S,
            FS4, S, G4, S, FS4, S, D4, S, E4, S, R, S,
            E4, S, R, S, D4, S, E4, E, D4, S, E4, S,
            R, S, E4, S, R, S, D4, S, E4, E, G4, S,
            A4, S, R, S, G4, S, R, S, G4, S, A4, S,
            G4, S, FS4, S, G4, S, R, S, FS4, S, R, S,
            FS4, S, G4, S, FS4, S, D4, S, E4, S, R, S,
            E4, S, R, S, D4, S, E4, E, D4, S, E4, S,
            R, S, E4, S, R, S, D4, S, E4, E
    );
    return t;
  }

  @Override
  public Track getBass() {
    Track t = new Track().withDefaults(BASS_VOL, BASS_DUTY);
    // Form, in 96-frame bars (5040 frames total, same as the other voices —
    // the synth loops each track independently, so all four totals MUST
    // stay equal or the song drifts apart on the second loop):
    //   half-bar pickup + [run 2 + pedal 2] x3 + [run 2 + turnaround 2]
    //   + chorus 8 + bridge 8 + chorus 8 + bridge 8 + chorus 4
    t.addNotes(R, H);              // half-bar pickup, shared with the harmony
    for (int i = 0; i < 3; i++) {
      addIntroRun(t);
      addIntroPedal(t);
    }
    addIntroRun(t);
    addTurnaround(t);
    addChorus(t, 2);               // [Em Em D Em] x2
    addBridge(t);
    addBridge(t);
    addChorus(t, 2);
    addBridge(t);
    addBridge(t);
    addChorus(t, 1);               // half-length final chorus, loops clean
    return t;
  }

  // Bars 1-2 of the intro: the unison run, an octave below the pulses. Kept
  // from the MIDI, micro-gaps and all — the run was never the problem.
  private static void addIntroRun(Track t) {
    t.addNotes(
            E2, Q - 1, R, 1, G2, S, R, S, B2, S, R, S,
            D3, DE - 1, R, 1, CS3, DE - 1, R, 1, A2, E,
            C3, DE - 1, R, 1, B2, DE - 1, R, 1, G2, E,
            A2, S, R, S, G2, S, R, S, E2, S, D2, E, E2, S
    );
  }

  // Bars 3-4 of the intro. The source MIDI padded this out to quarter-note
  // pulses; the NES original hammers straight eighth-note E2s here, and that
  // relentless pedal is half the song's drive. Each eighth is a 9-frame note
  // plus a 3-frame gap (DS + T = E): the triangle has no volume envelope, so
  // the gap is the only thing that re-articulates a repeated pitch.
  private static void addIntroPedal(Track t) {
    t.addNotes(
            R, E,
            E2, DS, R, T, E2, DS, R, T, E2, DS, R, T, E2, DS, R, T,
            E2, DS, R, T, E2, DS, R, T, E2, DS, R, T, E2, DS, R, T,
            E2, E, FS2, S, R, S, FS2, E,
            G2, S, R, S, B2, S, R, S, D2, Q - 1, R, 1
    );
  }

  // Fourth-time ending of the intro: shortened pedal, the D-major push, and
  // the E-DS-D-CS-C chromatic drop that tips into the chorus (kept verbatim).
  private static void addTurnaround(Track t) {
    t.addNotes(
            R, E,
            E2, DS, R, T, E2, DS, R, T, E2, DS, R, T,
            E2, DS, R, T, E2, DS, R, T, E2, DS, R, T,
            E2, E,
            D2, E, R, S, D2, S, R, S, D2, S, R, S, D2, S,
            E2, S, R, E, E2, S, DS2, S, D2, S, CS2, S, C2, S
    );
  }

  // One chorus bar of the original's funk cell (per the Gametabs bass tab):
  // root on 1 and the "and" of 1, a push on the "and" of 2, then root on 3
  // launching a 16th climb 3rd-5th-3rd-root through beat 4. The MIDI kept
  // only the climb plus one long root per bar — no pulse to nod along to.
  private static void addGrooveBar(Track t, int root, int third, int fifth) {
    t.addNotes(
            root, DS, R, T, root, E, R, E, root, DS, R, T,
            root, S, third, DS, R, T, fifth, DS, R, T, third, S,
            root, DS, R, T
    );
  }

  // 4-bar chorus phrase: | Em | Em | D | Em |, repeated `times` times.
  private static void addChorus(Track t, int times) {
    for (int i = 0; i < times; i++) {
      addGrooveBar(t, E2, G2, B2);
      addGrooveBar(t, E2, G2, B2);
      addGrooveBar(t, D2, FS2, A2);
      addGrooveBar(t, E2, G2, B2);
    }
  }

  // Bridge cell (4 bars): the G - FS - F syncopated stabs, the E-FS-E-G walk,
  // and the stop-time bar under the lead's ascending run. Kept verbatim from
  // the MIDI — the stabs and the dead stop are the original's drama, and the
  // contrast is what makes the chorus hit on the way back in.
  private static void addBridge(Track t) {
    t.addNotes(
            G2, S, R, S, G2, DE, R, S, G2, DE, R, S, G2, DE, R, S, G2, E,
            FS2, S, R, S, FS2, DE, R, S, FS2, E,
            F2, S, R, S, F2, DE, R, S, F2, E,
            R, E, E2, E, FS2, E, E2, E, G2, E, E2, E, R, E, E2, E,
            R, W
    );
  }

  /* Drum velocity layers. The kick has its own dedicated voice and ignores
     note volume, so only the noise-bus voices need balancing. */
  private static final double HIHAT_VOL = 0.30;
  private static final double SNARE_VOL = 0.85;
  private static final double CYMBAL_VOL = 0.55;

  @Override
  public Track getDrums() {
    Track t = new Track().withDefaults(DRUM_VOL, DRUM_DUTY);
    t.addNotes(
            R, H, KICK, T, R, T, HIHAT, T, R, T, HIHAT, T,
            R, T, HIHAT, T, R, T, SNARE, T, R, T, HIHAT, T,
            R, T, KICK, T, R, T, HIHAT, T, R, T, KICK, T,
            R, T, HIHAT, T, R, T, HIHAT, T, R, T, KICK, T,
            R, T, SNARE, T, R, T, HIHAT, T, R, T, KICK, T,
            R, T, HIHAT, T, R, T, HIHAT, T, R, T, HIHAT, T,
            R, T, KICK, T, R, T, KICK, T, R, T, SNARE, T,
            R, T, HIHAT, T, R, T, KICK, T, R, T, HIHAT, T,
            R, T, HIHAT, T, R, T, HIHAT, T, R, T, KICK, T,
            R, T, HIHAT, T, R, T, SNARE, T, R, T, KICK, T,
            R, T, HIHAT, T, R, T, KICK, T, R, DS, KICK, T,
            R, T, KICK, T, R, T, HIHAT, T, R, T, SNARE, T,
            R, T, HIHAT, T, R, T, HIHAT, T, R, T, HIHAT, T,
            R, T, HIHAT, T, R, T, HIHAT, T, R, T, KICK, T,
            R, T, KICK, T, R, T, SNARE, T, R, T, HIHAT, T,
            R, T, KICK, T, R, T, HIHAT, T, R, T, KICK, T,
            R, T, HIHAT, T, R, T, KICK, T, R, T, HIHAT, T,
            R, T, SNARE, T, R, T, HIHAT, T, R, T, HIHAT, T,
            R, T, HIHAT, T, R, T, HIHAT, T, R, T, SNARE, T,
            R, T, KICK, T, R, T, HIHAT, T, R, T, SNARE, T,
            R, T, HIHAT, T, R, T, HIHAT, T, R, T, HIHAT, T,
            R, T, KICK, T, R, T, HIHAT, T, R, T, HIHAT, T,
            R, T, HIHAT, T, R, T, SNARE, T, R, T, HIHAT, T,
            R, T, KICK, T, R, T, HIHAT, T, R, T, KICK, T,
            R, T, HIHAT, T, R, T, HIHAT, T, R, T, KICK, T,
            R, T, SNARE, T, R, T, HIHAT, T, R, T, KICK, T,
            R, T, HIHAT, T, R, T, HIHAT, T, R, T, HIHAT, T,
            R, T, KICK, T, R, T, KICK, T, R, T, SNARE, T,
            R, T, HIHAT, T, R, T, KICK, T, R, T, HIHAT, T,
            R, T, HIHAT, T, R, T, HIHAT, T, R, T, KICK, T,
            R, T, HIHAT, T, R, T, SNARE, T, R, T, KICK, T,
            R, T, HIHAT, T, R, T, KICK, T, R, DS, KICK, T,
            R, T, KICK, T, R, T, HIHAT, T, R, T, SNARE, T,
            R, T, HIHAT, T, R, T, HIHAT, T, R, T, HIHAT, T,
            R, T, HIHAT, T, R, T, HIHAT, T, R, T, KICK, T,
            R, T, KICK, T, R, T, SNARE, T, R, T, HIHAT, T,
            R, T, KICK, T, R, T, HIHAT, T, R, T, KICK, T,
            R, T, HIHAT, T, R, T, KICK, T, R, T, HIHAT, T,
            R, T, SNARE, T, R, T, HIHAT, T, R, T, HIHAT, T,
            R, T, HIHAT, T, R, T, HIHAT, T, R, T, SNARE, T,
            R, T, KICK, T, R, T, HIHAT, T, R, T, SNARE, T,
            R, T, HIHAT, T, R, T, HIHAT, T, R, T, HIHAT, T,
            R, T, KICK, T, R, T, HIHAT, T, R, T, HIHAT, T,
            R, T, HIHAT, T, R, T, SNARE, T, R, T, HIHAT, T,
            R, T, KICK, T, R, T, HIHAT, T, R, T, KICK, T,
            R, T, HIHAT, T, R, T, HIHAT, T, R, T, KICK, T,
            R, T, SNARE, T, R, T, HIHAT, T, R, T, KICK, T,
            R, T, HIHAT, T, R, T, HIHAT, T, R, T, HIHAT, T,
            R, T, KICK, T, R, T, KICK, T, R, T, SNARE, T,
            R, T, HIHAT, T, R, T, KICK, T, R, T, HIHAT, T,
            R, T, HIHAT, T, R, T, HIHAT, T, R, T, KICK, T,
            R, T, HIHAT, T, R, T, SNARE, T, R, T, KICK, T,
            R, T, HIHAT, T, R, T, KICK, T, R, DS, KICK, T,
            R, T, KICK, T, R, T, HIHAT, T, R, T, SNARE, T,
            R, T, HIHAT, T, R, T, HIHAT, T, R, T, HIHAT, T,
            R, T, HIHAT, T, R, T, HIHAT, T, R, T, KICK, T,
            R, T, KICK, T, R, T, SNARE, T, R, T, HIHAT, T,
            R, T, KICK, T, R, T, HIHAT, T, R, T, KICK, T,
            R, T, HIHAT, T, R, T, KICK, T, R, T, HIHAT, T,
            R, T, SNARE, T, R, T, HIHAT, T, R, T, HIHAT, T,
            R, T, HIHAT, T, R, T, HIHAT, T, R, T, SNARE, T,
            R, T, KICK, T, R, T, HIHAT, T, R, T, SNARE, T,
            R, T, HIHAT, T, R, T, HIHAT, T, R, T, HIHAT, T,
            R, T, KICK, T, R, T, HIHAT, T, R, T, HIHAT, T,
            R, T, HIHAT, T, R, T, SNARE, T, R, T, HIHAT, T,
            R, T, KICK, T, R, T, HIHAT, T, R, T, KICK, T,
            R, T, HIHAT, T, R, T, HIHAT, T, R, T, KICK, T,
            R, T, SNARE, T, R, T, HIHAT, T, R, T, KICK, T,
            R, T, HIHAT, T, R, T, HIHAT, T, R, T, HIHAT, T,
            R, T, KICK, T, R, T, KICK, T, R, T, SNARE, T,
            R, T, HIHAT, T, R, T, KICK, T, R, T, HIHAT, T,
            R, T, HIHAT, T, R, T, HIHAT, T, R, T, KICK, T,
            R, T, HIHAT, T, R, T, SNARE, T, R, T, KICK, T,
            R, T, HIHAT, T, R, T, KICK, T, R, DS, KICK, T,
            R, T, KICK, T, R, T, HIHAT, T, R, T, SNARE, T,
            R, T, HIHAT, T, R, T, HIHAT, T, R, T, HIHAT, T,
            R, T, HIHAT, T, R, T, HIHAT, T, R, T, KICK, T,
            R, T, KICK, T, R, T, SNARE, T, R, T, HIHAT, T,
            R, T, KICK, T, R, T, HIHAT, T, R, T, SNARE, T,
            R, T, SNARE, T, R, DS, SNARE, T, R, DS, SNARE, T,
            R, DS, SNARE, T, R, T, SNARE, T, R, E + T, KICK, T,
            R, T, SNARE, T, R, T, SNARE, T, R, T, SNARE, T,
            R, T, SNARE, T, R, T, KICK, T, R, Q - T, SNARE, T,
            R, E + T, HIHAT, T, R, T, HIHAT, T, R, T, HIHAT, T,
            R, T, KICK, T, R, DS, SNARE, T, R, DS, KICK, T,
            R, DS, KICK, T, R, Q - T, SNARE, T, R, E + T, HIHAT, T,
            R, T, HIHAT, T, R, T, HIHAT, T, R, T, KICK, T,
            R, DS, SNARE, T, R, DS, KICK, T, R, DS, KICK, T,
            R, Q - T, SNARE, T, R, E + T, HIHAT, T, R, T, HIHAT, T,
            R, T, HIHAT, T, R, T, KICK, T, R, DS, SNARE, T,
            R, DS, KICK, T, R, DS, KICK, T, R, Q - T, SNARE, T,
            R, E + T, HIHAT, T, R, T, HIHAT, T, R, T, HIHAT, T,
            R, T, KICK, T, R, DS, SNARE, T, R, DS, KICK, T,
            R, DS, KICK, T, R, Q - T, SNARE, T, R, E + T, HIHAT, T,
            R, T, HIHAT, T, R, T, HIHAT, T, R, T, KICK, T,
            R, DS, SNARE, T, R, DS, KICK, T, R, DS, KICK, T,
            R, Q - T, SNARE, T, R, E + T, HIHAT, T, R, T, HIHAT, T,
            R, T, HIHAT, T, R, T, KICK, T, R, DS, SNARE, T,
            R, DS, KICK, T, R, DS, KICK, T, R, Q - T, SNARE, T,
            R, E + T, HIHAT, T, R, T, HIHAT, T, R, T, HIHAT, T,
            R, T, KICK, T, R, DS, SNARE, T, R, DS, KICK, T,
            R, DS, KICK, T, R, Q - T, SNARE, T, R, E + T, HIHAT, T,
            R, T, HIHAT, T, R, T, HIHAT, T, R, T, KICK, T,
            R, DS, SNARE, T, R, DS, SNARE, T, R, T, SNARE, T,
            R, T, KICK, T, R, DS, KICK, T, R, DS, SNARE, T,
            R, DS, CYMBAL, T, R, DS, KICK, T, R, DS, KICK, T,
            R, DS, SNARE, T, R, DS, CYMBAL, T, R, DS, KICK, T,
            R, DS, KICK, T, R, DS, SNARE, T, R, DS, CYMBAL, T,
            R, DS, KICK, T, R, DS, KICK, T, R, DS, SNARE, T,
            R, DS, CYMBAL, T, R, DS, CYMBAL, T, R, DS, KICK, T,
            R, DS, SNARE, T, R, DS, KICK, T, R, DS, KICK, T,
            R, DS, KICK, T, R, DS, SNARE, T, R, DS, KICK, T,
            R, DS, SNARE, T, R, DS, KICK, T, R, DS, SNARE, T,
            R, DS, KICK, T, R, DS, SNARE, T, R, DS, KICK, T,
            R, DS, SNARE, T, R, DS, SNARE, T, R, T, SNARE, T,
            R, T, KICK, T, R, DS, KICK, T, R, DS, SNARE, T,
            R, DS, CYMBAL, T, R, DS, KICK, T, R, DS, KICK, T,
            R, DS, SNARE, T, R, DS, CYMBAL, T, R, DS, KICK, T,
            R, DS, KICK, T, R, DS, SNARE, T, R, DS, CYMBAL, T,
            R, DS, KICK, T, R, DS, KICK, T, R, DS, SNARE, T,
            R, DS, CYMBAL, T, R, DS, CYMBAL, T, R, DS, KICK, T,
            R, DS, SNARE, T, R, DS, KICK, T, R, DS, KICK, T,
            R, DS, KICK, T, R, DS, SNARE, T, R, DS, KICK, T,
            R, DS, SNARE, T, R, DS, KICK, T, R, DS, SNARE, T,
            R, DS, KICK, T, R, DS, SNARE, T, R, DS, KICK, T,
            R, DS, SNARE, T, R, DS, SNARE, T, R, T, SNARE, T,
            R, T, KICK, T, R, Q - T, SNARE, T, R, E + T, HIHAT, T,
            R, T, HIHAT, T, R, T, HIHAT, T, R, T, KICK, T,
            R, DS, SNARE, T, R, DS, KICK, T, R, DS, KICK, T,
            R, Q - T, SNARE, T, R, E + T, HIHAT, T, R, T, HIHAT, T,
            R, T, HIHAT, T, R, T, KICK, T, R, DS, SNARE, T,
            R, DS, KICK, T, R, DS, KICK, T, R, Q - T, SNARE, T,
            R, E + T, HIHAT, T, R, T, HIHAT, T, R, T, HIHAT, T,
            R, T, KICK, T, R, DS, SNARE, T, R, DS, KICK, T,
            R, DS, KICK, T, R, Q - T, SNARE, T, R, E + T, HIHAT, T,
            R, T, HIHAT, T, R, T, HIHAT, T, R, T, KICK, T,
            R, DS, SNARE, T, R, DS, KICK, T, R, DS, KICK, T,
            R, Q - T, SNARE, T, R, E + T, HIHAT, T, R, T, HIHAT, T,
            R, T, HIHAT, T, R, T, KICK, T, R, DS, SNARE, T,
            R, DS, KICK, T, R, DS, KICK, T, R, Q - T, SNARE, T,
            R, E + T, HIHAT, T, R, T, HIHAT, T, R, T, HIHAT, T,
            R, T, KICK, T, R, DS, SNARE, T, R, DS, KICK, T,
            R, DS, KICK, T, R, Q - T, SNARE, T, R, E + T, HIHAT, T,
            R, T, HIHAT, T, R, T, HIHAT, T, R, T, KICK, T,
            R, DS, SNARE, T, R, DS, KICK, T, R, DS, KICK, T,
            R, Q - T, SNARE, T, R, E + T, HIHAT, T, R, T, HIHAT, T,
            R, T, HIHAT, T, R, T, KICK, T, R, DS, SNARE, T,
            R, DS, SNARE, T, R, T, SNARE, T, R, T, KICK, T,
            R, DS, KICK, T, R, DS, SNARE, T, R, DS, CYMBAL, T,
            R, DS, KICK, T, R, DS, KICK, T, R, DS, SNARE, T,
            R, DS, CYMBAL, T, R, DS, KICK, T, R, DS, KICK, T,
            R, DS, SNARE, T, R, DS, CYMBAL, T, R, DS, KICK, T,
            R, DS, KICK, T, R, DS, SNARE, T, R, DS, CYMBAL, T,
            R, DS, CYMBAL, T, R, DS, KICK, T, R, DS, SNARE, T,
            R, DS, KICK, T, R, DS, KICK, T, R, DS, KICK, T,
            R, DS, SNARE, T, R, DS, KICK, T, R, DS, SNARE, T,
            R, DS, KICK, T, R, DS, SNARE, T, R, DS, KICK, T,
            R, DS, SNARE, T, R, DS, KICK, T, R, DS, SNARE, T,
            R, DS, SNARE, T, R, T, SNARE, T, R, T, KICK, T,
            R, DS, KICK, T, R, DS, SNARE, T, R, DS, CYMBAL, T,
            R, DS, KICK, T, R, DS, KICK, T, R, DS, SNARE, T,
            R, DS, CYMBAL, T, R, DS, KICK, T, R, DS, KICK, T,
            R, DS, SNARE, T, R, DS, CYMBAL, T, R, DS, KICK, T,
            R, DS, KICK, T, R, DS, SNARE, T, R, DS, CYMBAL, T,
            R, DS, CYMBAL, T, R, DS, KICK, T, R, DS, SNARE, T,
            R, DS, KICK, T, R, DS, KICK, T, R, DS, KICK, T,
            R, DS, SNARE, T, R, DS, KICK, T, R, DS, SNARE, T,
            R, DS, KICK, T, R, DS, SNARE, T, R, DS, KICK, T,
            R, DS, SNARE, T, R, DS, KICK, T, R, DS, SNARE, T,
            R, DS, SNARE, T, R, T, SNARE, T, R, T, KICK, T,
            R, Q - T, SNARE, T, R, E + T, HIHAT, T, R, T, HIHAT, T,
            R, T, HIHAT, T, R, T, KICK, T, R, DS, SNARE, T,
            R, DS, KICK, T, R, DS, KICK, T, R, Q - T, SNARE, T,
            R, E + T, HIHAT, T, R, T, HIHAT, T, R, T, HIHAT, T,
            R, T, KICK, T, R, DS, SNARE, T, R, DS, KICK, T,
            R, DS, KICK, T, R, Q - T, SNARE, T, R, E + T, HIHAT, T,
            R, T, HIHAT, T, R, T, HIHAT, T, R, T, KICK, T,
            R, DS, SNARE, T, R, DS, KICK, T, R, DS, KICK, T,
            R, Q - T, SNARE, T, R, E + T, HIHAT, T, R, T, HIHAT, T,
            R, T, HIHAT, T, R, T, KICK, T, R, DS, SNARE, T,
            R, DS, KICK, T, R, DS
    );
    // Velocity pass: the MIDI conversion left every hit at the flat
    // DRUM_VOL, which put the hi-hats at snare volume and buried the
    // backbeat under 16th-note ticking. Same idea as SurfCity's and
    // HyruleTemple's withVolume layers, applied as a post-pass so the
    // pattern data above stays untouched. Note's fields are final, so each
    // adjusted hit is rebuilt rather than mutated.
    for (int i = 0; i < t.notes.size(); i++) {
      Note n = t.notes.get(i);
      double v = n.midi == HIHAT ? HIHAT_VOL
               : n.midi == SNARE ? SNARE_VOL
               : n.midi == CYMBAL ? CYMBAL_VOL
               : n.volume;
      if (v != n.volume) {
        t.notes.set(i, new Note(n.midi, n.durationFrames, v, n.duty, n.decay, n.fx));
      }
    }
    return t;
  }
}
