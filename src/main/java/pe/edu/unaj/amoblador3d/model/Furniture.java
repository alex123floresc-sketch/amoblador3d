package pe.edu.unaj.amoblador3d.model;

/**
 * Mueble colocado en la habitación.
 * <ul>
 *   <li>type: id del catálogo ("cama", "mesa"... o el id de un modelo 3D).</li>
 *   <li>x, z: centro de la base en el piso.</li>
 *   <li>rotation: giro alrededor del eje vertical, en grados (0..359).</li>
 *   <li>color: solo en muebles paramétricos; los modelos 3D traen sus propios colores (null).</li>
 * </ul>
 */
public final class Furniture {
    public String id;
    public String type;
    public double x, z, rotation;
    public double width, depth, height;
    public String color;

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
        return new Furniture(id, type, x, z, rotation, width, depth, height, color);
    }
}
