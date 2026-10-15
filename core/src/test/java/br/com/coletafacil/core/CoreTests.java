package br.com.coletafacil.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Dependency-free tests of real production code; run with tools/test-core.sh. */
public final class CoreTests {
    private static int passed;
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        passed++;
    }
    static Set<String> codes(String... values) { return new LinkedHashSet<>(Arrays.asList(values)); }
    public static void main(String[] args) throws Exception {
        ScanGate gate = new ScanGate();
        check("001234".equals(gate.candidate(codes("001234"), 0)), "first visible code must be offered");
        check(gate.candidate(codes("001234"), 50) == null, "in-flight save must not duplicate");
        gate.accepted("001234");
        check(gate.candidate(codes("001234"), 100) == null, "stationary code must stay suppressed");
        check(gate.candidate(codes(), 200) == null, "empty frame adds nothing");
        check(gate.candidate(codes("001234"), 300) == null, "one missed frame must not rearm");
        gate.candidate(codes(), 400);
        gate.candidate(codes(), 1000);
        check("001234".equals(gate.candidate(codes("001234"), 1050)), "removal for 600ms must rearm");
        gate.failed("001234");
        check("001234".equals(gate.candidate(codes("001234"), 1100)), "failed persistence must allow retry");
        gate.accepted("001234");
        check("B".equals(gate.candidate(codes("B"), 1200)), "different visible code must be accepted immediately");
        gate.accepted("B");
        check(gate.candidate(codes("", " "), 1250) == null, "blank barcode must be ignored");
        check(" 001 ".equals(gate.candidate(codes(" 001 "), 1300)), "code whitespace must be preserved");
        gate.accepted(" 001 ");

        ScanSessions sessions = new ScanSessions();
        ScanGate beforePause = sessions.forCollection("coleta-a");
        check("SAME".equals(beforePause.candidate(codes("SAME"), 0)), "collection session offers first code");
        beforePause.accepted("SAME");
        ScanGate afterResume = sessions.forCollection("coleta-a");
        check(afterResume.candidate(codes("SAME"), 1000) == null, "preview recreation must not duplicate a stationary code");
        check("SAME".equals(sessions.forCollection("coleta-b").candidate(codes("SAME"), 1050)), "different collections keep independent gates");
        afterResume.candidate(codes(), 1100);
        afterResume.candidate(codes(), 1800);
        check("SAME".equals(afterResume.candidate(codes("SAME"), 1900)), "resumed session rearms after observed removal");

        List<ScanRecord> rows = Arrays.asList(
            new ScanRecord("00001234567890123456789", "Bomba d’água; setor \"A\"\nLinha 2", "Sim"),
            new ScanRecord("=1+2", "@SUM(A1)", "Não"),
            new ScanRecord("QR-ç-😀", "", "Pendente"),
            new ScanRecord("\t -2", " +malicioso", "Pendente"),
            new ScanRecord("_x000A_ & < >", "  Nome com espaços  ", "Sim")
        );
        ByteArrayOutputStream csv = new ByteArrayOutputStream();
        SpreadsheetWriter.writeCsv(csv, rows);
        byte[] bytes = csv.toByteArray();
        check(bytes.length > 3 && (bytes[0] & 255) == 239 && (bytes[1] & 255) == 187 && (bytes[2] & 255) == 191, "CSV UTF-8 BOM");
        String text = new String(bytes, StandardCharsets.UTF_8);
        check(text.contains("\"Código\";\"Nome\";\"Confere no Kronos?\"\r\n"), "exact CSV columns and order");
        check(text.contains("setor \"\"A\"\"\nLinha 2"), "CSV embedded quotes and newlines");
        check(text.contains("\"'=1+2\";\"'@SUM(A1)\""), "CSV formulas must be neutralized");
        check(text.contains("\"'\t -2\";\"' +malicioso\""), "CSV whitespace-prefixed formulas must be neutralized");
        check(text.contains("00001234567890123456789"), "CSV code leading zeros");

        Path out = Paths.get(args.length == 0 ? "build/test-exports" : args[0]);
        Files.createDirectories(out);
        Files.write(out.resolve("coleta.csv"), bytes);
        try (OutputStream xlsx = Files.newOutputStream(out.resolve("coleta.xlsx"))) { SpreadsheetWriter.writeXlsx(xlsx, rows); }
        try (OutputStream empty = Files.newOutputStream(out.resolve("vazia.xlsx"))) { SpreadsheetWriter.writeXlsx(empty, Collections.emptyList()); }
        boolean rejected = false;
        try { SpreadsheetWriter.writeXlsx(new ByteArrayOutputStream(), Arrays.asList(new ScanRecord("a", String.join("", Collections.nCopies(32768, "x")), "Sim"))); }
        catch (IOException expected) { rejected = true; }
        check(rejected, "XLSX must reject cells beyond Excel limit");
        System.out.println("PASS: " + passed + " assertions; production exports generated in " + out);
    }
}
