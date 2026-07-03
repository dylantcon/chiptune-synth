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
 * the output arrives pre-factored — repeated patterns become repeated method
 * calls you can immediately see and reshape into the house style.
 *
 * Channel mapping: pulse 1 -&gt; lead, pulse 2 -&gt; harmony, triangle -&gt; bass,
 * noise -&gt; drums (bucketed onto KICK/SNARE/HIHAT — remap by ear). The DPCM
 * column can't be synthesized, so its hits are emitted as comments in the
 * drums getter for hand-mapping onto the kick/snare/tom voices.
 *
 * Timing: FamiTracker (NTSC) and this synth both tick at 60 Hz, so row
 * durations transfer 1:1 as frames — frames per row = 150 * speed / tempo,
 * distributed with an accumulator when fractional. getTempoScale() is 1.0
 * for NTSC modules.
 *
 * WHAT IS NOT CONVERTED (v1) — finish these by ear, that's the fun part:
 *  - Instrument envelopes/macros: volume decay, duty sequences, and arps
 *    live in instruments, which are ignored. Set withDecay/withDuty per voice.
 *  - Effect columns: recognized and emitted as comments with a suggested
 *    with*() call (4xy vibrato, 0xy arp, 3xx portamento, Vxx duty, ...) but
 *    never applied automatically.
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

  /** Semitones added to every parsed pitch; FamiTracker C-4 = MIDI 60. */
  private static final int OCTAVE_OFFSET = 0;

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

  /** One tracker cell: note text, volume column, effect strings. */
  private static class Cell {
    String note = "...";
    int volume = -1;
    final List<String> fx = new ArrayList<String>();
  }

  /** One emitted note-or-rest with an optional volume switch before it. */
  private static class Ev {
    int midi;                 // -1 = rest
    int frames;
    int volume;               // 0..15, or -1 for "no change"
    String comment;           // e.g. original noise pitch
    Ev(int midi, int frames, int volume, String comment) {
      this.midi = midi; this.frames = frames;
      this.volume = volume; this.comment = comment;
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

  public static void main(String[] args) throws IOException {
    if (args.length < 1) {
      System.err.println(
          "usage: FamiTrackerTextConverter input.txt [output.java] [ClassName]");
      System.exit(2);
    }
    FamiTrackerTextConverter c = new FamiTrackerTextConverter();
    c.parse(Files.readAllLines(Paths.get(args[0]), StandardCharsets.UTF_8));

    String className = args.length >= 3 ? args[2]
        : args.length >= 2 ? classNameFrom(args[1])
        : "ConvertedSong";
    PrintStream out = args.length >= 2
        ? new PrintStream(args[1], "UTF-8") : System.out;
    c.emit(out, className, args[0]);
    if (out != System.out) {
      out.close();
      System.out.println("wrote " + args[1]);
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
      // tok[1] is the instrument column — ignored (envelopes not converted)
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

  /** Flatten one channel-column of one pattern into notes and rests. */
  private List<Ev> events(int patternId, int channel) {
    Cell[][] rows = patterns.get(patternId);
    List<Ev> evs = new ArrayList<Ev>();
    int curMidi = -1, curStart = 0, curVol = -1;
    String curComment = null;
    int total = rowStart(patternLength);

    for (int r = 0; r < patternLength; ++r) {
      Cell cell = (rows == null) ? null : rows[r][channel];
      String note = (cell == null) ? "..." : cell.note;
      if (note.equals("...")) {
        continue;                                   // current note/rest holds
      }
      int at = rowStart(r);
      if (at > curStart) {
        evs.add(new Ev(curMidi, at - curStart, curVol, curComment));
      }
      curStart = at;
      curVol = -1;
      curComment = null;
      if (note.equals("---") || note.equals("===")) {
        curMidi = -1;                               // halt / release -> rest
      } else if (note.endsWith("-#")) {             // noise pitch, 0-F
        int p = Integer.parseInt(note.substring(0, 1), 16);
        curMidi = noiseBucket(p);
        curComment = "noise " + note.charAt(0);
        curVol = (cell != null) ? cell.volume : -1;
      } else {
        curMidi = midiOf(note);
        curVol = (cell != null) ? cell.volume : -1;
      }
    }
    if (total > curStart) {
      evs.add(new Ev(curMidi, total - curStart, curVol, curComment));
    }
    return evs;
  }

  private int midiOf(String note) {
    int semi = "C D EF G A B".indexOf(note.charAt(0));
    if (note.charAt(1) == '#') {
      semi += 1;
    }
    int octave = note.charAt(2) - '0';
    return (octave + 1) * 12 + semi + OCTAVE_OFFSET;
  }

  // Noise pitches 0-F have no true pitch; bucket them onto the synth's drum
  // selectors as a starting point. The original value rides along in a
  // comment so remapping by ear is a find-and-replace.
  private int noiseBucket(int p) {
    return p <= 5 ? 36 : p <= 0xB ? 60 : 84;   // KICK / SNARE / HIHAT
  }

  private static String drumName(int midi) {
    return midi == 36 ? "KICK" : midi == 60 ? "SNARE" : "HIHAT";
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
    out.println(" * SKELETON ONLY -- instruments/envelopes/effects are not applied:");
    out.println(" *  set withDecay/withDuty/withVibrato per voice, act on the effect");
    out.println(" *  comments, remap the noise buckets by ear, and hand-map the DPCM");
    out.println(" *  comments onto the kick/snare/tom voices. Then factor patterns");
    out.println(" *  into named sections (see HyruleTempleSong for the house style).");
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
    // getter chaining the order list
    out.println();
    out.println("  @Override");
    out.println("  public Track " + CH_GETTER[channel] + "() {");
    out.println("    Track t = new Track().withDefaults("
        + CH_DEFAULTS[channel] + ");");
    Set<Integer> used = new LinkedHashSet<Integer>();
    for (int f = 0; f < order.size(); ++f) {
      int pat = order.get(f)[channel];
      used.add(pat);
      out.println("    t.addNotes(" + methodName(channel, pat)
          + "());   // order frame " + String.format("%02X", f));
    }
    out.println("    return t;");
    out.println("  }");

    // one builder per pattern actually referenced
    for (int pat : used) {
      emitPatternMethod(out, channel, pat);
    }
  }

  private void emitPatternMethod(PrintStream out, int channel, int patternId) {
    List<Ev> evs = events(patternId, channel);
    out.println();
    out.println("  private static Track " + methodName(channel, patternId) + "() {");
    out.println("    Track t = new Track().withDefaults("
        + CH_DEFAULTS[channel] + ");");
    emitFxComments(out, channel, patternId);

    boolean open = false;
    int lastVol = -1;
    for (Ev e : evs) {
      if (e.volume >= 0 && e.volume != lastVol) {
        if (open) {
          out.println();
          out.println("    );");
          open = false;
        }
        String frac = String.format(Locale.US, "%.2f", e.volume / 15.0);
        out.println("    t.withVolume(" + CH_BASE_VOL[channel]
            + " * " + frac + ");   // volume column "
            + Integer.toHexString(e.volume).toUpperCase());
        lastVol = e.volume;
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
          ? drumName(e.midi) : pitchName(e.midi);
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

  // Effects become comments with a suggested translation, never code.
  private void emitFxComments(PrintStream out, int channel, int patternId) {
    Cell[][] rows = patterns.get(patternId);
    if (rows == null) {
      return;
    }
    for (int r = 0; r < patternLength; ++r) {
      Cell cell = rows[r][channel];
      if (cell == null) {
        continue;
      }
      for (String fx : cell.fx) {
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
        return "pattern-flow effect — VERIFY section order by ear";
      case 'F':
        warnings.add("speed/tempo change " + fx + " mid-song is NOT converted");
        return "speed/tempo change — durations after this row are WRONG";
      case 'G': return "note delay Gxx -> shift the note by xx frames";
      case 'P': return "fine pitch Pxx -> usually ignorable";
      case 'Q': return "note slide up Qxy -> withSlide";
      case 'R': return "note slide down Rxy -> withSlide";
      case 'S': return "note cut Sxx -> shorten the note to xx frames";
      case 'V': return "duty/mode Vxx -> withDuty(...)";
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
