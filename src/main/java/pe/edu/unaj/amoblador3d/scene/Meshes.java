package pe.edu.unaj.amoblador3d.scene;

import javafx.scene.paint.Material;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;

/**
 * Cajas con la textura a escala real. Un {@code Box} de JavaFX estira la imagen sobre cada
 * cara; aquí las coordenadas de textura salen de la posición en el mundo dividida entre
 * {@code tile} (los metros que cubre la imagen), y JavaFX la repite. Como dependen del mundo
 * y no de la caja, los tramos de una pared cortada por una ventana siguen el mismo patrón.
 */
final class Meshes {
    private Meshes() {}

    /** Caja que ocupa [x0, x1] x [y0, y1] x [z0, z1] (coordenadas del proyecto, Y hacia arriba). */
    static MeshView box(double x0, double x1, double y0, double y1, double z0, double z1, double tile, Material material) {
        TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
        // Caras perpendiculares a Z: u = x; a X: u = z; a Y: u = x, v = z. En las verticales v = -y (la imagen va hacia abajo).
        face(mesh, tile, new double[][] {{x0, y0, z0}, {x1, y0, z0}, {x1, y1, z0}, {x0, y1, z0}}, 0, 0, -1, 0, 1);
        face(mesh, tile, new double[][] {{x1, y0, z1}, {x0, y0, z1}, {x0, y1, z1}, {x1, y1, z1}}, 0, 0, 1, 0, 1);
        face(mesh, tile, new double[][] {{x0, y0, z1}, {x0, y0, z0}, {x0, y1, z0}, {x0, y1, z1}}, -1, 0, 0, 2, 1);
        face(mesh, tile, new double[][] {{x1, y0, z0}, {x1, y0, z1}, {x1, y1, z1}, {x1, y1, z0}}, 1, 0, 0, 2, 1);
        face(mesh, tile, new double[][] {{x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}}, 0, 1, 0, 0, 2);
        face(mesh, tile, new double[][] {{x0, y0, z1}, {x1, y0, z1}, {x1, y0, z0}, {x0, y0, z0}}, 0, -1, 0, 0, 2);
        MeshView view = new MeshView(mesh);
        // Con normales propias, NONE dibuja bien sin importar el orden de los vértices (igual que los modelos glTF).
        view.setCullFace(CullFace.NONE);
        view.setMaterial(material);
        return view;
    }

    /**
     * Agrega un rectángulo (4 esquinas en orden) con su normal.
     *
     * @param uAxis eje del mundo que da la coordenada u (0 = X, 2 = Z)
     * @param vAxis eje que da v (1 = Y, invertido; 2 = Z)
     */
    private static void face(TriangleMesh mesh, double tile, double[][] corners, double nx, double ny, double nz, int uAxis, int vAxis) {
        int p = mesh.getPoints().size() / 3, t = mesh.getTexCoords().size() / 2, n = mesh.getNormals().size() / 3;
        mesh.getNormals().addAll((float) nx, (float) ny, (float) nz);
        for (double[] c : corners) {
            mesh.getPoints().addAll((float) c[0], (float) c[1], (float) c[2]);
            double u = c[uAxis] / tile, v = (vAxis == 1 ? -c[1] : c[vAxis]) / tile;
            mesh.getTexCoords().addAll((float) u, (float) v);
        }
        mesh.getFaces().addAll(p, n, t, p + 1, n, t + 1, p + 2, n, t + 2);
        mesh.getFaces().addAll(p, n, t, p + 2, n, t + 2, p + 3, n, t + 3);
    }
}
