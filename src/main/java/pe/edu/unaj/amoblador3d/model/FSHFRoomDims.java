package pe.edu.unaj.amoblador3d.model;

import pe.edu.unaj.amoblador3d.util.FSHFUnits;

/**
 * Medidas INTERIORES de la habitación (el espacio libre). Los muros crecen hacia afuera.
 */
public record FSHFRoomDims(double width, double length, double height, double wallThickness) {
    public static final FSHFRoomDims DEFAULT = new FSHFRoomDims(4, 3.5, 2.5, 0.15);

    public static final double MIN_SIDE = 1, MAX_SIDE = 30;
    public static final double MIN_HEIGHT = 2, MAX_HEIGHT = 6;
    public static final double MIN_WALL = 0.05, MAX_WALL = 0.5;

    /** Devuelve un mensaje de error o null si las medidas son válidas. */
    public String validate() {
        String e = FSHFUnits.checkRange("El ancho", width, MIN_SIDE, MAX_SIDE);
        if (e == null) e = FSHFUnits.checkRange("El largo", length, MIN_SIDE, MAX_SIDE);
        if (e == null) e = FSHFUnits.checkRange("El alto", height, MIN_HEIGHT, MAX_HEIGHT);
        if (e == null) e = FSHFUnits.checkRange("El grosor de muro", wallThickness, MIN_WALL, MAX_WALL);
        return e;
    }

    public FSHFRoomDims rounded() {
        return new FSHFRoomDims(FSHFUnits.round(width), FSHFUnits.round(length), FSHFUnits.round(height), FSHFUnits.round(wallThickness));
    }

    /** Largo interior de una pared. */
    public double wallLength(FSHFWallSide wall) {
        return wall.alongX() ? width : length;
    }
}
