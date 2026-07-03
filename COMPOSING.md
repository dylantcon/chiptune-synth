# Composing for ChiptuneSynth

An API reference and field guide for writing songs against this synth,
distilled from building HyruleTempleSong and overhauling SurfCitySong.

## Architecture in one breath

Four channels model the NES 2A03: **pulse 1** (lead), **pulse 2**
(harmony), **triangle** (bass; no volume envelope, glorious lows), and
**noise** (drums — special pitches route to dedicated kick/snare/tom
voices). Everything runs on a 60 Hz frame clock, exactly like the NES NMI
music drivers. A `Note` is a self-contained event (pitch, duration in
frames, volume, duty, decay, effects — all baked in at `addNotes` time).
A `Track` is a monophonic sequence of Notes. A `ChiptuneSong` supplies
four Tracks plus a tempo correction.

**The one invariant that matters:** the synth loops each track
independently, so all four tracks MUST sum to the same `totalFrames()` or
the song shears apart on the second loop. Build sections as whole bars and
alignment holds by construction.

## Time

The house grid is 150 BPM: a bar is 96 frames, `Q`=24, `E`=12, `S`=6,
`T`=3, plus dotted variants (`DQ`, `DE`, `DS`) and `SX` (16, one sixth of
a bar). Other tempos come from `getTempoScale()`: write on the house grid,
then scale — e.g. Surf City plays at 121.4 BPM via `0.8096` (= 121.4/150).
Other meters are fine too: nothing enforces 96 — a 3/4 song just uses
72-frame bars.

Frames are the only truthful unit. Seconds depend on `speed × tempoScale`
(synth state, changeable live), so they are computed only at the display
boundary (`ChiptuneSynth.getDurationSeconds()`), never stored on a Track.

Swing/shuffle needs no machinery: a swung eighth pair is just uneven
frames (16+8 instead of 12+12).

## Pitch and drums

Pitch constants are MIDI numbers named `C1`–`G6` (octave 5 = main melody
range, octave 2 = deep triangle bass). `R` = rest — and a rest is the only
true note-off; decay envelopes fade but rarely silence.

