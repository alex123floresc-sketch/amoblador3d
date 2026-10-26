package pe.edu.unaj.amoblador3d.model;

/**
 * Acabado de una pared, del piso o del techo: material y color.
 * <ul>
 *   <li>id: "piso", "techo" o "pared-norte", "pared-sur"... (fijo, sirve para seleccionarla).</li>
 *   <li>color: "#rrggbb"; tiñe la textura del material.</li>
 * </ul>
 */
public final class Surface {
    public static final String FLOOR_ID = "piso", CEILING_ID = "techo";
    public static final SurfaceMaterial DEFAULT_FLOOR = SurfaceMaterial.MADERA;
    public static final SurfaceMaterial DEFAULT_WALL = SurfaceMaterial.PINTURA;
    public static final SurfaceMaterial DEFAULT_CEILING = SurfaceMaterial.PINTURA;
    private static final String CEILING_COLOR = "#fafafa";

    public final String id;
    public SurfaceMaterial material;
    public String color;

    public Surface(String id, SurfaceMaterial material, String color) {
        this.id = id;
        this.material = material;
        this.color = color;
    }

    public static Surface floor() {
        return new Surface(FLOOR_ID, DEFAULT_FLOOR, DEFAULT_FLOOR.defaultColor());
    }

    /** Cielo raso: se ve en el recorrido (desde las otras vistas taparía la habitación). */
    public static Surface ceiling() {
        return new Surface(CEILING_ID, DEFAULT_CEILING, CEILING_COLOR);
    }

    public static Surface wall(WallSide side) {
        return new Surface(wallId(side), DEFAULT_WALL, DEFAULT_WALL.defaultColor());
    }

    public static String wallId(WallSide side) {
        return "pared-" + side.id();
    }

    public boolean isFloor() {
        return FLOOR_ID.equals(id);
    }

    public boolean isCeiling() {
        return CEILING_ID.equals(id);
    }

    /** La pared a la que pertenece, o null si es el piso o el techo. */
    public WallSide wallSide() {
        return id.startsWith("pared-") ? WallSide.fromId(id.substring("pared-".length())) : null;
    }

    public Surface copy() {
        return new Surface(id, material, color);
    }
}
