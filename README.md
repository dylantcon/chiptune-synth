# ChiptuneSynth

*A small NES-style chiptune synthesizer for Java, modeling the Ricoh 2A03 APU at a high level. Originally written as the music engine for [Javarominoes](https://github.com/dylantcon/javarominoes), now split out as a standalone library.*

## Overview

ChiptuneSynth began as a single question: could I make Java's `javax.sound.sampled` produce the sound of the [Ricoh 2A03](https://en.wikipedia.org/wiki/Ricoh_2A03), the audio processing unit inside the Nintendo Entertainment System, without any native code or external dependencies? The answer turned out to be yes, and the pursuit of that answer taught me more about digital signal processing, real-time audio, and the craft of 1980s game composers than I ever expected going in.

The library models the 2A03 the way the console's own music drivers saw it: two pulse (square) channels, one triangle channel, one noise channel, all driven by a sequencer ticking at approximately 60 Hz. On real hardware, that tick came from the NMI handler firing once per video frame, so note durations were measured in frames, or 60ths of a second. I kept that unit exactly. A quarter note at 150 BPM is 24 frames, and every duration constant in the library is derived from that arithmetic. When you write a song for this synth, you are writing in the same temporal vocabulary that Koji Kondo and David Wise wrote in.

Output is 44.1 kHz, 16-bit stereo. The synth mixes a single mono signal and duplicates it to both channels; not every audio backend will hand out a mono `SourceDataLine`, and rendering mono then writing it twice keeps one code path working everywhere.

## Features

- Two pulse (square) channels with selectable duty cycles and band-limited (polyBLEP) edges
- One triangle channel with a quantized, stair-stepped waveform
- One noise channel driven by a 15-bit linear feedback shift register, with long and short modes
- Dedicated kick, snare, and tom percussion voices that punch through the mix
- An immutable, composable per-note effect system: arpeggio, vibrato, portamento, pitch envelopes, and amplitude swells
- A frame-rate sequencer driving all channels at ~60 Hz, with note durations in frames
- Runtime volume (with slew) and playback-speed control, plus sample-accurate position reporting and seeking
- A FamiTracker text-export converter that emits compilable song skeletons
- A standalone Swing player for auditioning the repertoire

## Usage

Implement `ChiptuneSong` to describe a song as four `Track`s (lead, harmony, bass, drums) built from `Note`s, then hand it to the synth:

```java
ChiptuneSynth synth = ChiptuneSynth.getSynthesizer(new MySong()); // looping
synth.start();                  // plays on a daemon thread
synth.setVolume(0.8);           // 0.0 - 1.0, log-scaled with smooth slew
synth.setSpeed(1.25);           // tempo multiplier, e.g. for level scaling
synth.stop();
```

`ChiptuneSong` also provides the vocabulary for writing songs: note-length constants (`Q`, `E`, `DQ`, ...), MIDI pitch constants (`C4`, `FS5`, ...), drum selectors (`KICK`, `SNARE`, `HIHAT`, `CYMBAL`), and the rest marker `R`.

## How It Works

**The Frame Sequencer**: The heart of the synth is a loop that alternates between two jobs: advance the music by some number of frames, then render exactly one frame's worth of audio ($44100 / 60 = 735$ samples) and write it to the line. Tempo scaling falls out of this structure almost for free. Each render pass adds the product of two multipliers to a fractional accumulator, and the sequencer consumes whole frames from it. The first multiplier is `speed`, the gameplay knob: a host game can ramp it with difficulty, or drop it for a dramatic game-over slowdown. The second is `tempoScale`, a per-song correction for tracks whose frame data was transcribed from a MIDI with an imperfect frames-per-second assumption. Keeping them separate means restarting the music or scaling the game speed can never wipe out a song's tuning correction. At `speed = 1.0` the accumulator gains exactly 1.0 per pass and the synth behaves like the original hardware; at 1.25 it occasionally consumes two frames in one pass, and the music genuinely plays faster rather than merely pitch-shifting.

**The Pulse Channels**: A square wave is conceptually the simplest oscillator you can write: compare the phase against the duty cycle, output +1 or -1. The duty cycle controls timbre. 12.5% sounds nasal and thin (the Mario coin sound), 25% is the classic NES lead, and 50% is fuller and more clarinet-like. My first implementation did exactly the naive comparison, and it sounded *wrong* in a way that took real effort to diagnose. A mathematically perfect square wave has harmonics extending past the Nyquist frequency, and in a discrete-time system those fold back down as inharmonic aliases. On most material the artifact hides, but the Follin brothers' Silver Surfer title theme runs cascades with fundamentals in the 4 to 6 kHz range, and up there the folded garbage reads as a shrill "telephone" ring that the actual hardware (a 1.79 MHz clock feeding an analog output path) never produces. The fix is [polyBLEP](https://www.kvraudio.com/forum/viewtopic.php?t=375517): smooth each edge of the square over a single sample using a polynomial band-limited step residual. Two small corrections per period, and the aliasing collapses. It is a satisfying instance of a fix whose cost is two conditionals and whose effect is immediately audible.

**The Triangle Channel**: Real 2A03 hardware generates its triangle by stepping through 32 discrete levels, and that staircase is part of the console's distinctive bass character. I approximate it by generating a continuous triangle and quantizing with `Math.round(tri * 7) / 7.0`. A perfectly smooth triangle sounds like a soft sine cousin; the quantized one has a faint grit that reads unmistakably as *NES bass*. Notably, the hardware triangle has no volume control at all, only on or off, and I kept that constraint too. The channel's amplitude lives entirely in the mix weights.

**The Noise Channel**: This is the most faithful component in the library, because the original design is already so elegant that there was nothing to improve. The 2A03 generates noise with a 15-bit [linear feedback shift register](https://en.wikipedia.org/wiki/Linear-feedback_shift_register): the feedback bit is the XOR of bits 0 and 1, shifted in at the top, with the output taken from bit 0. In long mode this yields broadband noise with a period of 32,767 steps. In short mode the XOR taps bits 0 and 6 instead, collapsing the period to just 93 samples, which the ear no longer perceives as noise but as a metallic, pitched tone. A fun fact about this register: the original NES Tetris used the exact same LFSR construct for random piece generation, with no piece buffer to smooth it out, which is why that game was infamous for its long I-piece droughts. The same mathematics that gives the NES its percussion also starved its players of line clears.

**The Percussion Voices**: Early versions of the synth routed all drums through the noise channel, exactly as the hardware forces you to, and the drums drowned. A noise burst sharing a 0.18 mix weight with three melodic channels simply cannot anchor a groove. Listening closely to the original recordings clarified why: NES kick drums (often DPCM samples, which I do not emulate) carry a serious sub-150 Hz thud, on the order of a quarter of the total signal energy. So I broke with strict authenticity and gave the kick, snare, and tom their own dedicated voices, each a small layered synthesis (tone plus noise, with fast pitch and amplitude envelopes) with its own mix weight. The kick sits at 0.55, loud on purpose. Each voice's envelope is advanced by the sequencer at 60 Hz, so percussion decays are specified in the same frame units as everything else.

**The Effect System**: A `Note` with only pitch, duration, and volume can play a melody, but it cannot *sing*. The tricks that made two square channels sound like a full arrangement, which composers like David Wise deployed constantly, are all pitch and amplitude modulation, so I built an immutable `Effect` class carrying five of them:

- *Arpeggio*: cycle two or three semitone offsets every frame, so fast that the ear fuses them into a chord. One mono channel pretends to be a triad. `withArp(0, 4, 7)` is a major chord.
- *Vibrato*: a delayed-onset pitch LFO, the late wobble on held notes.
- *Slide (portamento)*: glide from the previous note's pitch into this one.
- *Pitch envelope*: a one-shot ramp toward the note's true pitch, for scoops, zaps, and drum sweeps.
- *Swell*: the one amplitude effect. A flat-topped square that switches on at full volume sounds like a telephone tone; a pad needs to bloom. The swell multiplies the amplitude by the parabola $4t(1-t)$ where $t = f/(d-1)$ across the note's $d$ frames: silent at both ends, full at the midpoint.

All pitch effects sum additively in semitone space, so they stack freely, and the whole offset is applied through `freqOf(double midi)`, which accepts fractional MIDI values so modulated pitches stay in tune between the integer semitones. The crucial implementation detail is that mid-note pitch updates go through `setFrequency()`, which changes the oscillator's rate *without touching its phase or envelope*. The waveform keeps running continuously, only its speed changes between frames, and that is what makes vibrato, arpeggio, and slides click-free.

**Writing Songs**: A `Track` is a monophonic note list with a stateful fluent builder. Calls like `withDecay(...)`, `withVolume(...)`, and `withVibrato(...)` set the state applied to every subsequent `addNotes(...)`, so a song reads top to bottom like a score with dynamics markings in the margin. Three builders deserve special mention. `addSegs()` accepts rows of `{pitch, frames, volume}`, which is the natural shape of NSF register-capture data, where the volume column is part of the music itself (echo gates, painted envelopes). `addRun()` emits the chromatic sweep-into-a-note rip that tracker drivers love. And `echoOf()` implements my favorite trick in the entire library: the David Wise echo. It produces a copy of a source track delayed by a few frames, volume-scaled down, and re-voiced at a thinner duty cycle, so the second pulse channel becomes a studio delay line instead of a second instrument. The ear hears one melody with reverb rather than two square waves. The copy is constructed to be exactly the source's length (delay prepended as a rest, tail truncated), which matters because of the invariant below.

**The Aligned-Totals Invariant**: The synth loops each channel's track independently, which means all four tracks of a song *must* sum to identical frame totals, or the channels drift apart from the second loop onward. Rather than fight this at runtime, I made it a design invariant that every construction helper preserves: `echoOf()` is length-exact by construction, and loop points are stamped onto all four tracks from a single `ChiptuneSong.getLoopStartFrames()` value, so a song physically cannot give its channels different loop points. Songs with a play-once intro return the intro's length there, and the tracks wrap NSF-style past the head together.

**Keeping Time Honestly**: The trickiest bug in the project was not in the audio at all, but in the position readout for the player's seek slider. My first attempt timed the render loop with wall-clock time, and the reported position led the audible music by a noticeable margin. The realization that fixed it: *wall-clock time inside the render loop is not the song position*. The loop renders a 735-sample block in microseconds and then blocks in `line.write()` when the buffer is full, so the sequencer runs up to the buffer depth (~67 ms) ahead of what your ears are hearing. Timing loop iterations characterizes the buffer, not the music. The audio device is the only clock that matters, and `SourceDataLine.getLongFramePosition()` reports exactly how many samples it has actually played. So the `SongChronometer` keeps a small ring buffer recording which musical frame each written block corresponds to, and answers device positions with "this is the frame you are hearing right now." All of its accounting is integer, samples and frames counted rather than summed as fractional seconds, which is fixed-point arithmetic with the radix parked at one block: zero accumulated error by construction. A pleasing consequence of grounding on the device clock is that immediately after a seek, the readout keeps walking through the *old* position for the four or so blocks still queued in the buffer, because that is genuinely what is still coming out of the speakers.

**Seeking**: Jumping to an arbitrary position in sequenced music has a classic pitfall called state chase: you cannot simply start at the next note event, because a note that *began* before the target may still be sounding there, and skipping to the next event replaces a sustain with silence. Because my `Note`s are self-contained (volume, duty, decay, and effects are all baked in when the note is added), the chase reduces to one note of lookbehind: find the note spanning the target frame, let the sequencer re-trigger it with its mid-note elapsed counter restored, and carry the last real pitch before it so a portamento keeps its glide origin. All the pitch effects resume exactly, since they are pure functions of that elapsed counter. One honest approximation remains: the volume-decay envelope restarts from full rather than partially faded. It is exact for sustained notes and for the triangle, which has no envelope, and inaudible in practice for the rest.

**From FamiTracker to Java**: Transcribing songs by ear is rewarding but slow, so the library includes `FamiTrackerTextConverter`, which reads FamiTracker's text export and emits a compilable `ChiptuneSong` skeleton: one private track-builder method per (channel, pattern) pair, chained in ORDER-list sequence, so the tracker's own pattern structure becomes the song's section structure and repeated passages arrive pre-factored. Timing transfers 1:1 because FamiTracker (NTSC) and this synth both tick at 60 Hz. The converter's best story is the octave offset. The Surf City capture labeled a pulse note as "C-1", but a 2A03 pulse channel physically bottoms out around 55 Hz, so that row can only be scientific C2. NSFImport-style captures, it turns out, label everything one octave below scientific pitch, and a `+12` correction (cross-checked against phrases I had already verified by ear) maps the labels back onto true MIDI. Hardware constraints as a debugging tool: the chip itself told me the transcription was wrong. Instrument macros and effect columns are deliberately not auto-applied; they are emitted as comments with suggested `with*()` calls, because finishing a song by ear is, frankly, the fun part.

## The Player

The library ships with a standalone Swing player (`chiptunesynth.player.ChiptunePlayer`) for auditioning the repertoire, with play/stop, volume, speed, and a seek slider driven by the chronometer's device-grounded position callbacks. The track list is discovered by reflection over `MusicHandler.concretized`, and all playback goes through the same `MusicHandler` interface a game would use, so running the player exercises exactly the surface an integrating application depends on. Run it with no arguments for the UI, or `--list` / `--play <n>` from the console.

## Building

A NetBeans Ant project targeting Java 8. Clean & Build produces `dist/ChiptuneSynth.jar`, or from the command line:

```
ant jar
```

## The Repertoire

The included songs are fan covers of NES-era compositions, transcribed and arranged by me for this synthesizer, written by a fan for other people's listening pleasure. To be absolutely, indisputably clear: I do not own any of the compositions that the synthesizer plays, nor am I attempting to take credit for them in any capacity. All credit goes to the creators, owners, and/or publishers for each work in question, specifically Yakov Prigozhy (*Korobeiniki*, Independent Russian Publisher, 1898), Takashi Tateishi (*Flash Man Theme, Dr. Wily: Stage 1 Theme*, Mega Man 2, 1988), Kenichi Matsubara and Satoe Terashima (*Bloody Tears*, Castlevania 2, 1987), Hidenori Maezawa and Kiyohiro Sada (*Jungle*, Contra, 1988), David Wise (*Surf City and Terra Tubes*, Battletoads, 1991), Akito Nakatsuka (*Palace*, a.k.a. *Hyrule Temple*, The Legend of Zelda II: The Adventure of Link), Hiroshige Tonomura (*The Moon*, DuckTales, 1989), Koji Kondo (*Dungeon*, The Legend of Zelda, 1986), Geoffrey and Timothy Follin (*Title Theme*, Silver Surfer, 1990).

## License

MIT, see [LICENSE](LICENSE).
