package pe.edu.unaj.amoblador3d.model;

public enum OpeningType {
    WINDOW("window", "Ventana", 1.2, 1.0, 0.9),
    DOOR("door", "Puerta", 0.9, 2.1, 0);

    private final String id;
    private final String label;
    final double defaultWidth;
    final double defaultHeight;
    final double defaultSill;

    OpeningType(String id, String label, double width, double height, double sill) {
        this.id = id;
        this.label = label;
        this.defaultWidth = width;
        this.defaultHeight = height;
        this.defaultSill = sill;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public static OpeningType fromId(String id) {
        for (OpeningType t : values()) if (t.id.equals(id)) return t;
        throw new IllegalArgumentException("Tipo de abertura desconocido: " + id);
    }
}
