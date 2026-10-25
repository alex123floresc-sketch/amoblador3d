package pe.edu.unaj.amoblador3d.model;

import pe.edu.unaj.amoblador3d.util.Units;

/**
 * Medidas INTERIORES de la habitación (el espacio libre). Los muros crecen hacia afuera.
 */
public record RoomDims(double width, double length, double height, double wallThickness) {
    public static final RoomDims DEFAULT = new RoomDims(4, 3.5, 2.5, 0.15);

    public static final double MIN_SIDE = 1, MAX_SIDE = 30;
    public static final double MIN_HEIGHT = 2, MAX_HEIGHT = 6;
    public static final double MIN_WALL = 0.05, MAX_WALL = 0.5;

    /** Devuelve un mensaje de error o null si las medidas son válidas. */
    public String validate() {
        String e = Units.checkRange("El ancho", width, MIN_SIDE, MAX_SIDE);
        if (e == null) e = Units.checkRange("El largo", length, MIN_SIDE, MAX_SIDE);
        if (e == null) e = Units.checkRange("El alto", height, MIN_HEIGHT, MAX_HEIGHT);
        if (e == null) e = Units.checkRange("El grosor de muro", wallThickness, MIN_WALL, MAX_WALL);
        return e;
    }

    public RoomDims rounded() {
        return new RoomDims(Units.round(width), Units.round(length), Units.round(height), Units.round(wallThickness));
    }

    /** Largo interior de una pared. */
    public double wallLength(WallSide wall) {
        return wall.alongX() ? width : length;
    }
}
