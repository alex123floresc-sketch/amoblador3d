package pe.edu.unaj.amoblador3d.model;

/**
 * Ventana o puerta sobre una pared (medidas en metros).
 * <ul>
 *   <li>offset: distancia desde la esquina interior hasta el borde de la abertura.
 *       En norte/sur se mide a lo largo de X (desde el oeste); en este/oeste a lo largo de Z (desde el norte).</li>
 *   <li>sill: altura del alféizar (del piso al borde inferior). En puertas es 0.</li>
 * </ul>
 */
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

    /** Abertura nueva con las medidas por defecto de su tipo (falta ubicarla). */
    public static FSHFOpening create(String id, FSHFOpeningType type) {
        return new FSHFOpening(id, type, FSHFWallSide.NORTE, 0, type.defaultWidth, type.defaultHeight, type.defaultSill);
    }

    public FSHFOpening copy() {
        return new FSHFOpening(id, type, wall, offset, width, height, sill);
    }
}
