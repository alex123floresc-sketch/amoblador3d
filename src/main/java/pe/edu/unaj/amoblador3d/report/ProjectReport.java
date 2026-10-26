package pe.edu.unaj.amoblador3d.report;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import pe.edu.unaj.amoblador3d.estimate.Estimate;
import pe.edu.unaj.amoblador3d.estimate.Quantities;
import pe.edu.unaj.amoblador3d.model.Furniture;
import pe.edu.unaj.amoblador3d.model.Opening;
import pe.edu.unaj.amoblador3d.model.Project;
import pe.edu.unaj.amoblador3d.model.RoomDims;
import pe.edu.unaj.amoblador3d.model.WallSide;
import pe.edu.unaj.amoblador3d.report.PdfLayout.Align;
import pe.edu.unaj.amoblador3d.report.PdfLayout.Row;
import pe.edu.unaj.amoblador3d.util.Units;

/**
 * Informe en PDF para entregar: datos de la habitación, la planta con cotas, la vista 3D,
 * la lista de muebles, ventanas y puertas, y el metrado con el presupuesto.
 *
 * <p>No depende de JavaFX: recibe las imágenes ya convertidas a PNG.
 *
 * @param planPng  planta 2D (puede ser null)
 * @param viewPng  vista 3D (puede ser null)
 * @param labels   nombre visible de cada elemento ("Cama 1")
 */
public record ProjectReport(String name, Project project, Estimate estimate, byte[] planPng, byte[] viewPng,
                            Function<Object, String> labels) {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Units.LOCALE);

    public void write(Path file) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDDocumentInformation info = doc.getDocumentInformation();
            info.setTitle(name);
            info.setCreator("Amoblador3D");
            try (PdfLayout pdf = new PdfLayout(doc)) {
                pdf.title(name, "Informe de diseño · " + LocalDateTime.now().format(DATE));
                room(pdf);
                if (planPng != null) {
                    pdf.heading("Planta");
                    pdf.image(planPng, "planta", 340);
                }
                if (viewPng != null) {
                    pdf.heading("Vista 3D");
                    pdf.image(viewPng, "vista", 300);
                }
                openings(pdf);
                furniture(pdf);
                budget(pdf);
                pdf.footer("Amoblador3D · " + name);
            }
            doc.save(file.toFile());
        }
    }

    private void room(PdfLayout pdf) throws IOException {
        RoomDims r = project.room;
        Quantities q = estimate.quantities();
        pdf.heading("Habitación");
        List<Row> rows = new ArrayList<>();
        rows.add(Row.of("Medidas interiores (ancho × largo × alto)",
                Units.number(r.width()) + " × " + Units.number(r.length()) + " × " + Units.number(r.height()) + " m"));
        rows.add(Row.of("Grosor de muro", Units.meters(r.wallThickness())));
        rows.add(Row.of("Área del piso", area(q.floorArea())));
        rows.add(Row.of("Perímetro", Units.meters(q.perimeter())));
        rows.add(Row.of("Área de paredes sin ventanas ni puertas", area(q.wallNetTotal())));
        rows.add(Row.of("Área de ventanas y puertas", area(q.openingsArea())));
        rows.add(Row.of("Volumen", Units.number(Units.round(q.volume())) + " m³"));
        rows.add(Row.of("Piso", project.floor.material.label()));
        for (WallSide side : WallSide.values()) {
            rows.add(Row.of("Pared " + side.id(), project.walls.get(side).material.label() + " (" + area(q.wallNet().get(side)) + ")"));
        }
        pdf.table(new String[] {"Dato", "Valor"}, new float[] {0.55f, 0.45f}, new Align[] {Align.LEFT, Align.LEFT}, rows);
    }

    private void openings(PdfLayout pdf) throws IOException {
        if (project.openings.isEmpty()) return;
        pdf.heading("Ventanas y puertas");
        List<Row> rows = new ArrayList<>();
        for (Opening o : project.openings) {
            rows.add(Row.of(labels.apply(o), Units.meters(o.offset), Units.number(o.width) + " × " + Units.number(o.height) + " m",
                    o.sill > 0 ? Units.meters(o.sill) : "—"));
        }
        pdf.table(new String[] {"Elemento", "Desde la esquina", "Ancho × alto", "Alféizar"},
                new float[] {0.4f, 0.2f, 0.25f, 0.15f}, new Align[] {Align.LEFT, Align.RIGHT, Align.RIGHT, Align.RIGHT}, rows);
    }

    private void furniture(PdfLayout pdf) throws IOException {
        pdf.heading("Muebles");
        if (project.furniture.isEmpty()) {
            pdf.note("No hay muebles en la habitación.");
            return;
        }
        List<Row> rows = new ArrayList<>();
        for (Furniture f : project.furniture) {
            rows.add(Row.of(labels.apply(f),
                    Units.number(f.width) + " × " + Units.number(f.depth) + " × " + Units.number(f.height) + " m",
                    "(" + Units.number(f.x) + "; " + Units.number(f.z) + ")", Units.number(f.rotation) + "°"));
        }
        pdf.table(new String[] {"Mueble", "Ancho × prof. × alto", "Centro (X; Z) m", "Giro"},
                new float[] {0.36f, 0.28f, 0.22f, 0.14f}, new Align[] {Align.LEFT, Align.RIGHT, Align.RIGHT, Align.RIGHT}, rows);
        pdf.note("Coordenadas desde la esquina interior noroeste: X hacia el este, Z hacia el sur.");
    }

    private void budget(PdfLayout pdf) throws IOException {
        pdf.heading("Metrado y presupuesto");
        List<Row> rows = new ArrayList<>();
        for (Estimate.Section section : Estimate.Section.values()) {
            List<Estimate.Line> lines = estimate.lines().stream().filter(l -> l.section() == section).toList();
            if (lines.isEmpty()) continue;
            for (Estimate.Line l : lines) {
                String concept = l.detail().isEmpty() ? l.concept() : l.concept() + "\n" + l.detail();
                rows.add(Row.of(concept, Units.number(l.quantity()), l.unit(), Units.money(l.unitPrice()), Units.money(l.subtotal())));
            }
            rows.add(Row.strong("Subtotal " + section.label().toLowerCase(), "", "", "", Units.money(estimate.total(section))));
        }
        rows.add(Row.strong("TOTAL", "", "", "", Units.money(estimate.total())));
        pdf.table(new String[] {"Concepto", "Cantidad", "Unidad", "P. unitario", "Subtotal"},
                new float[] {0.47f, 0.11f, 0.1f, 0.15f, 0.17f},
                new Align[] {Align.LEFT, Align.RIGHT, Align.LEFT, Align.RIGHT, Align.RIGHT}, rows);
        pdf.note("Precios referenciales en soles (S/), editables en Proyecto > Metrado y presupuesto. "
                + "No incluyen mano de obra ni transporte. Las cantidades ya incluyen el desperdicio indicado.");
    }

    private static String area(double value) {
        return Units.number(Units.round(value)) + " m²";
    }
}
