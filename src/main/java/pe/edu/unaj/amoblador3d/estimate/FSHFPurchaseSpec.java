package pe.edu.unaj.amoblador3d.estimate;

import pe.edu.unaj.amoblador3d.model.FSHFSurfaceMaterial;

public record FSHFPurchaseSpec(String unit, double coverage, int coats, double waste, double step) {
    private static final FSHFPurchaseSpec PAINT = new FSHFPurchaseSpec("gal", 35, 2, 0, 0.25);
    private static final FSHFPurchaseSpec WALLPAPER = new FSHFPurchaseSpec("rollo", 5.3, 1, 0.10, 1);
    private static final FSHFPurchaseSpec AREA = new FSHFPurchaseSpec("m²", 1, 1, 0.10, 0.1);

    public static FSHFPurchaseSpec of(FSHFSurfaceMaterial m) {
        return switch (m) {
            case PINTURA, SATINADO -> PAINT;
            case PAPEL_TAPIZ -> WALLPAPER;
            default -> AREA;
        };
    }

    public double quantity(double area) {
        if (area <= 0) return 0;
        double raw = area * coats * (1 + waste) / coverage;

        return Math.ceil(raw / step - 1e-9) * step;
    }

    public String explanation() {
        if (coats > 1) return coats + " manos, " + (int) coverage + " m² por galón";
        if (unit.equals("rollo")) return "rollos de " + String.valueOf(coverage).replace('.', ',') + " m², +10 % para empalmes";
        return "+" + Math.round(waste * 100) + " % por cortes";
    }
}
