package pe.edu.unaj.amoblador3d.estimate;

import pe.edu.unaj.amoblador3d.model.SurfaceMaterial;

/**
 * Cómo se compra un acabado y cuánto hace falta para cubrir un área.
 *
 * <ul>
 *   <li>Pinturas: por galón. Un galón de látex cubre unos 35 m² por mano y se dan 2 manos.
 *       Se redondea hacia arriba al cuarto de galón (la presentación más pequeña).</li>
 *   <li>Papel tapiz: por rollo de 5,3 m² (0,53 x 10 m), con 10 % extra para empalmar el dibujo.</li>
 *   <li>El resto (cerámico, madera, piedra...): por m², con 10 % extra por cortes y piezas rotas.</li>
 * </ul>
 *
 * @param unit     unidad de compra ("gal", "rollo", "m²")
 * @param coverage m² que cubre una unidad en una mano
 * @param coats    número de manos
 * @param waste    fracción extra por desperdicio (0,10 = 10 %)
 * @param step     la cantidad se redondea hacia arriba a un múltiplo de este valor
 */
public record PurchaseSpec(String unit, double coverage, int coats, double waste, double step) {
    private static final PurchaseSpec PAINT = new PurchaseSpec("gal", 35, 2, 0, 0.25);
    private static final PurchaseSpec WALLPAPER = new PurchaseSpec("rollo", 5.3, 1, 0.10, 1);
    private static final PurchaseSpec AREA = new PurchaseSpec("m²", 1, 1, 0.10, 0.1);

    public static PurchaseSpec of(SurfaceMaterial m) {
        return switch (m) {
            case PINTURA, SATINADO -> PAINT;
            case PAPEL_TAPIZ -> WALLPAPER;
            default -> AREA;
        };
    }

    /** Cantidad a comprar para cubrir {@code area} m². */
    public double quantity(double area) {
        if (area <= 0) return 0;
        double raw = area * coats * (1 + waste) / coverage;
        // El 1e-9 evita que 2,0000000001 por redondeo binario se convierta en 2,25.
        return Math.ceil(raw / step - 1e-9) * step;
    }

    /** Explicación del cálculo para el detalle del presupuesto: "2 manos", "+10 % por cortes"... */
    public String explanation() {
        if (coats > 1) return coats + " manos, " + (int) coverage + " m² por galón";
        if (unit.equals("rollo")) return "rollos de " + String.valueOf(coverage).replace('.', ',') + " m², +10 % para empalmes";
        return "+" + Math.round(waste * 100) + " % por cortes";
    }
}
