package pe.edu.unaj.amoblador3d.report;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

final class FSHFPdfLayout implements AutoCloseable {
    static final float MARGIN = 42;
    private static final PDRectangle SIZE = PDRectangle.A4;
    static final float WIDTH = SIZE.getWidth() - 2 * MARGIN;
    private static final Color ACCENT = new Color(0x2f, 0x6f, 0xb8);
    private static final Color HEADER_BG = new Color(0xe8, 0xee, 0xf6);
    private static final Color RULE = new Color(0xc8, 0xce, 0xd6);
    private static final Color MUTED = new Color(0x5a, 0x62, 0x6b);

    final PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    final PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private final PDDocument doc;
    private final Map<Integer, Boolean> encodable = new HashMap<>();
    private PDPageContentStream cs;
    private float y;

    FSHFPdfLayout(PDDocument doc) throws IOException {
        this.doc = doc;
        newPage();
    }

    void newPage() throws IOException {
        if (cs != null) cs.close();
        PDPage page = new PDPage(SIZE);
        doc.addPage(page);
        cs = new PDPageContentStream(doc, page);
        y = SIZE.getHeight() - MARGIN;
    }

    void ensure(float height) throws IOException {
        if (y - height < MARGIN + 16) newPage();
    }

    void space(float points) {
        y -= points;
    }

    void title(String text, String subtitle) throws IOException {
        write(bold, 20, ACCENT, MARGIN, y - 20, text);
        y -= 28;
        if (subtitle != null) {
            write(regular, 10, MUTED, MARGIN, y - 10, subtitle);
            y -= 16;
        }
        rule();
        y -= 10;
    }

    void heading(String text) throws IOException {
        ensure(60);
        y -= 8;
        write(bold, 13, ACCENT, MARGIN, y - 13, text);
        y -= 20;
    }

    void paragraph(String text, float size, Color color) throws IOException {
        for (String line : wrap(text, regular, size, WIDTH)) {
            ensure(size + 4);
            write(regular, size, color, MARGIN, y - size, line);
            y -= size + 4;
        }
    }

    void note(String text) throws IOException {
        paragraph(text, 8.5f, MUTED);
    }

    private void rule() throws IOException {
        cs.setStrokingColor(RULE);
        cs.setLineWidth(0.8f);
        cs.moveTo(MARGIN, y);
        cs.lineTo(MARGIN + WIDTH, y);
        cs.stroke();
    }

    private void write(PDType1Font font, float size, Color color, float x, float baseline, String text) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.setNonStrokingColor(color);
        cs.newLineAtOffset(x, baseline);
        cs.showText(clean(font, text));
        cs.endText();
    }

    String clean(PDType1Font font, String text) {
        StringBuilder sb = new StringBuilder(text.length());
        text.codePoints().forEach(cp -> {
            boolean ok = encodable.computeIfAbsent(cp, c -> {
                try {
                    font.encode(new String(Character.toChars(c)));
                    return true;
                } catch (IOException | IllegalArgumentException e) {
                    return false;
                }
            });
            sb.append(ok ? new String(Character.toChars(cp)) : "?");
        });
        return sb.toString();
    }

    float textWidth(PDType1Font font, float size, String text) throws IOException {
        return font.getStringWidth(clean(font, text)) / 1000 * size;
    }

    List<String> wrap(String text, PDType1Font font, float size, float width) throws IOException {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (textWidth(font, size, candidate) <= width || line.isEmpty()) {
                    line.setLength(0);
                    line.append(candidate);
                } else {
                    lines.add(line.toString());
                    line.setLength(0);
                    line.append(word);
                }
            }
            lines.add(line.toString());
        }
        return lines;
    }

    enum Align { LEFT, RIGHT }

    record Row(String[] cells, boolean strong) {
        static Row of(String... cells) {
            return new Row(cells, false);
        }

        static Row strong(String... cells) {
            return new Row(cells, true);
        }
    }

    void table(String[] header, float[] widths, Align[] align, List<Row> rows) throws IOException {
        float size = 9, pad = 4, leading = size + 2.5f;
        float[] w = new float[widths.length];
        for (int i = 0; i < w.length; i++) w[i] = widths[i] * WIDTH;
        ensure(60);
        drawRow(header, w, align, bold, size, pad, leading, HEADER_BG);
        for (Row row : rows) {
            float h = rowHeight(row.cells, w, row.strong ? bold : regular, size, pad, leading);
            if (y - h < MARGIN + 16) {
                newPage();
                drawRow(header, w, align, bold, size, pad, leading, HEADER_BG);
            }
            drawRow(row.cells, w, align, row.strong ? bold : regular, size, pad, leading, row.strong ? HEADER_BG : null);
        }
        y -= 6;
    }

    private float rowHeight(String[] cells, float[] w, PDType1Font font, float size, float pad, float leading) throws IOException {
        int lines = 1;
        for (int i = 0; i < cells.length; i++) lines = Math.max(lines, wrap(cells[i], font, size, w[i] - 2 * pad).size());
        return lines * leading + 2 * pad;
    }

    private void drawRow(String[] cells, float[] w, Align[] align, PDType1Font font, float size, float pad,
                         float leading, Color background) throws IOException {
        float h = rowHeight(cells, w, font, size, pad, leading);
        if (background != null) {
            cs.setNonStrokingColor(background);
            cs.addRect(MARGIN, y - h, WIDTH, h);
            cs.fill();
        }
        float x = MARGIN;
        for (int i = 0; i < cells.length; i++) {
            float baseline = y - pad - size + 1;
            for (String line : wrap(cells[i], font, size, w[i] - 2 * pad)) {
                float tx = align[i] == Align.RIGHT ? x + w[i] - pad - textWidth(font, size, line) : x + pad;
                write(font, size, Color.BLACK, tx, baseline, line);
                baseline -= leading;
            }
            x += w[i];
        }
        y -= h;
        cs.setStrokingColor(RULE);
        cs.setLineWidth(0.5f);
        cs.moveTo(MARGIN, y);
        cs.lineTo(MARGIN + WIDTH, y);
        cs.stroke();
    }

    void imageSection(String title, byte[] png, String name, float maxHeight) throws IOException {
        PDImageXObject img = PDImageXObject.createFromByteArray(doc, png, name);
        float scale = Math.min(WIDTH / img.getWidth(), maxHeight / img.getHeight());
        float w = img.getWidth() * scale, h = img.getHeight() * scale;
        ensure(h + 36);
        heading(title);
        cs.drawImage(img, MARGIN + (WIDTH - w) / 2, y - h, w, h);
        y -= h + 8;
    }

    void footer(String left) throws IOException {
        cs.close();
        cs = null;
        int n = doc.getNumberOfPages();
        for (int i = 0; i < n; i++) {
            PDPage page = doc.getPage(i);
            try (PDPageContentStream footer = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                PDPageContentStream saved = cs;
                cs = footer;
                write(regular, 8, MUTED, MARGIN, MARGIN - 14, left);
                String right = "Página " + (i + 1) + " de " + n;
                write(regular, 8, MUTED, MARGIN + WIDTH - textWidth(regular, 8, right), MARGIN - 14, right);
                cs = saved;
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (cs != null) cs.close();
        cs = null;
    }
}
