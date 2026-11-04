package pe.edu.unaj.amoblador3d.model;

import java.util.List;

/** Ventana o algún tipo de puerta. Todas las puertas van del piso hacia arriba (alféizar 0). */
public enum OpeningType {
    WINDOW("window", "Ventana", 1.2, 1.0, 0.9),
    DOOR("door", "Puerta", 0.9, 2.1, 0),
    ENTRY("entry", "Puerta principal", 1.0, 2.2, 0),
    DOUBLE("double", "Puerta doble", 1.4, 2.2, 0),
    SLIDING("sliding", "Puerta corrediza", 1.8, 2.1, 0),
    GARAGE("garage", "Portón de cochera", 2.6, 2.2, 0),
    PASSAGE("passage", "Vano sin puerta", 0.9, 2.1, 0);

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

    public double defaultWidth() {
        return defaultWidth;
    }

    public double defaultHeight() {
        return defaultHeight;
    }

    /** Todo lo que no es ventana: se pasa por ahí, va desde el piso y su paso no se tapa con muebles. */
    public boolean isDoor() {
        return this != WINDOW;
    }

    /** Puertas con hoja que gira (en planta se dibuja el arco de giro). */
    public boolean swings() {
        return this == DOOR || this == ENTRY || this == DOUBLE;
    }

    /** Los tipos de puerta, en el orden en que se ofrecen. */
    public static List<OpeningType> doors() {
        return List.of(DOOR, ENTRY, DOUBLE, SLIDING, GARAGE, PASSAGE);
    }

    public static OpeningType fromId(String id) {
        for (OpeningType t : values()) if (t.id.equals(id)) return t;
        throw new IllegalArgumentException("Tipo de abertura desconocido: " + id);
    }
}
