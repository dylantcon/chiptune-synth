package chiptunesynth.tools;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * FamiTracker text export -&gt; ChiptuneSynth song skeleton.
 *
 * Reads the output of FamiTracker's "File &gt; Export text..." and emits a
 * compilable {@code ChiptuneSong} class: one private Track-builder method per
 * (channel, pattern) pair, and getters that chain them in ORDER-list order.
 * The tracker's pattern/order structure IS the song's section structure, so
 * the output arrives pre-factored  repeated patterns become repeated method
 * calls you can immediately see and reshape into the house style.
 *
 * Channel mapping: pulse 1 -&gt; lead, pulse 2 -&gt; harmony, triangle -&gt; bass,
 * noise -&gt; drums (bucketed onto KICK/SNARE/HIHAT  remap by ear). The DPCM
 * column can't be synthesized, so its hits are emitted as comments in the
 * drums getter for hand-mapping onto the kick/snare/tom voices.
 *
 * Timing: FamiTracker (NTSC) and this synth both tick at 60 Hz, so row
 * durations transfer 1:1 as frames  frames per row = 150 * speed / tempo,
 * distributed with an accumulator when fractional. getTempoScale() is 1.0
 * for NTSC modules.
 *
 * WHAT IS APPLIED automatically:
 *  - Notes (pitch, length) and the volume column (as withVolume segments).
 *  - Every voice is SUSTAINED so the captured volume column IS the envelope.
 *  - Vxx duty on the two pulse channels -&gt; withDuty(), sampled at each attack.
 *  - Pxx fine pitch on the two PULSE channels -&gt; withDetune(), resolved to a
 *    semitone offset against each note's own period, but only when it clears a
 *    ~15-cent threshold (below that it is capture dither that just detunes the
 *    song; see DETUNE_THRESHOLD_SEMIS). The triangle bass gets no detune.
 *  - An optional --frames N truncates output to the song's true loop length.
 *
 * WHAT IS NOT CONVERTED  finish these by ear, that's the fun part:
 *  - Instrument envelopes/macros: volume decay, duty sequences, and arps live
 *    in instruments, which are ignored. Set withDecay per voice.
 *  - The remaining effect columns (4xy vibrato, 0xy arp, 3xx portamento, ...):
 *    still recognized and emitted as comments with a suggested with*() call.
 *  - Vxx on the noise channel is noise MODE, not duty; left as a comment.
 *  - A note's fine pitch is sampled once, at its attack; a mid-note Pxx bend
 *    (rare, mostly capture dither) is not baked  hand-shape those by ear.
 *  - Notes held across a pattern boundary are split and will re-attack.
 *  - Bxx/Dxx/Cxx flow effects: the ORDER list is assumed to play linearly.
 *
 * If the whole song lands an octave off (see SurfCitySong's rip history),
 * adjust OCTAVE_OFFSET here or add withTranspose(12) in the generated file.
 *
 * Usage: FamiTrackerTextConverter input.txt [output.java] [ClassName]
 *
 * @author dylan
 */
public class FamiTrackerTextConverter {

  /**
   * Semitones added to every parsed pitch. NSFImport-style captures label
   * notes one octave BELOW scientific pitch  proven by the Surf City
   * export, whose pulse 1 shows "C-1": a 2A03 pulse bottoms out at ~55 Hz,
   * so that row can only be sci C2 (65.4 Hz). Its answer phrase ("C-4")
   * and chime ("D#3") likewise matched our ear-verified C5/DS4 exactly.
   * +12 maps labels to true MIDI. For a hand-authored .ftm (not an
   * NSFImport capture), set this back to 0 and verify by ear.
   */
  private static final int OCTAVE_OFFSET = 12;

  private static final int CHANNELS = 5;   // 2A03: p1, p2, tri, noise, dpcm
  private static final String[] CH_LABEL = {"p1", "p2", "tri", "noi", "dpcm"};
  private static final String[] CH_GETTER =
      {"getLead", "getHarmony", "getBass", "getDrums", null};
  private static final String[] CH_DEFAULTS = {
    "LEAD_VOL, LEAD_DUTY", "HARMONY_VOL, HARMONY_DUTY",
    "BASS_VOL, BASS_DUTY", "DRUM_VOL, DRUM_DUTY", null
  };
  private static final String[] CH_BASE_VOL =
      {"LEAD_VOL", "HARMONY_VOL", "BASS_VOL", "DRUM_VOL", null};
  // the house-default duty each channel falls back to when no Vxx is in force
  // (the per-pattern getter resets to this at every pattern via withDefaults;
  // the bar emitter re-asserts it so the two match note-for-note).
  private static final String[] CH_DEFAULT_DUTY =
      {"LEAD_DUTY", "HARMONY_DUTY", "BASS_DUTY", "DRUM_DUTY", null};

  /** One tracker cell: note text, volume column, effect strings. */
  private static class Cell {
    String note = "...";
    int volume = -1;
    final List<String> fx = new ArrayList<String>();
  }

  /**
   * One emitted note-or-rest, carrying the column state sampled at its onset:
   * the volume switch (0-15), the pulse duty from the last Vxx (as its raw
   * 0-3 index, -1 = none seen yet), and the fine-pitch detune from the last
   * Pxx (as period-register units off 0x80). Duty and detune are read once,
   * when the note attacks  a mid-note Vxx/Pxx write updates state for the
   * NEXT note, matching how the synth latches duty at note-on and how these
   * NSF captures re-assert the registers at each attack.
   */
  private static class Ev {
    int midi;                 // -1 = rest
    int frames;
    int volume;               // 0..15, or -1 for "no change"
    int dutyUnits;            // Vxx value 0..3, or -1 for "none / no change"
    int detuneDev;            // Pxx value - 0x80, in period units (0 = centered)
    String comment;           // e.g. original noise pitch
    Ev(int midi, int frames, int volume, int dutyUnits, int detuneDev,
       String comment) {
      this.midi = midi; this.frames = frames;
      this.volume = volume; this.dutyUnits = dutyUnits;
      this.detuneDev = detuneDev; this.comment = comment;
    }
  }

  // ---- parsed module ----
  private int machine = 0;
  private int expansion = 0;
  private int patternLength = 64;
  private int speed = 6;
  private int tempo = 150;
  private String trackTitle = "";
  private final List<int[]> order = new ArrayList<int[]>();
  private final Map<Integer, Cell[][]> patterns =
      new LinkedHashMap<Integer, Cell[][]>();   // id -> [row][channel]
  private final List<String> warnings = new ArrayList<String>();

  // >0 truncates every channel to this many frames  the song's TRUE loop,
  // when the capture padded a short loop out to a fixed (e.g. 3-minute) window.
  // The synth loops each track at its own length, so cutting here makes the
  // turnaround land on the real loop point instead of after dead repeats.
  private int loopFrames = 0;

  // >0 switches to bar-sectioned output: one void method per bar of this many
  // frames, sharing a Track, instead of one Track method per pattern.
  private int barFrames = 0;

  // true renders the noise channel as authentic raw NES periods (NZ0..NZ15 +
  // NOISE_DECAY) instead of bucketing onto the KICK/SNARE/HIHAT selectors.
  private boolean rawNoise = false;

  public static void main(String[] args) throws IOException {
    // pull the optional "--frames N" / "--bars N" / "--noise-raw" flags out
    int loop = 0, bars = 0;
    boolean raw = false;
    List<String> pos = new ArrayList<String>();
    for (int i = 0; i < args.length; ++i) {
      if (args[i].equals("--frames") && i + 1 < args.length) {
        loop = Integer.parseInt(args[++i]);
      } else if (args[i].equals("--bars") && i + 1 < args.length) {
        bars = Integer.parseInt(args[++i]);
      } else if (args[i].equals("--noise-raw")) {
        raw = true;
      } else {
        pos.add(args[i]);
      }
    }
    if (pos.isEmpty()) {
      System.err.println("usage: FamiTrackerTextConverter input.txt "
          + "[output.java] [ClassName] [--frames N] [--bars N] [--noise-raw]");
      System.exit(2);
    }
    FamiTrackerTextConverter c = new FamiTrackerTextConverter();
    c.loopFrames = loop;
    c.barFrames = bars;
    c.rawNoise = raw;
    c.parse(Files.readAllLines(Paths.get(pos.get(0)), StandardCharsets.UTF_8));

    String className = pos.size() >= 3 ? pos.get(2)
        : pos.size() >= 2 ? classNameFrom(pos.get(1))
        : "ConvertedSong";
    PrintStream out = pos.size() >= 2
        ? new PrintStream(pos.get(1), "UTF-8") : System.out;
    if (c.barFrames > 0) {
      c.emitSectioned(out, className, pos.get(0));
    } else {
      c.emit(out, className, pos.get(0));
    }
    if (out != System.out) {
      out.close();
      System.out.println("wrote " + pos.get(1));
    }
    for (String w : c.warnings) {
      System.err.println("warning: " + w);
    }
  }

  private static String classNameFrom(String path) {
    String base = Paths.get(path).getFileName().toString();
    int dot = base.lastIndexOf('.');
    return dot > 0 ? base.substring(0, dot) : base;
  }

  /* ==================== PARSING ==================== */

  private void parse(List<String> lines) {
    int currentPattern = -1;
    boolean inFirstTrack = false, seenTrack = false;

    for (String raw : lines) {
      String line = raw.trim();
      if (line.isEmpty() || line.startsWith("#")) {
        continue;
      }
      String[] tok = line.split("\\s+");
      String key = tok[0];

      if (key.equals("MACHINE")) {
        machine = Integer.parseInt(tok[1]);
      } else if (key.equals("EXPANSION")) {
        expansion = Integer.parseInt(tok[1]);
        if (expansion != 0) {
          warnings.add("module uses expansion audio (EXPANSION=" + expansion
              + "); only the five 2A03 channels are converted");
        }
      } else if (key.equals("TRACK")) {
        if (seenTrack) {
          inFirstTrack = false;   // only the first song in the file
          warnings.add("multiple TRACKs in file; converted the first only");
          continue;
        }
        seenTrack = true;
        inFirstTrack = true;
        patternLength = Integer.parseInt(tok[1]);
        speed = Integer.parseInt(tok[2]);
        tempo = Integer.parseInt(tok[3]);
        int q = line.indexOf('"');
        if (q >= 0) {
          trackTitle = line.substring(q + 1, line.lastIndexOf('"'));
        }
      } else if (!inFirstTrack) {
        continue;   // ignore everything outside the first TRACK
      } else if (key.equals("ORDER")) {
        String[] ids = line.substring(line.indexOf(':') + 1).trim().split("\\s+");
        int[] frame = new int[CHANNELS];
        for (int c = 0; c < CHANNELS && c < ids.length; ++c) {
          frame[c] = Integer.parseInt(ids[c], 16);
        }
        order.add(frame);
      } else if (key.equals("PATTERN")) {
        currentPattern = Integer.parseInt(tok[1], 16);
        patterns.put(currentPattern, new Cell[patternLength][CHANNELS]);
      } else if (key.equals("ROW") && currentPattern >= 0) {
        parseRow(line, patterns.get(currentPattern));
      }
    }
  }

  private void parseRow(String line, Cell[][] rows) {
    String[] parts = line.split(":");
    int row = Integer.parseInt(parts[0].trim().split("\\s+")[1], 16);
    if (row >= patternLength) {
      return;
    }
    for (int c = 0; c < CHANNELS && c + 1 < parts.length; ++c) {
      String[] tok = parts[c + 1].trim().split("\\s+");
      Cell cell = new Cell();
      if (tok.length >= 1) {
        cell.note = tok[0];
      }
      // tok[1] is the instrument column  ignored (envelopes not converted)
      if (tok.length >= 3 && !tok[2].equals(".")) {
        cell.volume = Integer.parseInt(tok[2], 16);
      }
      for (int i = 3; i < tok.length; ++i) {
        if (!tok[i].startsWith(".")) {
          cell.fx.add(tok[i]);
        }
      }
      rows[row][c] = cell;
    }
  }

  /* ==================== EVENT EXTRACTION ==================== */

  // Row r of a pattern starts at round(r * fpr) frames; using the rounded
  // boundaries for durations keeps every channel's total identical even
  // when frames-per-row is fractional (tempo != 150).
  private double framesPerRow() {
    return 150.0 * speed / tempo;
  }

  private int rowStart(int row) {
    return (int) Math.round(row * framesPerRow());
  }

  /**
   * Flatten one channel-column of one pattern into notes and rests.
   *
   * The volume column is part of the music, not decoration: NSF drivers
   * gate held notes with volume writes (echo ghosts, tremolo chops  the
   * Follin shimmer is v3/v0 flips on a sustained pitch). So a volume write
   * on a note-less row SPLITS the current note into a new segment at the
   * new level, re-attacking the same pitch. In this synth that re-attack is
   * click-free (the channel's phase never resets; only the envelope is set),
   * so the result is exactly a hardware volume-register write.
   */
  private List<Ev> events(int patternId, int channel, int maxRows) {
    return events(patternId, channel, maxRows, -1, null);
  }

  /**
   * As above, but starting with {@code enterMidi} already sounding  the note
   * held over from the previous pattern. If this pattern opens on a hold row it
   * simply continues; if it opens on a new note, a release, or a rest, that
   * overrides it (so a boundary the driver actually cut still cuts). This is
   * what makes the output boundary-AWARE: a note ringing across the 256-frame
   * seam is carried instead of dropped, which had left ~5-frame holes. (Volume,
   * duty and detune still reset per pattern  the captures re-assert them, and
   * keeping the reset preserves the per-pattern character.)
   */
  private List<Ev> events(int patternId, int channel, int maxRows,
                          int enterMidi, String enterComment) {
    Cell[][] rows = patterns.get(patternId);
    List<Ev> evs = new ArrayList<Ev>();
    int curMidi = enterMidi, curStart = 0, curVol = -1;
    String curComment = enterComment;
    int total = rowStart(maxRows);

    // Live column state for the effect registers, and the values sampled at
    // the current segment's ONSET. Vxx (duty) and Pxx (fine pitch) persist
    // until rewritten, exactly like the hardware registers; a note takes the
    // value in force when it attacks, so a later write only affects the next
    // segment. segDuty/segDev freeze that onset value while the note holds.
    int curDutyUnits = -1, segDutyUnits = -1;       // -1 = no Vxx seen yet
    int curDev = 0, segDev = 0;                     // Pxx units off 0x80

    for (int r = 0; r < maxRows; ++r) {
      Cell cell = (rows == null) ? null : rows[r][channel];
      // effect registers latch on EVERY row, held rows included, so a write
      // during a sustain is picked up by whatever attacks next
      if (cell != null) {
        for (String fx : cell.fx) {
          if (fx.length() >= 3 && fx.charAt(0) == 'V') {
            curDutyUnits = parseHex(fx.substring(1));
          } else if (fx.length() >= 3 && fx.charAt(0) == 'P') {
            curDev = parseHex(fx.substring(1)) - 0x80;
          }
        }
      }
      String note = (cell == null) ? "..." : cell.note;
      int vol = (cell == null) ? -1 : cell.volume;
      boolean newNote = !note.equals("...");
      boolean volSplit = !newNote && vol >= 0 && vol != curVol;
      if (!newNote && !volSplit) {
        continue;                                   // current segment holds
      }
      int at = rowStart(r);
      if (at > curStart) {
        evs.add(new Ev(curMidi, at - curStart, curVol, segDutyUnits, segDev,
                curComment));
        curStart = at;
      }
      segDutyUnits = curDutyUnits;                  // freeze this segment's onset
      segDev = curDev;
      if (newNote) {
        if (note.equals("---") || note.equals("===")) {
          curMidi = -1;                             // halt / release -> rest
          curComment = null;
        } else if (note.endsWith("-#")) {           // noise pitch, 0-F
          int p = parseHex(note.substring(0, 1));
          // raw mode: encode the true NES period index (NZ_BASE + p) so the
          // synth clocks the real LFSR; default mode buckets onto KICK/SNARE/
          // HIHAT. Either way the original nibble rides along in the comment.
          curMidi = rawNoise ? NZ_BASE + p : noiseBucket(p);
          curComment = "noise " + note.charAt(0);
        } else {
          curMidi = midiOf(note);
          curComment = null;
        }
      }
      if (vol >= 0) {
        curVol = vol;                               // persists until rewritten
      }
    }
    if (total > curStart) {
      evs.add(new Ev(curMidi, total - curStart, curVol, segDutyUnits, segDev,
              curComment));
    }
    return coalesceGaps(evs);
  }

  // Frames of silence at or below this are swallowed by the preceding note.
  private static final int COALESCE_FRAMES = 2;

  /**
   * Legato pass: fold a 1-2 frame silence into the note before it. NSF drivers
   * write a single key-off frame between notes to re-trigger the channel; on
   * hardware it is inaudible, but our clean envelopes turn each into a ~16 ms
   * hole  a pervasive "tiny rest" that breaks legato lines (measured ~130 per
   * loop on a lead). These stubs are always exactly one frame and never a real
   * rest (the shortest musical rest here is a 7-frame articulation gap), so
   * extending the previous note through them is safe and preserves total
   * length: the next note still attacks on its own frame.
   */
  private static List<Ev> coalesceGaps(List<Ev> evs) {
    List<Ev> out = new ArrayList<Ev>();
    for (Ev e : evs) {
      boolean silent = (e.midi < 0) || (e.volume == 0);
      if (silent && e.frames <= COALESCE_FRAMES && !out.isEmpty()) {
        Ev prev = out.get(out.size() - 1);
        if (prev.midi >= 0 && prev.volume != 0) {   // a real note precedes it
          prev.frames += e.frames;                  // hold through the stub
          continue;
        }
      }
      out.add(e);
    }
    return out;
  }

  private static int parseHex(String s) {
    return Integer.parseInt(s, 16);
  }

  private int midiOf(String note) {
    int semi = "C D EF G A B".indexOf(note.charAt(0));
    if (note.charAt(1) == '#') {
      semi += 1;
    }
    int octave = note.charAt(2) - '0';
    return (octave + 1) * 12 + semi + OCTAVE_OFFSET;
  }

  // Mirror of ChiptuneSong.NZ_BASE: raw noise notes are encoded midi 128..143,
  // one per NES period index. Kept here as a literal because the converter
  // emits source and never links against the interface's constants.
  private static final int NZ_BASE = 128;

  // Noise pitches 0-F have no true pitch; bucket them onto the synth's drum
  // selectors as a starting point. The original value rides along in a
  // comment so remapping by ear is a find-and-replace.
  private int noiseBucket(int p) {
    return p <= 5 ? 36 : p <= 0xB ? 60 : 84;   // KICK / SNARE / HIHAT
  }

  // Stringify a noise-channel note to the constant name the generated source
  // should use: NZ0..NZ15 for a raw period (--noise-raw), else the bucketed
  // KICK/SNARE/HIHAT selector.
  private static String noiseName(int midi) {
    return midi >= NZ_BASE ? "NZ" + (midi - NZ_BASE) : drumName(midi);
  }

  // Track-level decay constant for a channel's generated builder. Every voice
  // is SOSTENUTO (hold flat, so the captured volume column is the envelope),
  // except the raw-noise drums track, whose hits get a percussive NOISE_DECAY.
  private String trackDecay(int channel) {
    return (rawNoise && channel == 3) ? "NOISE_DECAY" : "SOSTENUTO";
  }

  private static String drumName(int midi) {
    return midi == 36 ? "KICK" : midi == 60 ? "SNARE" : "HIHAT";
  }

  /* ---- effect-column conversion (Vxx duty, Pxx fine pitch) ---- */

  // NTSC 2A03 CPU clock. A channel's timer period is CPU/(divider*freq):
  // divider 16 for the pulses, 32 for the triangle. EXPANSION=0 here, so this
  // is the right clock; an expansion chip would need its own divider.
  private static final double CPU_NTSC = 1789773.0;

  // FamiTracker duty index (Vxx) -> pulse duty cycle. 25% and 75% are the same
  // waveform inverted, so they sound identical on a symmetric mixer.
  private static double dutyOf(int vUnits) {
    switch (vUnits) {
      case 0:  return 0.125;
      case 1:  return 0.25;
      case 2:  return 0.5;
      case 3:  return 0.75;
      default: return 0.5;      // unknown mode: neutral
    }
  }

  // Below this magnitude a baked detune is dropped to exact equal temperament.
  // The Pxx column of an NSF capture is dominated by +/-1 period-unit writes
  // (a few cents) that reconstruct the NES's own slightly-off tuning; baking
  // those pulls the whole song off-pitch (a measured ~4-cent flat bias) for no
  // musical gain. Only genuine bends/pitch-corrections  the large Pxx moves
  // clear this bar and survive. 0.15 st = 15 cents, comfortably above the
  // single-unit noise (~3-13 cents across the register) and below a real bend.
  private static final double DETUNE_THRESHOLD_SEMIS = 0.15;

  // Pxx fine pitch -> semitone detune at a given sounding pitch. FamiTracker
  // SUBTRACTS (value - 0x80) from the timer period, so a positive dev shortens
  // the period and RAISES the pitch. One period unit is a bigger slice of a
  // semitone up high (short period) than down low, so the offset is resolved
  // against THIS note's period instead of a fixed cents-per-unit. Sub-threshold
  // results snap to 0 so only real bends are baked (see DETUNE_THRESHOLD_SEMIS).
  private double detuneSemis(int dev, int midi, int channel) {
    if (dev == 0 || midi < 0) {
      return 0.0;
    }
    double freq = 440.0 * Math.pow(2.0, (midi - 69) / 12.0);
    double divider = (channel == 2) ? 32.0 : 16.0;        // triangle vs pulse
    double period = CPU_NTSC / (divider * freq);
    double shifted = period - dev;
    if (shifted <= 1.0) {
      return 0.0;                                          // nonsensical extreme
    }
    double semis = 12.0 * Math.log(period / shifted) / Math.log(2.0);
    return (Math.abs(semis) < DETUNE_THRESHOLD_SEMIS) ? 0.0 : semis;
  }

  private static String hex(int v) {
    return Integer.toHexString(v).toUpperCase(Locale.US);
  }

  /* ==================== EMISSION ==================== */

  private static final String[] NOTE_NAMES =
      {"C", "CS", "D", "DS", "E", "F", "FS", "G", "GS", "A", "AS", "B"};

  /** MIDI number -> ChiptuneSong constant name, or the raw number. */
  private static String pitchName(int midi) {
    if (midi < 0) {
      return "R";
    }
    int octave = midi / 12 - 1;
    if (octave < 1 || octave > 6) {
      return String.valueOf(midi);      // outside the named constants
    }
    return NOTE_NAMES[midi % 12] + octave;
  }

  private String methodName(int channel, int patternId) {
    return CH_LABEL[channel] + "Pat" + String.format("%02X", patternId);
  }

  private void emit(PrintStream out, String className, String sourceName) {
    out.println("package chiptunesynth.songs;");
    out.println();
    out.println("import chiptunesynth.ChiptuneSong;");
    out.println("import chiptunesynth.Track;");
    out.println();
    out.println("/**");
    out.println(" * Generated by FamiTrackerTextConverter from " + sourceName);
    out.println(" * Track: \"" + trackTitle + "\"  patternLength=" + patternLength
        + "  speed=" + speed + "  tempo=" + tempo
        + "  (" + String.format(Locale.US, "%.3f", framesPerRow())
        + " frames/row)");
    out.println(" *");
    if (loopFrames > 0) {
      out.println(" * Truncated to the true loop: " + loopFrames + " frames ("
          + String.format(Locale.US, "%.2f", loopFrames / 60.0)
          + "s at 60 Hz)  the capture padded this out to a fixed window.");
      out.println(" *");
    }
    out.println(" * Applied inline: notes, the volume column (withVolume), Vxx duty");
    out.println(" * (withDuty), and Pxx fine pitch on the PULSES (withDetune, only bends");
    out.println(" * past ~15 cents  the sub-cent dither is dropped to stay in tune; the");
    out.println(" * triangle bass is left at exact pitch).");
    out.println(" * Every voice is SUSTAINED so the captured volume column carries the");
    out.println(" * envelope. Still by hand: instrument timbre/decay nuance, remaining");
    out.println(" * effect columns (left as comments), noise buckets (remap by ear), and");
    out.println(" * the DPCM comments. Then factor patterns into named sections (see");
    out.println(" * FlashmanSong / HyruleTempleSong for the house style).");
    out.println(" */");
    out.println("public class " + className + " implements ChiptuneSong {");

    if (machine == 1) {
      out.println();
      out.println("  // PAL module: rows were authored against a 50 Hz tick.");
      out.println("  @Override");
      out.println("  public double getTempoScale() {");
      out.println("    return 50.0 / 60.0;");
      out.println("  }");
    }

    for (int c = 0; c < 4; ++c) {
      emitChannel(out, c);
    }
    emitDpcmComments(out);
    out.println("}");
  }

  private void emitChannel(PrintStream out, int channel) {
    // resolve the loop truncation into whole patterns + an optional partial
    // final pattern (a mid-pattern loop point, e.g. Air Man's 2720 frames).
    int framesPerPattern = rowStart(patternLength);
    int nOrder = order.size();
    int partialRows = 0;
    if (loopFrames > 0 && loopFrames < order.size() * framesPerPattern) {
      nOrder = loopFrames / framesPerPattern;
      int remFrames = loopFrames - nOrder * framesPerPattern;
      partialRows = (int) Math.round(remFrames / framesPerRow());
    }

    // getter chaining the order list
    out.println();
    out.println("  @Override");
    out.println("  public Track " + CH_GETTER[channel] + "() {");
    out.println("    Track t = new Track().withDefaults("
        + CH_DEFAULTS[channel] + ");");
    Set<Integer> used = new LinkedHashSet<Integer>();
    for (int f = 0; f < nOrder; ++f) {
      int pat = order.get(f)[channel];
      used.add(pat);
      out.println("    t.addNotes(" + methodName(channel, pat)
          + "());   // order frame " + String.format("%02X", f));
    }
    int partialPat = -1;
    if (partialRows > 0 && nOrder < order.size()) {
      partialPat = order.get(nOrder)[channel];
      out.println("    t.addNotes(" + partialName(channel, partialPat, partialRows)
          + "());   // order frame " + String.format("%02X", nOrder)
          + " (first " + partialRows + " rows  loop cut)");
    }
    out.println("    return t;");
    out.println("  }");

    // one builder per full pattern referenced, plus the partial builder
    for (int pat : used) {
      emitPatternMethod(out, channel, pat, patternLength, false);
    }
    if (partialPat >= 0) {
      emitPatternMethod(out, channel, partialPat, partialRows, true);
    }
  }

  private String partialName(int channel, int patternId, int rows) {
    return methodName(channel, patternId) + "r" + rows;
  }

  private void emitPatternMethod(PrintStream out, int channel, int patternId,
                                 int maxRows, boolean partial) {
    List<Ev> evs = events(patternId, channel, maxRows);
    out.println();
    String name = partial ? partialName(channel, patternId, maxRows)
                          : methodName(channel, patternId);
    out.println("  private static Track " + name + "() {");
    out.println("    Track t = new Track().withDefaults("
        + CH_DEFAULTS[channel] + ").withDecay(" + trackDecay(channel) + ");");
    emitFxComments(out, channel, patternId, maxRows);

    boolean applyDuty = (channel == 0 || channel == 1);   // pulses latch duty
    // Detune is pulse-only. The triangle's Pxx, resolved through the /32
    // divider (and the label+12 octave the converter gives every channel),
    // comes out as 50-100 cent swings on a bass voice that must sit dead on
    // pitch  clearly wrong, so the bass is left at exact equal temperament.
    boolean applyDetune = (channel == 0 || channel == 1);

    boolean open = false;
    int lastVol = -1, lastDuty = -1;
    String lastDetune = "0";
    for (Ev e : evs) {
      // collect the state-setter lines this event needs; any one of them has
      // to break the addNotes(...) list, since a with*() call must land before
      // the notes it affects (all three persist like withVolume).
      List<String> setters = new ArrayList<String>();
      if (e.volume >= 0 && e.volume != lastVol) {
        String frac = String.format(Locale.US, "%.2f", e.volume / 15.0);
        setters.add("t.withVolume(" + CH_BASE_VOL[channel] + " * " + frac
            + ");   // volume column " + hex(e.volume));
        lastVol = e.volume;
      }
      if (applyDuty && e.midi >= 0 && e.dutyUnits >= 0
          && e.dutyUnits != lastDuty) {
        setters.add(String.format(Locale.US,
            "t.withDuty(%.3f);   // V%02X duty", dutyOf(e.dutyUnits),
            e.dutyUnits));
        lastDuty = e.dutyUnits;
      }
      if (applyDetune && e.midi >= 0) {
        double semis = detuneSemis(e.detuneDev, e.midi, channel);
        String sv = (Math.abs(semis) < 0.005) ? "0"
            : String.format(Locale.US, "%.2f", semis);
        if (!sv.equals(lastDetune)) {
          setters.add("t.withDetune(" + sv + ");   // P" + hex(e.detuneDev + 0x80)
              + " fine pitch");
          lastDetune = sv;
        }
      }
      if (!setters.isEmpty()) {
        if (open) {
          out.println();
          out.println("    );");
          open = false;
        }
        for (String s : setters) {
          out.println("    " + s);
        }
      }
      if (!open) {
        out.println("    t.addNotes(");
        out.print("        ");
        open = true;
      } else {
        out.print(",");
        out.println();
        out.print("        ");
      }
      // noise-channel hits get the drum-selector names (KICK=36 is also C2,
      // but the generated code should say what it means)
      String pname = (channel == 3 && e.midi >= 0 && e.comment != null)
          ? noiseName(e.midi) : pitchName(e.midi);
      out.print(pname + ", " + e.frames);
      if (e.comment != null) {
        out.print(" /* " + e.comment + " */");
      }
    }
    if (open) {
      out.println();
      out.println("    );");
    }
    out.println("    return t;");
    out.println("  }");
  }

  /* ==================== BAR-SECTIONED EMISSION (--bars) ==================== */

  /** Carried emission state: bar methods share one Track, so withVolume/
   *  withDuty/withDetune are only re-emitted when the value actually changes. */
  private static class EmitState {
    int lastVol = -1, lastDuty = -1;
    String lastDetune = "0";
  }

  /**
   * Emit the setter + addNotes lines for one event list into an already-open
   * method body ("Track t" assumed to exist). {@code st} carries across bars so
   * state set in one bar is not redundantly re-emitted in the next (they share
   * the runtime Track). Same translation as emitPatternMethod, factored so the
   * per-bar methods read identically to the per-pattern ones.
   */
  private void emitEvents(PrintStream out, int channel, List<Ev> evs,
                          EmitState st) {
    boolean applyDuty = (channel == 0 || channel == 1);
    boolean applyDetune = (channel == 0 || channel == 1);
    boolean open = false;
    for (Ev e : evs) {
      List<String> setters = new ArrayList<String>();
      if (e.volume >= 0 && e.volume != st.lastVol) {
        String frac = String.format(Locale.US, "%.2f", e.volume / 15.0);
        setters.add("t.withVolume(" + CH_BASE_VOL[channel] + " * " + frac
            + ");   // volume column " + hex(e.volume));
        st.lastVol = e.volume;
      }
      if (applyDuty && e.midi >= 0) {
        if (e.dutyUnits >= 0 && e.dutyUnits != st.lastDuty) {
          setters.add(String.format(Locale.US,
              "t.withDuty(%.3f);   // V%02X duty", dutyOf(e.dutyUnits),
              e.dutyUnits));
          st.lastDuty = e.dutyUnits;
        } else if (e.dutyUnits < 0 && st.lastDuty != -1) {
          // no Vxx in force -> fall back to the channel default (matches the
          // per-pattern getter's withDefaults reset)
          setters.add("t.withDuty(" + CH_DEFAULT_DUTY[channel]
              + ");   // default duty");
          st.lastDuty = -1;
        }
      }
      if (applyDetune && e.midi >= 0) {
        double semis = detuneSemis(e.detuneDev, e.midi, channel);
        String sv = (Math.abs(semis) < 0.005) ? "0"
            : String.format(Locale.US, "%.2f", semis);
        if (!sv.equals(st.lastDetune)) {
          setters.add("t.withDetune(" + sv + ");   // P"
              + hex(e.detuneDev + 0x80) + " fine pitch");
          st.lastDetune = sv;
        }
      }
      if (!setters.isEmpty()) {
        if (open) {
          out.println();
          out.println("    );");
          open = false;
        }
        for (String s : setters) {
          out.println("    " + s);
        }
      }
      if (!open) {
        out.println("    t.addNotes(");
        out.print("        ");
        open = true;
      } else {
        out.print(",");
        out.println();
        out.print("        ");
      }
      String pname = (channel == 3 && e.midi >= 0 && e.comment != null)
          ? noiseName(e.midi) : pitchName(e.midi);
      out.print(pname + ", " + e.frames);
      if (e.comment != null) {
        out.print(" /* " + e.comment + " */");
      }
    }
    if (open) {
      out.println();
      out.println("    );");
    }
  }

  /**
   * Concatenate the per-pattern events for one channel across the (truncated)
   * ORDER into one stream  the exact same note events the per-pattern getter
   * produces, so the bar-sectioned output sounds identical.
   */
  private List<Ev> concatEvents(int channel) {
    int framesPerPattern = rowStart(patternLength);
    int nOrder = order.size();
    int partialRows = 0;
    if (loopFrames > 0 && loopFrames < order.size() * framesPerPattern) {
      nOrder = loopFrames / framesPerPattern;
      int remFrames = loopFrames - nOrder * framesPerPattern;
      partialRows = (int) Math.round(remFrames / framesPerRow());
    }
    List<Ev> all = new ArrayList<Ev>();
    int enterMidi = -1;
    String enterComment = null;
    for (int f = 0; f < nOrder; ++f) {
      List<Ev> evs = events(order.get(f)[channel], channel, patternLength,
              enterMidi, enterComment);
      all.addAll(evs);
      // carry whatever note is still sounding at this pattern's end into the
      // next one, so a sustain that spans the seam is continued not cut
      Ev last = evs.isEmpty() ? null : evs.get(evs.size() - 1);
      if (last != null && last.midi >= 0) {
        enterMidi = last.midi;
        enterComment = last.comment;
      } else {
        enterMidi = -1;
        enterComment = null;
      }
    }
    if (partialRows > 0 && nOrder < order.size()) {
      all.addAll(events(order.get(nOrder)[channel], channel, partialRows,
              enterMidi, enterComment));
    }
    // The per-pattern getter starts each pattern at the channel's default
    // volume (withDefaults), so a note before the first volume write plays at
    // full base level. That is exactly volume 15, so resolve -1 -> 15 here and
    // the shared-Track bar emitter reaches the same level. (Duty's default is
    // not a Vxx value, so it is handled in emitEvents instead.)
    for (Ev e : all) {
      if (e.volume < 0) {
        e.volume = 15;
      }
    }
    return all;
  }

  /**
   * Split an event stream into per-bar lists of {@code barFrames} frames each.
   * An event straddling a bar line is cut in two (same pitch/volume/duty/
   * detune); with the SUSTAINED body the continuation re-attacks seamlessly, so
   * the sound is unchanged  it just lets each bar be its own method.
   */
  private List<List<Ev>> barSegment(List<Ev> evs, int barFrames) {
    List<List<Ev>> bars = new ArrayList<List<Ev>>();
    List<Ev> cur = new ArrayList<Ev>();
    int pos = 0, barEnd = barFrames;
    for (Ev e : evs) {
      int remaining = e.frames;
      while (remaining > 0) {
        int take = Math.min(remaining, barEnd - pos);
        cur.add(new Ev(e.midi, take, e.volume, e.dutyUnits, e.detuneDev,
                e.comment));
        pos += take;
        remaining -= take;
        if (pos >= barEnd) {
          bars.add(cur);
          cur = new ArrayList<Ev>();
          barEnd += barFrames;
        }
      }
    }
    if (!cur.isEmpty()) {
      bars.add(cur);
    }
    return bars;
  }

  private String barMethodName(int channel, int bar) {
    return CH_LABEL[channel] + "Bar" + String.format("%02d", bar);
  }

  private void emitSectionedChannel(PrintStream out, int channel) {
    List<Ev> all = concatEvents(channel);
    List<List<Ev>> bars = barSegment(all, barFrames);
    // getter: build the track, run every bar builder over it in turn
    out.println();
    out.println("  @Override");
    out.println("  public Track " + CH_GETTER[channel] + "() {");
    out.println("    Track t = new Track().withDefaults("
        + CH_DEFAULTS[channel] + ").withDecay(" + trackDecay(channel) + ");");
    for (int b = 0; b < bars.size(); ++b) {
      out.println("    " + barMethodName(channel, b) + "(t);");
    }
    out.println("    return t;");
    out.println("  }");
    // one void builder per bar; a shared EmitState carries state across them
    EmitState st = new EmitState();
    for (int b = 0; b < bars.size(); ++b) {
      out.println();
      out.println("  private static void " + barMethodName(channel, b)
          + "(Track t) {   // bar " + b);
      emitEvents(out, channel, bars.get(b), st);
      out.println("  }");
    }
  }

  private void emitSectioned(PrintStream out, String className,
                             String sourceName) {
    out.println("package chiptunesynth.songs;");
    out.println();
    out.println("import chiptunesynth.ChiptuneSong;");
    out.println("import chiptunesynth.Track;");
    out.println();
    out.println("/**");
    out.println(" * Generated by FamiTrackerTextConverter (bar-sectioned) from "
        + sourceName);
    out.println(" * loop=" + loopFrames + " frames, bar=" + barFrames
        + " frames (" + (loopFrames / barFrames) + " bars).");
    out.println(" *");
    out.println(" * One void method per bar, each adding to the shared Track so");
    out.println(" * volume/duty/detune state carries across bars (house style). The");
    out.println(" * note events are identical to the per-pattern output  just");
    out.println(" * grouped by bar for reading and hand-tuning.");
    out.println(" */");
    out.println("public class " + className + " implements ChiptuneSong {");
    if (machine == 1) {
      out.println();
      out.println("  @Override");
      out.println("  public double getTempoScale() {");
      out.println("    return 50.0 / 60.0;");
      out.println("  }");
    }
    for (int c = 0; c < 4; ++c) {
      emitSectionedChannel(out, c);
    }
    emitDpcmComments(out);
    out.println("}");
  }

  // Effects we can't (yet) translate become comments with a suggested
  // with*() call. Vxx duty and Pxx fine pitch are applied inline on the
  // melodic channels, so they are filtered out here to avoid duplicating them.
  private void emitFxComments(PrintStream out, int channel, int patternId,
                              int maxRows) {
    Cell[][] rows = patterns.get(patternId);
    if (rows == null) {
      return;
    }
    for (int r = 0; r < maxRows; ++r) {
      Cell cell = rows[r][channel];
      if (cell == null) {
        continue;
      }
      for (String fx : cell.fx) {
        char k = fx.charAt(0);
        if (channel <= 2 && (k == 'V' || k == 'P')) {
          continue;                    // applied as withDuty / withDetune
        }
        if (channel == 3 && k == 'P') {
          continue;                    // fine pitch is meaningless on noise
        }
        out.println("    // row " + String.format("%02X", r) + ": effect "
            + fx + " -- " + fxHint(fx));
      }
    }
  }

  private String fxHint(String fx) {
    switch (fx.charAt(0)) {
      case '0': return "arpeggio 0xy -> withArp(0, x, y)";
      case '1': return "slide up 1xx -> withPitchEnv/withSlide";
      case '2': return "slide down 2xx -> withPitchEnv/withSlide";
      case '3': return "portamento 3xx -> withSlide(frames)";
      case '4': return "vibrato 4xy (x=speed, y=depth) -> withVibrato(y/10ish, hz, delay)";
      case '7': return "tremolo 7xy -> no direct equivalent; consider withSwell";
      case 'A': return "volume slide Axy -> approximate with withVolume steps";
      case 'B': case 'C': case 'D':
        warnings.add("flow effect " + fx + " present; ORDER assumed linear");
        return "pattern-flow effect  VERIFY section order by ear";
      case 'F':
        warnings.add("speed/tempo change " + fx + " mid-song is NOT converted");
        return "speed/tempo change  durations after this row are WRONG";
      case 'G': return "note delay Gxx -> shift the note by xx frames";
      case 'P': return "fine pitch Pxx  applied as withDetune on pitched voices";
      case 'Q': return "note slide up Qxy -> withSlide";
      case 'R': return "note slide down Rxy -> withSlide";
      case 'S': return "note cut Sxx -> shorten the note to xx frames";
      case 'V': return "duty/mode Vxx  applied as withDuty on the pulses; on "
          + "the drum channel this is noise mode (short LFSR) -- remap by ear";
      default:  return "unhandled effect";
    }
  }

  // The synth has no sampler; surface every DPCM hit for hand-mapping.
  private void emitDpcmComments(PrintStream out) {
    StringBuilder sb = new StringBuilder();
    for (int f = 0; f < order.size(); ++f) {
      Cell[][] rows = patterns.get(order.get(f)[4]);
      if (rows == null) {
        continue;
      }
      for (int r = 0; r < patternLength; ++r) {
        Cell cell = rows[r][4];
        if (cell != null && !cell.note.equals("...")
            && !cell.note.equals("---") && !cell.note.equals("===")) {
          sb.append("  //   order frame ").append(String.format("%02X", f))
            .append(" row ").append(String.format("%02X", r))
            .append(": ").append(cell.note).append('\n');
        }
      }
    }
    if (sb.length() > 0) {
      out.println();
      out.println("  // DPCM hits (not synthesized -- map onto KICK/SNARE/toms");
      out.println("  // in getDrums by ear):");
      out.print(sb);
    }
  }
}
