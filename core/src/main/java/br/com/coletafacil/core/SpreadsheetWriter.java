package br.com.coletafacil.core;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
public final class SpreadsheetWriter {
    private static final String[] HEADERS = {"Código", "Nome", "Confere no Kronos?"};
    private static final String NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private SpreadsheetWriter() { }

    public static void writeCsv(OutputStream out, List<ScanRecord> records) throws IOException {
        out.write(new byte[]{(byte)239, (byte)187, (byte)191});
        Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        csvRow(writer, HEADERS);
        for (ScanRecord row : records) csvRow(writer, new String[]{row.code, row.name, row.status});
        writer.flush();
    }

    private static void csvRow(Writer writer, String[] cells) throws IOException {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) writer.write(';');
            String value = Objects.requireNonNull(cells[i]);
            int start = 0;
            while (start < value.length() && (Character.isWhitespace(value.charAt(start)) || value.charAt(start) == '\uFEFF')) start++;
            if (start < value.length() && "=+-@".indexOf(value.charAt(start)) >= 0) value = "'" + value;
            writer.write('"');
            writer.write(value.replace("\"", "\"\""));
            writer.write('"');
        }
        writer.write("\r\n");
    }

    public static void writeXlsx(OutputStream out, List<ScanRecord> records) throws IOException {
        if (records.size() > 1048575) throw new IOException("A coleta excede o limite de linhas do Excel.");
        LinkedHashMap<String, Integer> strings = new LinkedHashMap<>();
        for (String header : HEADERS) addString(strings, header);
        for (ScanRecord row : records) {
            addString(strings, row.code); addString(strings, row.name); addString(strings, row.status);
        }
        // ZipOutputStream owns native compressor resources. Release them while
        // retaining the caller's ownership of its document-provider stream.
        try (ZipOutputStream zip = new ZipOutputStream(new FilterOutputStream(out) {
            @Override public void close() throws IOException { flush(); }
        })) {
        entry(zip, "[Content_Types].xml", "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/><Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/><Override PartName=\"/xl/sharedStrings.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml\"/></Types>");
        entry(zip, "_rels/.rels", "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
        entry(zip, "xl/workbook.xml", "<workbook xmlns=\"" + NS + "\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Coleta\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
        entry(zip, "xl/_rels/workbook.xml.rels", "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/><Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/><Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings\" Target=\"sharedStrings.xml\"/></Relationships>");
        entry(zip, "xl/styles.xml", "<styleSheet xmlns=\"" + NS + "\"><fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font><font><b/><color rgb=\"FFFFFFFF\"/><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts><fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF334155\"/><bgColor indexed=\"64\"/></patternFill></fill></fills><borders count=\"1\"><border/></borders><cellStyleXfs count=\"1\"><xf/></cellStyleXfs><cellXfs count=\"2\"><xf numFmtId=\"49\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"><alignment vertical=\"top\" wrapText=\"1\"/></xf><xf numFmtId=\"49\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyNumberFormat=\"1\"/></cellXfs><cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles></styleSheet>");
        zip.putNextEntry(new ZipEntry("xl/sharedStrings.xml"));
        write(zip, "<?xml version=\"1.0\" encoding=\"UTF-8\"?><sst xmlns=\"" + NS + "\" count=\"" + ((records.size()+1L)*3) + "\" uniqueCount=\"" + strings.size() + "\">");
        for (String s : strings.keySet()) write(zip, "<si><t xml:space=\"preserve\">" + xml(s) + "</t></si>");
        write(zip, "</sst>"); zip.closeEntry();
        zip.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml"));
        int lastRow = records.size() + 1;
        write(zip, "<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"" + NS + "\"><dimension ref=\"A1:C" + lastRow + "\"/><sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews><cols><col min=\"1\" max=\"1\" width=\"30\" customWidth=\"1\"/><col min=\"2\" max=\"2\" width=\"42\" customWidth=\"1\"/><col min=\"3\" max=\"3\" width=\"24\" customWidth=\"1\"/></cols><sheetData>");
        xlsxRow(zip, 1, HEADERS, strings);
        int index = 2;
        for (ScanRecord row : records) xlsxRow(zip, index++, new String[]{row.code,row.name,row.status}, strings);
        write(zip, "</sheetData><autoFilter ref=\"A1:C" + lastRow + "\"/></worksheet>");
        zip.closeEntry(); zip.finish(); zip.flush();
        }
    }

    private static void addString(Map<String,Integer> strings, String s) throws IOException {
        if (s == null || s.length() > 32767) throw new IOException("Um campo excede o limite de 32.767 caracteres do Excel.");
        for (int i=0; i<s.length(); i++) {
            char c=s.charAt(i);
            if ((c < 32 && c != '\t' && c != '\n' && c != '\r') || c == 0xFFFE || c == 0xFFFF) throw new IOException("Um campo contém caracteres incompatíveis com o Excel. Edite o registro antes de exportar.");
            if (Character.isHighSurrogate(c)) {
                if (i+1 >= s.length() || !Character.isLowSurrogate(s.charAt(i+1))) throw new IOException("Texto Unicode inválido.");
                i++;
            } else if (Character.isLowSurrogate(c)) throw new IOException("Texto Unicode inválido.");
        }
        if (!strings.containsKey(s)) strings.put(s, strings.size());
    }
    private static String xml(String s) {
        return s.replaceAll("_x([0-9a-fA-F]{4})_", "_x005F_x$1_").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\r", "&#13;");
    }
    private static void xlsxRow(ZipOutputStream zip, int index, String[] cells, Map<String,Integer> strings) throws IOException {
        write(zip, "<row r=\"" + index + "\">");
        for (int i=0; i<3; i++) write(zip, "<c r=\"" + (char)('A'+i) + index + "\" t=\"s\" s=\"" + (index==1?1:0) + "\"><v>" + strings.get(cells[i]) + "</v></c>");
        write(zip, "</row>");
    }
    private static void entry(ZipOutputStream zip, String name, String body) throws IOException {
        zip.putNextEntry(new ZipEntry(name)); write(zip, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+body); zip.closeEntry();
    }
    private static void write(OutputStream out, String s) throws IOException { out.write(s.getBytes(StandardCharsets.UTF_8)); }
}
