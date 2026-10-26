package pe.edu.unaj.amoblador3d.model;

/**
 * Zócalo: la franja al pie de las paredes (y de las columnas), cortada en las puertas.
 * Con alto 0 no hay zócalo.
 */
public final class Baseboard {
    /** Id fijo para seleccionarlo como cualquier otro elemento. */
    public static final String ID = "zocalo";
    public static final double DEFAULT_HEIGHT = 0.08, MAX_HEIGHT = 0.2;
    public static final String DEFAULT_COLOR = "#f7f5f0";

    public double height = DEFAULT_HEIGHT;
    public String color = DEFAULT_COLOR;

    public Baseboard copy() {
        Baseboard b = new Baseboard();
        b.height = height;
        b.color = color;
        return b;
    }

    public boolean isDefault() {
        return height == DEFAULT_HEIGHT && DEFAULT_COLOR.equalsIgnoreCase(color);
    }
}
