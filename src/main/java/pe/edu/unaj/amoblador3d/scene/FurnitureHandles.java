package pe.edu.unaj.amoblador3d.scene;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Cursor;
import pe.edu.unaj.amoblador3d.catalog.CatalogEntry;
import pe.edu.unaj.amoblador3d.design.Design;
import pe.edu.unaj.amoblador3d.model.Furniture;
import pe.edu.unaj.amoblador3d.util.Units;

/**
 * Asas del mueble seleccionado en la vista 3D y en la planta: se arrastran para cambiar el
 * ancho (asas de los costados), la profundidad (adelante y atrás), el alto (asa de arriba, solo
 * en 3D) y el giro (asa redonda delante del mueble; se pega cada 15°, Shift para girar libre).
 *
 * <p>Al estirar un lado, el lado opuesto se queda quieto, como al estirar una caja real.
 */
final class FurnitureHandles {
    enum Kind { RIGHT, LEFT, FRONT, BACK, TOP, ROTATE }

    /** Un asa: qué hace y dónde está (en el mundo). */
    record Handle(Kind kind, Point3D at) {}

    /** Distancia (en píxeles) a la que el ratón "agarra" un asa. */
    static final double GRAB = 11;
    /** Distancia del asa de giro al frente del mueble (m). */
    private static final double ROTATE_GAP = 0.5;

    private Kind kind;
    private Furniture item, start;
    private double pressX, pressY;
    private boolean changed;

    /** Punto local del mueble (x a lo ancho, y hacia arriba, z hacia su frente) en el mundo. */
    static Point3D world(Furniture f, double lx, double ly, double lz) {
        double a = Math.toRadians(f.rotation), c = Math.cos(a), s = Math.sin(a);
        return new Point3D(f.x + lx * c + lz * s, ly, f.z - lx * s + lz * c);
    }

    static List<Handle> of(Furniture f, boolean plan) {
        double hw = f.width / 2, hd = f.depth / 2, y = plan ? 0 : 0.02;
        List<Handle> list = new ArrayList<>(List.of(
                new Handle(Kind.RIGHT, world(f, hw, y, 0)),
                new Handle(Kind.LEFT, world(f, -hw, y, 0)),
                new Handle(Kind.FRONT, world(f, 0, y, hd)),
                new Handle(Kind.BACK, world(f, 0, y, -hd)),
                new Handle(Kind.ROTATE, world(f, 0, y, hd + ROTATE_GAP))));
        if (!plan) list.add(new Handle(Kind.TOP, world(f, 0, f.height, 0)));
        return list;
    }

    /** Asa bajo el ratón, o null. */
    static Kind hit(ViewCamera cam, Furniture f, double sx, double sy, boolean plan) {
        Kind best = null;
        double bestD = GRAB;
        for (Handle h : of(f, plan)) {
            Point2D p = cam.project(h.at());
            if (p == null) continue;
            double d = Math.hypot(p.getX() - sx, p.getY() - sy);
            if (d < bestD) {
                bestD = d;
                best = h.kind();
            }
        }
        return best;
    }

    static Cursor cursor(Kind k) {
        return switch (k) {
            case RIGHT, LEFT, FRONT, BACK -> Cursor.MOVE;
            case TOP -> Cursor.V_RESIZE;
            case ROTATE -> Cursor.HAND;
        };
    }

    boolean active() {
        return kind != null;
    }

    Kind kind() {
        return kind;
    }

    void begin(Kind k, Furniture f, double sx, double sy) {
        kind = k;
        item = f;
        start = f.copy();
        pressX = sx;
        pressY = sy;
        changed = false;
    }

    /** Termina el arrastre. Devuelve true si el mueble cambió (para registrarlo en deshacer). */
    boolean end() {
        boolean c = changed;
        kind = null;
        item = start = null;
        return c;
    }

    /**
     * Aplica el arrastre con el ratón en (sx, sy). Si el resultado no es válido (choca o se sale),
     * el mueble se queda como en el último paso válido.
     *
     * @return el texto para la barra de estado ("Ancho: 1,40 m"), o null si no se pudo aplicar
     */
    String drag(Design design, ViewCamera cam, double sx, double sy, boolean free) {
        CatalogEntry e = design.catalog().get(item.type);
        Furniture c = start.copy();
        double a = Math.toRadians(start.rotation), cos = Math.cos(a), sin = Math.sin(a);
        String text;
        boolean keepPosition = true;
        if (kind == Kind.TOP) {
            // Cuántos metros sube el ratón: se proyecta su movimiento sobre la vertical de la pantalla.
            Point2D top = cam.project(world(start, 0, start.height, 0)), up = cam.project(world(start, 0, start.height + 1, 0));
            if (top == null || up == null) return null;
            double vx = up.getX() - top.getX(), vy = up.getY() - top.getY(), len2 = vx * vx + vy * vy;
            if (len2 < 1) return null;
            double t = ((sx - pressX) * vx + (sy - pressY) * vy) / len2;
            c.height = Units.round(clamp(start.height + t, e.heightRange().min(), Math.min(e.heightRange().max(), design.room().height())));
            text = "Alto: " + Units.meters(c.height);
        } else {
            Point3D p = cam.floorPoint(sx, sy);
            if (p == null) return null;
            double dx = p.getX() - start.x, dz = p.getZ() - start.z;
            double lx = dx * cos - dz * sin, lz = dx * sin + dz * cos; // punto en coordenadas del mueble
            double hw = start.width / 2, hd = start.depth / 2;
            switch (kind) {
                case RIGHT, LEFT -> {
                    double w = kind == Kind.RIGHT ? lx + hw : hw - lx;
                    c.width = Units.round(clamp(w, e.widthRange().min(), e.widthRange().max()));
                    double cx = kind == Kind.RIGHT ? -hw + c.width / 2 : hw - c.width / 2;
                    c.x = Units.round(start.x + cx * cos);
                    c.z = Units.round(start.z - cx * sin);
                    text = "Ancho: " + Units.meters(c.width);
                }
                case FRONT, BACK -> {
                    double d = kind == Kind.FRONT ? lz + hd : hd - lz;
                    c.depth = Units.round(clamp(d, e.depthRange().min(), e.depthRange().max()));
                    double cz = kind == Kind.FRONT ? -hd + c.depth / 2 : hd - c.depth / 2;
                    c.x = Units.round(start.x + cz * sin);
                    c.z = Units.round(start.z + cz * cos);
                    text = "Profundidad: " + Units.meters(c.depth);
                }
                default -> { // ROTATE: el frente del mueble apunta hacia el ratón
                    double deg = Math.toDegrees(Math.atan2(dx, dz));
                    double snap = free ? 1 : 15;
                    c.rotation = ((Math.round(deg / snap) * snap) % 360 + 360) % 360;
                    keepPosition = false;
                    text = "Giro: " + Units.number(c.rotation) + "°" + (free ? "" : "   (Shift: giro libre)");
                }
            }
        }
        if (c.x == item.x && c.z == item.z && c.width == item.width && c.depth == item.depth && c.height == item.height
                && c.rotation == item.rotation) {
            return text;
        }
        String err = design.reshape(item, c, keepPosition);
        if (err != null) return text + "   · " + err;
        changed = true;
        return text;
    }

    private static double clamp(double v, double min, double max) {
        return Math.min(Math.max(v, min), max);
    }
}
