package pe.edu.unaj.amoblador3d.scene;

import javafx.geometry.Point3D;
import javafx.scene.paint.Material;
import javafx.scene.shape.Box;
import javafx.scene.shape.MeshView;
import pe.edu.unaj.amoblador3d.model.RoomDims;
import pe.edu.unaj.amoblador3d.model.WallSide;

/**
 * Coordenadas "de pared" para construir muros y aberturas sin pensar en su orientación:
 * <ul>
 *   <li>a: distancia a lo largo de la pared desde la esquina interior (el mismo "offset" de las aberturas).</li>
 *   <li>y: altura.</li>
 *   <li>d: profundidad dentro del muro, 0 = cara interior, t = cara exterior (negativo = dentro de la habitación).</li>
 * </ul>
 */
record WallFrame(WallSide side, double width, double length, double thickness) {

    static WallFrame of(WallSide side, RoomDims r) {
        return new WallFrame(side, r.width(), r.length(), r.wallThickness());
    }

    /** Norte y sur cubren también las esquinas (ancho + 2 grosores); este y oeste van entre ellas. */
    double start() {
        return side.alongX() ? -thickness : 0;
    }

    double end() {
        return side.alongX() ? width + thickness : length;
    }

    /** Punto del mundo. */
    Point3D point(double a, double y, double d) {
        return switch (side) {
            case NORTE -> new Point3D(a, y, -d);
            case SUR -> new Point3D(a, y, length + d);
            case OESTE -> new Point3D(-d, y, a);
            case ESTE -> new Point3D(width + d, y, a);
        };
    }

    /** Vector unitario que apunta desde la pared hacia el interior. */
    Point3D interiorNormal() {
        return switch (side) {
            case NORTE -> new Point3D(0, 0, 1);
            case SUR -> new Point3D(0, 0, -1);
            case OESTE -> new Point3D(1, 0, 0);
            case ESTE -> new Point3D(-1, 0, 0);
        };
    }

    /** Caja que ocupa [a0, a1] x [y0, y1] x [d0, d1]. */
    Box box(double a0, double a1, double y0, double y1, double d0, double d1, Material material) {
        double along = a1 - a0, depth = d1 - d0;
        Box b = side.alongX() ? new Box(along, y1 - y0, depth) : new Box(depth, y1 - y0, along);
        Point3D c = point((a0 + a1) / 2, (y0 + y1) / 2, (d0 + d1) / 2);
        b.setTranslateX(c.getX());
        b.setTranslateY(c.getY());
        b.setTranslateZ(c.getZ());
        b.setMaterial(material);
        return b;
    }

    /** Igual que {@link #box}, pero con la textura del material a escala real (ver {@link Meshes}). */
    MeshView texturedBox(double a0, double a1, double y0, double y1, double d0, double d1, double tile, Material material) {
        Point3D p0 = point(a0, y0, d0), p1 = point(a1, y1, d1);
        return Meshes.box(Math.min(p0.getX(), p1.getX()), Math.max(p0.getX(), p1.getX()), y0, y1,
                Math.min(p0.getZ(), p1.getZ()), Math.max(p0.getZ(), p1.getZ()), tile, material);
    }
}
