# ChiptuneSynth

A small NES-style chiptune synthesizer for Java, modeling the 2A03 APU at a
high level. Originally written as the music engine for
[Javarominoes](https://github.com/dylantcon/javarominoes), now split out as a
standalone library.

## Features

- Two pulse (square) channels with selectable duty cycles
- One triangle channel with a quantized waveform
- One noise channel driven by a 15-bit linear feedback shift register
- Dedicated kick, snare, and tom percussion voices that punch through the mix
- Note effects: portamento (slide), vibrato, arpeggio, pitch envelopes, and
  amplitude swells
- A frame-rate sequencer driving all channels at ~60 Hz — the same rate the
  NES ticked its music engine via the NMI handler; note durations are measured
  in frames (60ths of a second), exactly the unit the original NES drivers used
- Runtime volume (with slew) and playback-speed control
- 44.1 kHz / 16-bit / stereo output via `javax.sound.sampled` — no native code,
  no external dependencies

## Usage

Implement `ChiptuneSong` to describe a song as four `Track`s (lead, harmony,
bass, drums) built from `Note`s, then hand it to the synth:

```java
ChiptuneSynth synth = ChiptuneSynth.getSynthesizer(new MySong()); // looping
synth.start();                  // plays on a daemon thread
synth.setVolume(0.8);           // 0.0 - 1.0, log-scaled with smooth slew
synth.setSpeed(1.25);           // tempo multiplier, e.g. for level scaling
synth.stop();
```

`ChiptuneSong` also provides the vocabulary for writing songs: note-length
constants (`Q`, `E`, `DQ`, ...), MIDI pitch constants (`C4`, `FS5`, ...), drum
selectors (`KICK`, `SNARE`, `HIHAT`, `CYMBAL`), and the rest marker `R`.

## Building

A NetBeans Ant project targeting Java 8. Clean & Build produces
`dist/ChiptuneSynth.jar`, or from the command line:

```
ant jar
```

## License

MIT — see [LICENSE](LICENSE).
