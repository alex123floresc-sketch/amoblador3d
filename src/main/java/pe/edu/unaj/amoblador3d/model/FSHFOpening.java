package pe.edu.unaj.amoblador3d.model;

public final class FSHFOpening {
    public String id;
    public FSHFOpeningType type;
    public FSHFWallSide wall;
    public double offset;
    public double width;
    public double height;
    public double sill;

    public FSHFOpening(String id, FSHFOpeningType type, FSHFWallSide wall, double offset, double width, double height, double sill) {
        this.id = id;
        this.type = type;
        this.wall = wall;
        this.offset = offset;
        this.width = width;
        this.height = height;
        this.sill = sill;
    }

    public static FSHFOpening create(String id, FSHFOpeningType type) {
        return new FSHFOpening(id, type, FSHFWallSide.NORTE, 0, type.defaultWidth, type.defaultHeight, type.defaultSill);
    }

    public FSHFOpening copy() {
        return new FSHFOpening(id, type, wall, offset, width, height, sill);
    }
}
