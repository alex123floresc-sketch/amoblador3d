package pe.edu.unaj.amoblador3d.model;

/**
 * Tipos de luz:
 * <ul>
 *   <li>POINT: bombilla, ilumina en todas direcciones desde un punto (intensidad en candelas).</li>
 *   <li>SPOT: foco, cono de luz que apunta a un punto del piso (candelas).</li>
 *   <li>SUN: sol que entra por una ventana, rayos paralelos (lux).</li>
 * </ul>
 */
public enum LightType {
    POINT("point", "Bombilla", "#ffe2b8", 6),
    SPOT("spot", "Foco", "#fff4e0", 25),
    SUN("sun", "Sol", "#fff1d6", 3);

    private final String id;
    private final String label;
    final String defaultColor;
    final double defaultIntensity;

    LightType(String id, String label, String color, double intensity) {
        this.id = id;
        this.label = label;
        this.defaultColor = color;
        this.defaultIntensity = intensity;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public double maxIntensity() {
        return this == SUN ? 20 : 500;
    }

    public String unit() {
        return this == SUN ? "lx" : "cd";
    }

    public static LightType fromId(String id) {
        for (LightType t : values()) if (t.id.equals(id)) return t;
        throw new IllegalArgumentException("Tipo de luz desconocido: " + id);
    }
}
