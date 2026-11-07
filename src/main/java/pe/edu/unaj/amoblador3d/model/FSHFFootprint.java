package pe.edu.unaj.amoblador3d.model;

public record FSHFFootprint(double x, double z, double hw, double hd, double angle) {
    private static final double EPS = 1e-6;

    public static FSHFFootprint of(FSHFFurniture f) {
        return new FSHFFootprint(f.x, f.z, f.width / 2, f.depth / 2, Math.toRadians(f.rotation));
    }

    public FSHFFootprint withCenter(double x, double z) {
        return new FSHFFootprint(x, z, hw, hd, angle);
    }

    private double[][] axes() {
        double c = Math.cos(angle), s = Math.sin(angle);
        return new double[][] {{c, -s}, {s, c}};
    }

    private double projectedRadius(double nx, double nz) {
        double[][] a = axes();
        return hw * Math.abs(a[0][0] * nx + a[0][1] * nz) + hd * Math.abs(a[1][0] * nx + a[1][1] * nz);
    }

    public boolean overlaps(FSHFFootprint other) {
        double dx = other.x - x, dz = other.z - z;
        double[][] all = {axes()[0], axes()[1], other.axes()[0], other.axes()[1]};
        for (double[] n : all) {
            double distance = Math.abs(dx * n[0] + dz * n[1]);
            if (distance >= projectedRadius(n[0], n[1]) + other.projectedRadius(n[0], n[1]) - EPS) return false;
        }
        return true;
    }

    public double[][] corners() {
        double[][] a = axes();
        double[][] result = new double[4][];
        int i = 0;
        for (double[] s : new double[][] {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}}) {
            result[i++] = new double[] {x + s[0] * hw * a[0][0] + s[1] * hd * a[1][0], z + s[0] * hw * a[0][1] + s[1] * hd * a[1][1]};
        }
        return result;
    }

    public double halfExtentX() {
        return hw * Math.abs(Math.cos(angle)) + hd * Math.abs(Math.sin(angle));
    }

    public double halfExtentZ() {
        return hw * Math.abs(Math.sin(angle)) + hd * Math.abs(Math.cos(angle));
    }

    public boolean insideRoom(double width, double length) {
        double hx = halfExtentX(), hz = halfExtentZ();
        return x - hx >= -EPS && x + hx <= width + EPS && z - hz >= -EPS && z + hz <= length + EPS;
    }

    public FSHFFootprint clampToRoom(double width, double length) {
        return withCenter(clamp(x, halfExtentX(), width - halfExtentX()), clamp(z, halfExtentZ(), length - halfExtentZ()));
    }

    private static double clamp(double v, double min, double max) {
        return min > max ? (min + max) / 2 : Math.min(Math.max(v, min), max);
    }
}
