package pe.edu.unaj.amoblador3d.estimate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import pe.edu.unaj.amoblador3d.catalog.Catalog;
import pe.edu.unaj.amoblador3d.design.Editors;
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

        Map<SurfaceGroup, List<String>> names = new LinkedHashMap<>();
        Map<SurfaceGroup, Double> areas = new LinkedHashMap<>();
        SurfaceGroup floor = new SurfaceGroup(true, p.floor.material, p.floor.color);
        names.put(floor, new ArrayList<>(List.of("Piso")));
        areas.put(floor, q.floorArea());
        for (WallSide side : WallSide.values()) {
            Surface w = p.walls.get(side);
            SurfaceGroup g = new SurfaceGroup(false, w.material, w.color);
            names.computeIfAbsent(g, k -> new ArrayList<>()).add(side.id());
            areas.merge(g, q.wallNet().get(side), Double::sum);
        }
        for (Map.Entry<SurfaceGroup, Double> e : areas.entrySet()) {
            SurfaceGroup g = e.getKey();
            double area = Units.round(e.getValue());
            PurchaseSpec spec = PurchaseSpec.of(g.material);
            List<String> where = names.get(g);
            String place = g.floor ? "Piso" : (where.size() == 1 ? "Pared " : "Paredes ") + String.join(", ", where);
            String detail = place + ": " + Units.number(area) + " m² · " + spec.explanation();
            PriceList.Key key = PriceList.Key.material(g.material);
            lines.add(new Line(Section.FINISHES, g.material.label() + " · " + Editors.colorName(g.color), detail, spec.quantity(area),
                    spec.unit(), prices.get(key), key));
        }

        double baseboard = Math.ceil(q.baseboard() * 10 - 1e-9) / 10;
        if (baseboard > 0) {
            lines.add(new Line(Section.EXTRAS, "Zócalo", "Perímetro " + Units.number(q.perimeter())
                    + " m menos el ancho de las puertas", baseboard, "m", prices.get(PriceList.BASEBOARD), PriceList.BASEBOARD));
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
}
