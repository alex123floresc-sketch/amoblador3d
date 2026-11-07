package pe.edu.unaj.amoblador3d.scene;

import javafx.scene.paint.Material;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;

final class FSHFMeshes {
    private FSHFMeshes() {}

    static MeshView box(double x0, double x1, double y0, double y1, double z0, double z1, double tile, Material material) {
        TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);

        face(mesh, tile, new double[][] {{x0, y0, z0}, {x1, y0, z0}, {x1, y1, z0}, {x0, y1, z0}}, 0, 0, -1, 0, 1);
        face(mesh, tile, new double[][] {{x1, y0, z1}, {x0, y0, z1}, {x0, y1, z1}, {x1, y1, z1}}, 0, 0, 1, 0, 1);
        face(mesh, tile, new double[][] {{x0, y0, z1}, {x0, y0, z0}, {x0, y1, z0}, {x0, y1, z1}}, -1, 0, 0, 2, 1);
        face(mesh, tile, new double[][] {{x1, y0, z0}, {x1, y0, z1}, {x1, y1, z1}, {x1, y1, z0}}, 1, 0, 0, 2, 1);
        face(mesh, tile, new double[][] {{x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}}, 0, 1, 0, 0, 2);
        face(mesh, tile, new double[][] {{x0, y0, z1}, {x1, y0, z1}, {x1, y0, z0}, {x0, y0, z0}}, 0, -1, 0, 0, 2);
        MeshView view = new MeshView(mesh);

        view.setCullFace(CullFace.NONE);
        view.setMaterial(material);
        return view;
    }

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
