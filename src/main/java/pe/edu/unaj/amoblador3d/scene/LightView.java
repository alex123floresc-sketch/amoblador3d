package pe.edu.unaj.amoblador3d.scene;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.DirectionalLight;
import javafx.scene.Group;
import javafx.scene.PointLight;
import javafx.scene.SpotLight;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import pe.edu.unaj.amoblador3d.design.Design;
import pe.edu.unaj.amoblador3d.model.Light;
import pe.edu.unaj.amoblador3d.model.LightType;
import pe.edu.unaj.amoblador3d.model.Opening;
import pe.edu.unaj.amoblador3d.model.RoomDims;

/**
 * Luces de JavaFX, sus símbolos (esferas que se pueden seleccionar) y la "mancha de sol".
 *
 * <p>Las intensidades del proyecto están en unidades físicas (candelas y lux). JavaFX no usa
 * unidades físicas, así que se convierten con la atenuación de la luz: con más candelas, la
 * luz se debilita menos con la distancia. JavaFX tampoco dibuja sombras; para que el sol
 * "entre por la ventana" se proyecta la ventana sobre el piso y se dibuja esa zona iluminada.
 */
final class LightView {
    /** Brillo de la luz ambiental por cada unidad del control deslizante (0..2). */
    private static final double AMBIENT_SCALE = 0.45;
    private static final double POINT_SCALE = 0.35;
    private static final double SPOT_SCALE = 0.15;
    private static final double SUN_LUX_FOR_FULL = 5;

    final Group group = new Group();
    private final AmbientLight ambient = new AmbientLight();
    private final List<SunInfo> suns = new ArrayList<>();

    /** Datos del sol para dibujar su flecha en la capa 2D. */
    record SunInfo(Light light, Point3D helper, Point3D windowCenter) {}

    LightView() {
        group.getChildren().add(ambient);
    }

    List<SunInfo> suns() {
        return suns;
    }

    void rebuild(Design design) {
        group.getChildren().setAll(ambient);
        suns.clear();
        ambient.setColor(Color.gray(Math.min(1, design.ambient() * AMBIENT_SCALE)));
        for (Light l : design.lights()) {
            switch (l.type) {
                case POINT -> addPoint(l);
                case SPOT -> addSpot(l);
                case SUN -> addSun(design, l);
            }
        }
    }

    /** Posición del símbolo de una luz (para el contorno de selección). */
    Point3D helperPosition(Light l) {
        if (l.type != LightType.SUN) return new Point3D(l.x, l.y, l.z);
        for (SunInfo s : suns) if (s.light == l) return s.helper;
        return null;
    }

    private void addPoint(Light l) {
        PointLight light = new PointLight(Color.web(l.color));
        attenuate(light, l.intensity * POINT_SCALE);
        place(light, l.x, l.y, l.z);
        group.getChildren().addAll(light, helper(l, new Point3D(l.x, l.y, l.z)));
    }

    private void addSpot(Light l) {
        SpotLight light = new SpotLight(Color.web(l.color));
        attenuate(light, l.intensity * SPOT_SCALE);
        place(light, l.x, l.y, l.z);
        Point3D dir = new Point3D(l.targetX - l.x, -l.y, l.targetZ - l.z);
        if (dir.magnitude() > 1e-6) light.setDirection(dir.normalize());
        light.setOuterAngle(l.angle);
        light.setInnerAngle(l.angle * (1 - l.penumbra));
        light.setFalloff(1);
        group.getChildren().addAll(light, helper(l, new Point3D(l.x, l.y, l.z)));
    }

    /** factor = intensidad a 1 m; la luz cae con el cuadrado de la distancia. */
    private static void attenuate(PointLight light, double factor) {
        if (factor <= 0) {
            light.setLightOn(false);
            return;
        }
        light.setConstantAttenuation(1 / factor);
        light.setLinearAttenuation(0);
        light.setQuadraticAttenuation(1 / factor);
    }

    private void addSun(Design design, Light l) {
        Opening window = design.findOpening(l.windowId == null ? "" : l.windowId);
        if (window == null) return;
        RoomDims r = design.room();
        WallFrame wall = WallFrame.of(window.wall, r);
        Point3D dir = sunDirection(wall, l);

        double strength = Math.min(1, l.intensity / SUN_LUX_FOR_FULL);
        Color c = Color.web(l.color);
        DirectionalLight light = new DirectionalLight(Color.color(c.getRed() * strength, c.getGreen() * strength, c.getBlue() * strength));
        light.setDirection(dir);
        light.setLightOn(strength > 0);
        group.getChildren().add(light);

        // Símbolo fuera de la ventana, en la dirección de donde vienen los rayos.
        Point3D windowCenter = wall.point(window.offset + window.width / 2, window.sill + window.height / 2, r.wallThickness() / 2);
        Point3D helperPos = windowCenter.subtract(dir.multiply(1.1));
        group.getChildren().add(helper(l, helperPos));
        suns.add(new SunInfo(l, helperPos, windowCenter));

        MeshView patch = sunPatch(r, wall, window, dir, c, strength);
        if (patch != null) group.getChildren().add(patch);
    }

