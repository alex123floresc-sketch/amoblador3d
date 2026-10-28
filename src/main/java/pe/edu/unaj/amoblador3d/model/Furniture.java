package pe.edu.unaj.amoblador3d.model;

/**
 * Mueble colocado en la habitación.
 * <ul>
 *   <li>type: id del catálogo ("cama", "mesa"... o el id de un modelo 3D o de un diseño propio).</li>
 *   <li>x, z: centro de la base en el piso.</li>
 *   <li>rotation: giro alrededor del eje vertical, en grados (0..359).</li>
 *   <li>color: color principal. En los modelos 3D es un tinte opcional (null = sus colores originales).</li>
 *   <li>color2: color secundario (frazada, cojines, frentes de cajones...); null = automático.</li>
 *   <li>finish: acabado (mate, satinado...); null = el típico de ese mueble.</li>
 *   <li>locked: bloqueado, no se puede mover, girar, cambiar ni eliminar hasta desbloquearlo.</li>
 * </ul>
 */
public final class Furniture {
    public String id;
    public String type;
    public double x, z, rotation;
    public double width, depth, height;
    public String color;
    public String color2;
    public Finish finish;
    public boolean locked;

    public Furniture(String id, String type, double x, double z, double rotation,
                     double width, double depth, double height, String color) {
        this.id = id;
        this.type = type;
        this.x = x;
        this.z = z;
        this.rotation = rotation;
        this.width = width;
        this.depth = depth;
        this.height = height;
        this.color = color;
    }

    public Furniture copy() {
        Furniture f = new Furniture(id, type, x, z, rotation, width, depth, height, color);
        f.color2 = color2;
        f.finish = finish;
        f.locked = locked;
        return f;
    }

    /** Firma de lo que cambia su forma o su aspecto (no la posición): si cambia, hay que reconstruirlo. */
    public String appearance() {
        return type + "|" + width + "|" + depth + "|" + height + "|" + color + "|" + color2 + "|" + finish;
    }
}
