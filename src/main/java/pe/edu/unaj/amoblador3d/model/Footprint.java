package pe.edu.unaj.amoblador3d.model;

/**
 * Huella de un objeto en planta (plano XZ): un rectángulo orientado.
 *
 * <p>Para saber si dos rectángulos rotados se tocan se usa el Teorema del Eje Separador
 * (SAT): si existe un eje donde sus proyecciones no se solapan, no chocan. En rectángulos
 * basta con probar los 2 ejes de cada uno (4 en total).
 *
 * @param x     centro en X (m)
 * @param z     centro en Z (m)
 * @param hw    mitad del ancho
 * @param hd    mitad de la profundidad
 * @param angle giro alrededor del eje Y, en radianes
 */
public record Footprint(double x, double z, double hw, double hd, double angle) {
    private static final double EPS = 1e-6;

    public static Footprint of(Furniture f) {
        return new Footprint(f.x, f.z, f.width / 2, f.depth / 2, Math.toRadians(f.rotation));
    }

    public Footprint withCenter(double x, double z) {
        return new Footprint(x, z, hw, hd, angle);
    }

    /** Ejes locales (ancho y profundidad) en coordenadas del mundo. */
    private double[][] axes() {
        double c = Math.cos(angle), s = Math.sin(angle);
        return new double[][] {{c, -s}, {s, c}};
    }

    private double projectedRadius(double nx, double nz) {
        double[][] a = axes();
        return hw * Math.abs(a[0][0] * nx + a[0][1] * nz) + hd * Math.abs(a[1][0] * nx + a[1][1] * nz);
    }

    /** true si se superponen (tocarse justo en el borde NO cuenta). */
    public boolean overlaps(Footprint other) {
        double dx = other.x - x, dz = other.z - z;
        double[][] all = {axes()[0], axes()[1], other.axes()[0], other.axes()[1]};
        for (double[] n : all) {
            double distance = Math.abs(dx * n[0] + dz * n[1]);
            if (distance >= projectedRadius(n[0], n[1]) + other.projectedRadius(n[0], n[1]) - EPS) return false;
        }
        return true;
    }

    /** Mitad del ancho (X) de la caja alineada a los ejes que envuelve el rectángulo. */
    public double halfExtentX() {
        return hw * Math.abs(Math.cos(angle)) + hd * Math.abs(Math.sin(angle));
    }

    /** Mitad del largo (Z) de la caja alineada a los ejes que envuelve el rectángulo. */
    public double halfExtentZ() {
        return hw * Math.abs(Math.sin(angle)) + hd * Math.abs(Math.cos(angle));
    }

    /** true si cabe completo dentro de la habitación [0, width] x [0, length]. */
    public boolean insideRoom(double width, double length) {
        double hx = halfExtentX(), hz = halfExtentZ();
        return x - hx >= -EPS && x + hx <= width + EPS && z - hz >= -EPS && z + hz <= length + EPS;
    }

    /** Mueve el centro para que el rectángulo quede dentro de la habitación (si es posible). */
    public Footprint clampToRoom(double width, double length) {
        return withCenter(clamp(x, halfExtentX(), width - halfExtentX()), clamp(z, halfExtentZ(), length - halfExtentZ()));
    }

    private static double clamp(double v, double min, double max) {
        return min > max ? (min + max) / 2 : Math.min(Math.max(v, min), max);
    }
}
