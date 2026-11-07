package pe.edu.unaj.amoblador3d.model;

public enum FSHFWallSide {
    NORTE("norte", "Norte"),
    SUR("sur", "Sur"),
    ESTE("este", "Este"),
    OESTE("oeste", "Oeste");

    private final String id;
    private final String label;

    FSHFWallSide(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public boolean alongX() {
        return this == NORTE || this == SUR;
    }

    public static FSHFWallSide fromId(String id) {
        for (FSHFWallSide w : values()) if (w.id.equals(id)) return w;
        throw new IllegalArgumentException("Pared desconocida: " + id);
    }
}
