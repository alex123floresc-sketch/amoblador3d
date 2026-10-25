package pe.edu.unaj.amoblador3d.model;

/**
 * Las cuatro paredes. El norte es z = 0 y el oeste es x = 0
 * (ver {@link Project} para el sistema de coordenadas).
 */
public enum WallSide {
    NORTE("norte", "Norte"),
    SUR("sur", "Sur"),
    ESTE("este", "Este"),
    OESTE("oeste", "Oeste");

    private final String id;
    private final String label;

    WallSide(String id, String label) {
        this.id = id;
        this.label = label;
    }

    /** Texto que se guarda en los archivos y en la base de datos. */
    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    /** Norte y sur van a lo largo del eje X; este y oeste, a lo largo de Z. */
    public boolean alongX() {
        return this == NORTE || this == SUR;
    }

    public static WallSide fromId(String id) {
        for (WallSide w : values()) if (w.id.equals(id)) return w;
        throw new IllegalArgumentException("Pared desconocida: " + id);
    }
}
