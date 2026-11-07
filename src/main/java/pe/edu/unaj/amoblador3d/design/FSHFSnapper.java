package pe.edu.unaj.amoblador3d.design;

import java.util.ArrayList;
import java.util.List;
import pe.edu.unaj.amoblador3d.model.FSHFFootprint;

public final class FSHFSnapper {
    public static final double THRESHOLD = 0.08;

    public record Guide(double x1, double z1, double x2, double z2) {}

    public record Result(double dx, double dz, Guide guideX, Guide guideZ) {
        public List<Guide> guides() {
            List<Guide> list = new ArrayList<>();
            if (guideX != null) list.add(guideX);
            if (guideZ != null) list.add(guideZ);
            return list;
        }
    }

    record Box(double minX, double maxX, double minZ, double maxZ) {
        static Box of(FSHFFootprint f) {
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

    private record Candidate(double delta, double at, Box other) {}

    private FSHFSnapper() {}

    static Result snap(Box moving, double width, double length, List<Box> others) {
        Candidate cx = best(moving.minX, moving.maxX, moving.centerX(), 0, width, others, true);
        Candidate cz = best(moving.minZ, moving.maxZ, moving.centerZ(), 0, length, others, false);
        double dx = cx == null ? 0 : cx.delta, dz = cz == null ? 0 : cz.delta;
        Box placed = moving.shift(dx, dz);
        Guide gx = null, gz = null;
        if (cx != null) {
            Box span = cx.other == null ? new Box(cx.at, cx.at, 0, length) : placed.union(cx.other);
            gx = new Guide(cx.at, span.minZ, cx.at, span.maxZ);
        }
        if (cz != null) {
            Box span = cz.other == null ? new Box(0, width, cz.at, cz.at) : placed.union(cz.other);
            gz = new Guide(span.minX, cz.at, span.maxX, cz.at);
        }
        return new Result(dx, dz, gx, gz);
    }

    private static Candidate best(double min, double max, double center, double wallMin, double wallMax,
                                  List<Box> others, boolean alongX) {
        List<Candidate> all = new ArrayList<>();
        all.add(new Candidate(wallMin - min, wallMin, null));
        all.add(new Candidate(wallMax - max, wallMax, null));
        for (Box o : others) {
            double oMin = alongX ? o.minX : o.minZ, oMax = alongX ? o.maxX : o.maxZ;
            double oCenter = (oMin + oMax) / 2;
            all.add(new Candidate(oMax - min, oMax, o));
            all.add(new Candidate(oMin - max, oMin, o));
            all.add(new Candidate(oMin - min, oMin, o));
            all.add(new Candidate(oMax - max, oMax, o));
            all.add(new Candidate(oCenter - center, oCenter, o));
        }
        Candidate best = null;
        for (Candidate c : all) {
            if (Math.abs(c.delta) <= THRESHOLD && (best == null || Math.abs(c.delta) < Math.abs(best.delta) - 1e-9)) best = c;
        }
        return best;
    }
}
