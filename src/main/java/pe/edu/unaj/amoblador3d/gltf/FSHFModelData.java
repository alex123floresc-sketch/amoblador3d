package pe.edu.unaj.amoblador3d.gltf;

import java.util.List;

public record FSHFModelData(List<Primitive> primitives) {
    public record Primitive(float[] positions, float[] normals, float[] uvs, int[] indices, Material material) {}

    public record Material(double[] color, byte[] image, boolean doubleSided) {
        public static final Material DEFAULT = new Material(new double[] {0.8, 0.8, 0.8, 1}, null, false);
    }

    public Box bounds(double rotationDeg) {
        double a = Math.toRadians(rotationDeg), c = Math.cos(a), s = Math.sin(a);
        double minX = Double.POSITIVE_INFINITY, minY = minX, minZ = minX;
        double maxX = Double.NEGATIVE_INFINITY, maxY = maxX, maxZ = maxX;
        for (Primitive p : primitives) {
            float[] v = p.positions();
            for (int i = 0; i < v.length; i += 3) {
                double x = v[i] * c + v[i + 2] * s;
                double z = -v[i] * s + v[i + 2] * c;
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minY = Math.min(minY, v[i + 1]);
                maxY = Math.max(maxY, v[i + 1]);
                minZ = Math.min(minZ, z);
                maxZ = Math.max(maxZ, z);
            }
        }
        if (minX > maxX) return new Box(0, 0, 0, 0, 0, 0);
        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        public double sizeX() {
            return maxX - minX;
        }

        public double sizeY() {
            return maxY - minY;
        }

        public double sizeZ() {
            return maxZ - minZ;
        }
    }
}
