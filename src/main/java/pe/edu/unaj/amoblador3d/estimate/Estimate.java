package pe.edu.unaj.amoblador3d.estimate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import pe.edu.unaj.amoblador3d.catalog.Catalog;
import pe.edu.unaj.amoblador3d.design.Editors;
import pe.edu.unaj.amoblador3d.model.Column;
import pe.edu.unaj.amoblador3d.model.Furniture;
import pe.edu.unaj.amoblador3d.model.Project;
import pe.edu.unaj.amoblador3d.model.Surface;
import pe.edu.unaj.amoblador3d.model.SurfaceMaterial;
import pe.edu.unaj.amoblador3d.model.WallSide;
import pe.edu.unaj.amoblador3d.util.Units;

/**
 * Presupuesto del proyecto: cuánto comprar de cada acabado y cuánto cuesta, más los muebles.
 *
 * <p>Las paredes con el mismo material y color se suman en una sola línea (se compra la misma
 * pintura para todas). El piso va aparte aunque use el mismo material que una pared: un piso de
 * madera y un revestimiento de madera son productos distintos.
 */
public record Estimate(Quantities quantities, List<Line> lines) {

    public enum Section {
        FINISHES("Acabados"), EXTRAS("Complementos"), FURNITURE("Muebles");

        private final String label;

        Section(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /**
     * Una línea del presupuesto.
     *
     * @param concept  qué se compra ("Pintura mate · Blanco hueso")
     * @param detail   de dónde sale la cantidad ("Paredes norte, sur: 18,40 m², 2 manos...")
     * @param priceKey precio que se puede editar para esta línea
     */
    public record Line(Section section, String concept, String detail, double quantity, String unit,
                       double unitPrice, PriceList.Key priceKey) {
        public double subtotal() {
            return Math.round(quantity * unitPrice * 100) / 100.0;
        }
    }

    public double total() {
        double t = 0;
        for (Line l : lines) t += l.subtotal();
        return Math.round(t * 100) / 100.0;
    }

    public double total(Section section) {
        double t = 0;
        for (Line l : lines) if (l.section == section) t += l.subtotal();
        return Math.round(t * 100) / 100.0;
    }

    private record SurfaceGroup(boolean floor, SurfaceMaterial material, String color) {}

    public static Estimate of(Project p, Catalog catalog, PriceList prices) {
        Quantities q = Quantities.of(p);
        List<Line> lines = new ArrayList<>();

        // Paredes, techo y caras de columnas con el mismo acabado se compran juntos.
        Map<SurfaceGroup, List<String>> walls = new LinkedHashMap<>(), others = new LinkedHashMap<>();
        Map<SurfaceGroup, Double> areas = new LinkedHashMap<>();
        SurfaceGroup floor = new SurfaceGroup(true, p.floor.material, p.floor.color);
        areas.put(floor, q.floorArea());
        for (WallSide side : WallSide.values()) {
            Surface w = p.walls.get(side);
            SurfaceGroup g = new SurfaceGroup(false, w.material, w.color);
            walls.computeIfAbsent(g, k -> new ArrayList<>()).add(side.id());
            areas.merge(g, q.wallNet().get(side), Double::sum);
        }
        SurfaceGroup ceiling = new SurfaceGroup(false, p.ceiling.material, p.ceiling.color);
        others.computeIfAbsent(ceiling, k -> new ArrayList<>()).add("techo");
        areas.merge(ceiling, q.floorArea(), Double::sum);
        for (Quantities.ColumnFaces f : q.columnFaces()) {
            if (f.area() <= 0) continue;
            SurfaceGroup g = new SurfaceGroup(false, f.column().material, f.column().color);
            others.computeIfAbsent(g, k -> new ArrayList<>()).add(columnName(f.column()));
            areas.merge(g, f.area(), Double::sum);
        }
        for (Map.Entry<SurfaceGroup, Double> e : areas.entrySet()) {
            SurfaceGroup g = e.getKey();
            double area = Units.round(e.getValue());
            if (area <= 0) continue;
            PurchaseSpec spec = PurchaseSpec.of(g.material);
            String place = g.floor ? "Piso" : place(walls.getOrDefault(g, List.of()), others.getOrDefault(g, List.of()));
            String detail = place + ": " + Units.number(area) + " m² · " + spec.explanation();
            PriceList.Key key = PriceList.Key.material(g.material);
            lines.add(new Line(Section.FINISHES, g.material.label() + " · " + Editors.colorName(g.color), detail, spec.quantity(area),
                    spec.unit(), prices.get(key), key));
        }

        double baseboard = Math.ceil(q.baseboard() * 10 - 1e-9) / 10;
        if (baseboard > 0 && p.baseboard.height > 0) {
            String around = q.columnFaces().isEmpty() ? "" : ", rodeando las columnas";
            lines.add(new Line(Section.EXTRAS, "Zócalo de " + Units.number(p.baseboard.height * 100) + " cm",
                    "Perímetro " + Units.number(Units.round(q.perimeter())) + " m" + around + ", menos el ancho de las puertas",
                    baseboard, "m", prices.get(PriceList.BASEBOARD), PriceList.BASEBOARD));
        }

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Furniture f : p.furniture) counts.merge(f.type, 1, Integer::sum);
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            PriceList.Key key = PriceList.Key.furniture(e.getKey());
            lines.add(new Line(Section.FURNITURE, catalog.get(e.getKey()).name(), "", e.getValue(), "und",
                    prices.get(key), key));
        }
        return new Estimate(q, List.copyOf(lines));
    }

    /** "Pared norte", "Paredes norte, sur, techo, columna 1", "Techo"... */
    private static String place(List<String> walls, List<String> others) {
        String text = walls.isEmpty() ? "" : (walls.size() == 1 ? "Pared " : "Paredes ") + String.join(", ", walls);
        if (!others.isEmpty()) {
            String rest = String.join(", ", others);
            text = text.isEmpty() ? Character.toUpperCase(rest.charAt(0)) + rest.substring(1) : text + ", " + rest;
        }
        return text;
    }

    private static String columnName(Column c) {
        if (c.kind == Column.Kind.NOTCH) return "recorte " + c.corner.id();
        return c.kind.label().toLowerCase() + " " + c.id.substring(c.id.lastIndexOf('-') + 1);
    }
}