    /**
     * Dirección de los rayos: hacia el interior (normal de la pared), girada "deviation"
     * grados a un lado y bajando según la altura del sol.
     */
    static Point3D sunDirection(WallFrame wall, Light l) {
        Point3D n = wall.interiorNormal();
        double a = Math.toRadians(l.deviation), e = Math.toRadians(l.elevation);
        double hx = n.getX() * Math.cos(a) + n.getZ() * Math.sin(a);
        double hz = -n.getX() * Math.sin(a) + n.getZ() * Math.cos(a);
        return new Point3D(hx * Math.cos(e), -Math.sin(e), hz * Math.cos(e)).normalize();
    }

    /** Zona del piso iluminada por el sol: la ventana proyectada sobre el piso (recortada a la habitación). */
    private static MeshView sunPatch(RoomDims r, WallFrame wall, Opening w, Point3D dir, Color color, double strength) {
        if (strength <= 0) return null;
        double[][] corners = {
            {w.offset, w.sill}, {w.offset + w.width, w.sill}, {w.offset + w.width, w.sill + w.height}, {w.offset, w.sill + w.height},
        };
        List<double[]> poly = new ArrayList<>();
        for (double[] c : corners) {
            Point3D p = wall.point(c[0], c[1], 0);
            double t = p.getY() / -dir.getY();
            poly.add(new double[] {p.getX() + dir.getX() * t, p.getZ() + dir.getZ() * t});
        }
        poly = clip(poly, 0, 0, r.width(), r.length());
        if (poly.size() < 3) return null;

        TriangleMesh mesh = new TriangleMesh();
        for (double[] p : poly) mesh.getPoints().addAll((float) p[0], 0.004f, (float) p[1]);
        mesh.getTexCoords().addAll(0, 0);
        for (int i = 1; i + 1 < poly.size(); i++) mesh.getFaces().addAll(0, 0, i, 0, i + 1, 0);
        MeshView view = new MeshView(mesh);
        view.setCullFace(CullFace.NONE);
        view.setMouseTransparent(true);
        WritableImage glow = new WritableImage(1, 1);
        glow.getPixelWriter().setColor(0, 0, Color.color(color.getRed() * 0.55 * strength, color.getGreen() * 0.5 * strength, color.getBlue() * 0.35 * strength));
        PhongMaterial m = new PhongMaterial(Color.rgb(255, 240, 200, 0.35 * strength));
        m.setSelfIlluminationMap(glow);
        view.setMaterial(m);
        return view;
    }

    /** Recorta un polígono convexo al rectángulo [x0, x1] x [z0, z1] (algoritmo de Sutherland-Hodgman). */
    static List<double[]> clip(List<double[]> poly, double x0, double z0, double x1, double z1) {
        double[][] edges = {{0, x0, 1}, {0, x1, -1}, {1, z0, 1}, {1, z1, -1}}; // eje, límite, lado que se conserva
        for (double[] e : edges) {
            if (poly.isEmpty()) break;
            int axis = (int) e[0];
            List<double[]> out = new ArrayList<>();
            for (int i = 0; i < poly.size(); i++) {
                double[] a = poly.get(i), b = poly.get((i + 1) % poly.size());
                double da = (a[axis] - e[1]) * e[2], db = (b[axis] - e[1]) * e[2];
                if (da >= 0) out.add(a);
                if ((da >= 0) != (db >= 0)) {
                    double t = da / (da - db);
                    out.add(new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t});
                }
            }
            poly = out;
        }
        return poly;
    }

    private static Sphere helper(Light l, Point3D p) {
        Sphere s = new Sphere(l.type == LightType.SUN ? 0.09 : 0.06, 16);
        s.setMaterial(Materials.glow(l.color));
        place(s, p.getX(), p.getY(), p.getZ());
        SceneView.tag(s, l);
        return s;
    }

    private static void place(javafx.scene.Node n, double x, double y, double z) {
        n.setTranslateX(x);
        n.setTranslateY(y);
        n.setTranslateZ(z);
    }
}