On the noise track, four selector pitches route to voices: `KICK` (sine
sweep 150→40 Hz — the sub-thud the noise channel physically can't make),
`SNARE` (layered crack + sizzle), `HIHAT` (tight broadband tick),
`CYMBAL` (long wash). **Any other pitch becomes a tuned tom** — fills are
just notes: `t.addNotes(G3, 3, R, 9, DS3, 3, ...)`. All three drum voices
have a `CRUSH` constant (sample-and-hold at ~11 kHz + coarse
quantization) for the DPCM sampled-drum grit; set `CRUSH = 1` to A/B the
clean sound.

## Track API — what each knob is *for*

Builder state applies to every subsequent `addNotes` until changed, so
songs read top to bottom.

| Method | Use it for |
|---|---|
| `withDefaults(vol, duty)` | Voice setup, once per track. `LEAD_VOL/LEAD_DUTY` etc. are the house mix. |
| `withVolume(v)` | Accents and dynamics: drum velocity layers, snare-roll crescendos, terracing a riff louder as a build-up grows (Surf City chug: 0.60 → full at the halfway mark). |
| `withDuty(d)` | Timbre. 0.125 thin/nasal, ~0.2 classic lead, 0.35 fat, 0.5 hollow/clarinet. The era's "second verse, bigger instrument" trick is a duty switch on the repeat (Surf City pass 2: 0.205 → 0.35). Thin duty makes an echo read as a reflection, not a double. |
| `withDecay(d)` | Articulation: `LEGATO` default, `SUSTAINED` holds flat (pair with swell), `GENTLE_FADE` pad-like, `STACCATO`/`PORTATO` percussive. For hard-cut stabs use short-note-plus-rest instead (see Lessons). |
| `withTranspose(semis)` | Fixing a rip transcribed in the wrong octave without touching note data. |
| `withArp(0,3,7)` + `withArpSpeed(n)` | The NES fake chord: one mono channel pretends to be a triad. `(0,3,7)` minor, `(0,4,7)` major; speed 1 shimmers, 2–3 chunk. Great for off-beat stabs. |
| `withVibrato(depth, hz, delayFrames)` | Makes held notes sing. The delay is the secret: 8–14 frames keeps fast runs razor-clean while only sustained targets pick up the wobble. |
| `withSlide(frames)` | Portamento from the previous pitch — bass swoops (Surf City's coda glides C2→Bb1→Ab1), expressive lead smears. Works across a rest. |
| `withPitchEnv(startSemis, frames)` | One-shot scoop (negative start) or rise (positive) into a note — zaps, drum sweeps, attitude. |
| `withSwell()` | Parabolic amplitude bloom across the note — what makes a pad a pad instead of a telephone tone. Pair with `SUSTAINED`. |
| `withNoFx()` | Drop all pitch modulation before a clean passage. |
| `addNotes(p, d, p, d, ...)` | The note data itself, alternating pitch/duration pairs. |
| `addNotes(track)` | Composition: build sections as functions returning Tracks, then chain them — the song's form becomes readable code. |
| `Track.echoOf(src, delay, volScale, duty)` | The David Wise echo: pulse 2 shadows the lead 2–3 sixteenths behind, ~half volume, thin duty. One melody sounds like a produced record. Result is exactly source-length, so alignment survives. |
| `totalFrames()` | The alignment check. Assert all four tracks equal (see FrameCheck pattern). |
| `seek(frame)` | State-chase seek: the note spanning the target re-triggers mid-flight with effects intact, so a sustain keeps sounding instead of dropping to silence. |

## Synth-level

- `speed` (gameplay knob) × `tempoScale` (per-song correction) multiply;
  Track frames never change meaning.
- `seek(fraction)` — the dev "pan" control; chases all four tracks.
- `addListener(ChiptuneSynthListener)` — playback-position callbacks
  grounded on the **audio device's sample clock** (`SongChronometer`), so
  the reported frame is what the ears hear, not where the sequencer has
  rendered ahead to. All integer accounting; no accumulated float error.

## Lessons learned (the house style)

1. **Sections are functions, form is the composition expression.**
   `getIntro()`, `getVerse()`... chained in `getLead()`. The song's shape
   is readable without playing a note.
2. **Repetition is a loop; variation is a conditional.** Write the *rule*
   ("downbeat walks D→D#→E by bar number"), not four copies. Development
   on repeats then costs one line.
3. **The harmonic plan is data.** Per-bar chord arrays + small builders
   (`bassFrom`, `harmonyFrom`) mean a chord change is a one-array edit.
4. **Whole bars everywhere.** Equal channel totals by construction; check
   with `totalFrames()`.
5. **Stabs are note+rest, not decay.** The punchy rip feel is ~5 frames
   of tone in a 12-frame slot (`p,5,R,7`) — a rest is the only true cut.
6. **Develop the repeat.** Echo enters, duty fattens, bass octave-pumps,
   drums stack, riff lifts an octave. Two passes of the same music with
   pass 2 "going harder" beats four identical passes.
7. **Lopsided phrasing beats grid symmetry.** Surf City's call/answer
   rests are deliberately ~2.4:1 — even the breaths are composed. Don't
   "fix" asymmetry that makes the beat lively.
8. **The bass is the engine of a build-up.** Drums stack the excitement,
   but relentless eighth-note bass with octave pops is what drives.
9. **MIDI rips are banned.** MIDI cannot represent duty, arps, slides, or
   noise modes — the engineering is stripped out. Use FamiTracker text
   exports (map ~1:1 onto this API) with NSF playback as ground truth.
10. **No fake precision.** Display decimals at or below measurement
    resolution; lead with the exact native unit (frames).

## The pipeline (next step)

1. Get the **NES-cartridge** NSF (not the FDS rip — the Famicom Disk
   System version uses the FDS expansion channel this synth doesn't
   model). Check: FamiTracker's Module Properties should say expansion
   "None". NTSC, not PAL.
2. NSFImport: import, select the track, capture a bit over one loop,
   save the module.
3. FamiTracker: File → Export text… → e.g. `zelda-dungeon.txt`.
4. `java -cp build\classes chiptunesynth.tools.FamiTrackerTextConverter
   zelda-dungeon.txt src\chiptunesynth\songs\ZeldaDungeonSong.java
   ZeldaDungeonSong`
5. The output is a compilable skeleton: one method per pattern, getters
   chaining the ORDER list (the tracker's structure arrives pre-factored).
   Then the human part: trim to exact bars, act on the effect comments,
   set voices/decay, remap noise buckets by ear, hand-map DPCM comments
   onto the drum voices, and factor into named sections.
