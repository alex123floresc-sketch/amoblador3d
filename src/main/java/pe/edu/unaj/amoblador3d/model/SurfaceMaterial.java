package pe.edu.unaj.amoblador3d.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Acabados de paredes y piso. Cada uno tiene un color sugerido (el que toma la superficie al
 * elegirlo) y dice dónde se puede usar: una alfombra en la pared no tiene sentido.
 */
public enum SurfaceMaterial {
    PINTURA("pintura", "Pintura mate", "#f2efe9", true, false),
    SATINADO("satinado", "Pintura satinada", "#e9eef2", true, false),
    PAPEL_TAPIZ("papel-tapiz", "Papel tapiz (rayas)", "#d8c7a8", true, false),
    LADRILLO("ladrillo", "Ladrillo", "#a4553a", true, false),
    PIEDRA("piedra", "Piedra", "#9d9488", true, true),
    CONCRETO("concreto", "Concreto", "#a7a7a2", true, true),
    MADERA("madera", "Madera", "#b07d4f", true, true),
    CERAMICO("ceramico", "Cerámico", "#e4e0d8", true, true),
    MARMOL("marmol", "Mármol", "#ece9e4", true, true),
    PORCELANATO("porcelanato", "Porcelanato", "#c9c3b9", false, true),
    ALFOMBRA("alfombra", "Alfombra", "#7d8a96", false, true);

    private final String id;
    private final String label;
    private final String defaultColor;
    private final boolean wall;
    private final boolean floor;

    SurfaceMaterial(String id, String label, String defaultColor, boolean wall, boolean floor) {
        this.id = id;
        this.label = label;
        this.defaultColor = defaultColor;
        this.wall = wall;
        this.floor = floor;
    }

    /** Texto que se guarda en los archivos y en la base de datos. */
    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public String defaultColor() {
        return defaultColor;
    }

    public boolean allowedOn(boolean isFloor) {
        return isFloor ? floor : wall;
    }

    public static List<SurfaceMaterial> forSurface(boolean isFloor) {
        List<SurfaceMaterial> result = new ArrayList<>();
        for (SurfaceMaterial m : values()) if (m.allowedOn(isFloor)) result.add(m);
        return result;
    }

    public static SurfaceMaterial fromId(String id) {
        for (SurfaceMaterial m : values()) if (m.id.equals(id)) return m;
        throw new IllegalArgumentException("Material desconocido: " + id);
    }
}
