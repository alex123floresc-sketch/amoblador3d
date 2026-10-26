package pe.edu.unaj.amoblador3d.design;

import java.util.ArrayList;
import java.util.List;
import pe.edu.unaj.amoblador3d.model.Footprint;

/**
 * Imán al arrastrar muebles: si un borde (o el centro) del mueble queda a menos de
 * {@link #THRESHOLD} de una pared o del borde/centro de otro mueble, se pega a él.
 *
 * <p>Se trabaja con la caja alineada a los ejes que envuelve cada mueble (exacta con giros de
 * 0°, 90°, 180° y 270°, los más comunes). Cada eje se resuelve por separado: así un mueble
 * puede quedar pegado a la pared norte y alineado con la cama a la vez.
 */
public final class Snapper {
    /** Distancia a la que actúa el imán (m). */
    public static final double THRESHOLD = 0.08;

    /** Línea guía en el piso, de (x1, z1) a (x2, z2), para mostrar a qué se pegó. */
    public record Guide(double x1, double z1, double x2, double z2) {}

    /**
     * Cuánto mover en cada eje para pegarse y la guía de cada eje (null si en ese eje no se pegó).
     */
    public record Result(double dx, double dz, Guide guideX, Guide guideZ) {
        public List<Guide> guides() {
            List<Guide> list = new ArrayList<>();
            if (guideX != null) list.add(guideX);
            if (guideZ != null) list.add(guideZ);
            return list;
        }
    }

    /** Caja alineada a los ejes: [minX, maxX] x [minZ, maxZ]. */
    record Box(double minX, double maxX, double minZ, double maxZ) {
        static Box of(Footprint f) {
            double hx = f.halfExtentX(), hz = f.halfExtentZ();
            return new Box(f.x() - hx, f.x() + hx, f.z() - hz, f.z() + hz);
        }

        Box union(Box o) {
            return new Box(Math.min(minX, o.minX), Math.max(maxX, o.maxX), Math.min(minZ, o.minZ), Math.max(maxZ, o.maxZ));
        }

        Box shift(double dx, double dz) {
            return new Box(minX + dx, maxX + dx, minZ + dz, maxZ + dz);
        }

        double centerX() {
            return (minX + maxX) / 2;
        }

        double centerZ() {
            return (minZ + maxZ) / 2;
        }
    }

    /** Un posible "pegado" en un eje: cuánto mover y a qué valor queda la guía. */
    private record Candidate(double delta, double at, Box other) {}

    private Snapper() {}

    /**
     * @param moving caja del mueble (o del grupo) ya en la posición que pide el ratón
     * @param width  ancho interior de la habitación
     * @param length largo interior de la habitación
     * @param others cajas de los demás muebles
     * @return cuánto hay que mover la caja (dx, dz) y las guías
     */
    static Result snap(Box moving, double width, double length, List<Box> others) {
        Candidate cx = best(moving.minX, moving.maxX, moving.centerX(), 0, width, others, true);
        Candidate cz = best(moving.minZ, moving.maxZ, moving.centerZ(), 0, length, others, false);
        double dx = cx == null ? 0 : cx.delta, dz = cz == null ? 0 : cz.delta;
        Box placed = moving.shift(dx, dz);
        Guide gx = null, gz = null;
        if (cx != null) {
            // Línea a lo largo de Z que cubre el mueble y aquello a lo que se pegó.
            Box span = cx.other == null ? new Box(cx.at, cx.at, 0, length) : placed.union(cx.other);
            gx = new Guide(cx.at, span.minZ, cx.at, span.maxZ);
        }
        if (cz != null) {
            Box span = cz.other == null ? new Box(0, width, cz.at, cz.at) : placed.union(cz.other);
            gz = new Guide(span.minX, cz.at, span.maxX, cz.at);
        }
        return new Result(dx, dz, gx, gz);
    }

    /** El pegado más cercano en un eje, o null si no hay ninguno dentro del umbral. */
    private static Candidate best(double min, double max, double center, double wallMin, double wallMax,
                                  List<Box> others, boolean alongX) {
        List<Candidate> all = new ArrayList<>();
        all.add(new Candidate(wallMin - min, wallMin, null)); // pegado a la pared norte/oeste
        all.add(new Candidate(wallMax - max, wallMax, null)); // pegado a la pared sur/este
        for (Box o : others) {
            double oMin = alongX ? o.minX : o.minZ, oMax = alongX ? o.maxX : o.maxZ;
            double oCenter = (oMin + oMax) / 2;
            all.add(new Candidate(oMax - min, oMax, o)); // al lado (mi borde izquierdo con su derecho)
            all.add(new Candidate(oMin - max, oMin, o)); // al lado (mi borde derecho con su izquierdo)
            all.add(new Candidate(oMin - min, oMin, o)); // alineados por la izquierda
            all.add(new Candidate(oMax - max, oMax, o)); // alineados por la derecha
            all.add(new Candidate(oCenter - center, oCenter, o)); // centrados
        }
        Candidate best = null;
        for (Candidate c : all) {
            if (Math.abs(c.delta) <= THRESHOLD && (best == null || Math.abs(c.delta) < Math.abs(best.delta) - 1e-9)) best = c;
        }
        return best;
    }
}
