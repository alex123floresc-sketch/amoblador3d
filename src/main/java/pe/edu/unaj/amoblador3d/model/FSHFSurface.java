package pe.edu.unaj.amoblador3d.model;

public final class FSHFSurface {
    public static final String FLOOR_ID = "piso", CEILING_ID = "techo";
    public static final FSHFSurfaceMaterial DEFAULT_FLOOR = FSHFSurfaceMaterial.MADERA;
    public static final FSHFSurfaceMaterial DEFAULT_WALL = FSHFSurfaceMaterial.PINTURA;
    public static final FSHFSurfaceMaterial DEFAULT_CEILING = FSHFSurfaceMaterial.PINTURA;
    private static final String CEILING_COLOR = "#fafafa";

    public final String id;
    public FSHFSurfaceMaterial material;
    public String color;

    public FSHFSurface(String id, FSHFSurfaceMaterial material, String color) {
        this.id = id;
        this.material = material;
        this.color = color;
    }

    public static FSHFSurface floor() {
        return new FSHFSurface(FLOOR_ID, DEFAULT_FLOOR, DEFAULT_FLOOR.defaultColor());
    }

    public static FSHFSurface ceiling() {
        return new FSHFSurface(CEILING_ID, DEFAULT_CEILING, CEILING_COLOR);
    }

    public static FSHFSurface wall(FSHFWallSide side) {
        return new FSHFSurface(wallId(side), DEFAULT_WALL, DEFAULT_WALL.defaultColor());
    }

    public static String wallId(FSHFWallSide side) {
        return "pared-" + side.id();
    }

    public boolean isFloor() {
        return FLOOR_ID.equals(id);
    }

    public boolean isCeiling() {
        return CEILING_ID.equals(id);
    }

    public FSHFWallSide wallSide() {
        return id.startsWith("pared-") ? FSHFWallSide.fromId(id.substring("pared-".length())) : null;
    }

    public FSHFSurface copy() {
        return new FSHFSurface(id, material, color);
    }
}
