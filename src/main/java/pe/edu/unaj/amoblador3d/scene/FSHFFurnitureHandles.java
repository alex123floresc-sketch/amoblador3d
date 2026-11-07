package pe.edu.unaj.amoblador3d.scene;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Cursor;
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalogEntry;
import pe.edu.unaj.amoblador3d.design.FSHFDesign;
import pe.edu.unaj.amoblador3d.model.FSHFFurniture;
import pe.edu.unaj.amoblador3d.util.FSHFUnits;

final class FSHFFurnitureHandles {
    enum Kind { RIGHT, LEFT, FRONT, BACK, TOP, ROTATE }

    record Handle(Kind kind, Point3D at) {}

    static final double GRAB = 11;
    private static final double ROTATE_GAP = 0.5;

    private Kind kind;
    private FSHFFurniture item, start;
    private double pressX, pressY;
    private boolean changed;

    static Point3D world(FSHFFurniture f, double lx, double ly, double lz) {
        double a = Math.toRadians(f.rotation), c = Math.cos(a), s = Math.sin(a);
        return new Point3D(f.x + lx * c + lz * s, ly, f.z - lx * s + lz * c);
    }

    static List<Handle> of(FSHFFurniture f, boolean plan) {
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

    static Kind hit(FSHFViewCamera cam, FSHFFurniture f, double sx, double sy, boolean plan) {
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

    void begin(Kind k, FSHFFurniture f, double sx, double sy) {
        kind = k;
        item = f;
        start = f.copy();
        pressX = sx;
        pressY = sy;
        changed = false;
    }

    boolean end() {
        boolean c = changed;
        kind = null;
        item = start = null;
        return c;
    }

    String drag(FSHFDesign design, FSHFViewCamera cam, double sx, double sy, boolean free) {
        FSHFCatalogEntry e = design.catalog().get(item.type);
        FSHFFurniture c = start.copy();
        double a = Math.toRadians(start.rotation), cos = Math.cos(a), sin = Math.sin(a);
        String text;
        boolean keepPosition = true;
        if (kind == Kind.TOP) {
            Point2D top = cam.project(world(start, 0, start.height, 0)), up = cam.project(world(start, 0, start.height + 1, 0));
            if (top == null || up == null) return null;
            double vx = up.getX() - top.getX(), vy = up.getY() - top.getY(), len2 = vx * vx + vy * vy;
            if (len2 < 1) return null;
            double t = ((sx - pressX) * vx + (sy - pressY) * vy) / len2;
            c.height = FSHFUnits.round(clamp(start.height + t, e.heightRange().min(), Math.min(e.heightRange().max(), design.room().height())));
            text = "Alto: " + FSHFUnits.meters(c.height);
        } else {
            Point3D p = cam.floorPoint(sx, sy);
            if (p == null) return null;
            double dx = p.getX() - start.x, dz = p.getZ() - start.z;
            double lx = dx * cos - dz * sin, lz = dx * sin + dz * cos;
            double hw = start.width / 2, hd = start.depth / 2;
            switch (kind) {
                case RIGHT, LEFT -> {
                    double w = kind == Kind.RIGHT ? lx + hw : hw - lx;
                    c.width = FSHFUnits.round(clamp(w, e.widthRange().min(), e.widthRange().max()));
                    double cx = kind == Kind.RIGHT ? -hw + c.width / 2 : hw - c.width / 2;
                    c.x = FSHFUnits.round(start.x + cx * cos);
                    c.z = FSHFUnits.round(start.z - cx * sin);
                    text = "Ancho: " + FSHFUnits.meters(c.width);
                }
                case FRONT, BACK -> {
                    double d = kind == Kind.FRONT ? lz + hd : hd - lz;
                    c.depth = FSHFUnits.round(clamp(d, e.depthRange().min(), e.depthRange().max()));
                    double cz = kind == Kind.FRONT ? -hd + c.depth / 2 : hd - c.depth / 2;
                    c.x = FSHFUnits.round(start.x + cz * sin);
                    c.z = FSHFUnits.round(start.z + cz * cos);
                    text = "Profundidad: " + FSHFUnits.meters(c.depth);
                }
                default -> {
                    double deg = Math.toDegrees(Math.atan2(dx, dz));
                    double snap = free ? 1 : 15;
                    c.rotation = ((Math.round(deg / snap) * snap) % 360 + 360) % 360;
                    keepPosition = false;
                    text = "Giro: " + FSHFUnits.number(c.rotation) + "°" + (free ? "" : "   (Shift: giro libre)");
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
