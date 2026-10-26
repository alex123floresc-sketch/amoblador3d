package pe.edu.unaj.amoblador3d.scene;

import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.PerspectiveCamera;
import javafx.scene.transform.Affine;

/**
 * Cámara de la vista 3D (órbita alrededor de un punto) y de la planta 2D (desde arriba).
 *
 * <p>Toda la matemática se hace en coordenadas del MUNDO (metros, Y hacia arriba, igual que
 * el proyecto). JavaFX usa Y hacia abajo; {@link SceneView} gira el mundo 180° en X, así que
 * un punto del mundo (x, y, z) está en (x, -y, -z) de la escena de JavaFX.
 *
 * <p>Hacemos nosotros la proyección (punto -> píxel) y el rayo (píxel -> recta) para dibujar
 * las cotas encima y para saber en qué punto del piso está el ratón al arrastrar un mueble.
 */
final class ViewCamera {
    enum Mode { THREE_D, PLAN }

    /**
     * La planta usa una cámara en perspectiva MUY lejana y con un ángulo pequeñísimo: se ve
     * prácticamente sin perspectiva (como una cámara ortográfica) y funciona igual con los clics.
     */
    private static final double PLAN_DISTANCE = 1000;
    private static final double FOV_3D = 50;
    private static final Point3D WORLD_UP = new Point3D(0, 1, 0);
    private static final Point3D NORTH = new Point3D(0, 0, -1);

    final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final Affine transform = new Affine();

    Mode mode = Mode.THREE_D;
    private double width = 1, height = 1;

    // Órbita 3D
    private Point3D target = new Point3D(2, 1, 1.75);
    private double yaw = Math.atan2(0.6, 1);
    private double pitch = Math.asin(0.75 / Math.sqrt(0.36 + 0.5625 + 1));
    private double distance = 10;

    // Planta
    private Point3D planTarget = new Point3D(2, 0, 1.75);
    private double planViewSize = 10; // metros visibles en vertical

    // Calculados en update()
    private Point3D eye = Point3D.ZERO, right = Point3D.ZERO, up = Point3D.ZERO, forward = Point3D.ZERO;
    private double tanHalf = 1;

    ViewCamera() {
        camera.getTransforms().add(transform);
        update();
    }

    void setViewport(double w, double h) {
        width = Math.max(w, 1);
        height = Math.max(h, 1);
        update();
    }

    Point3D eye() {
        return eye;
    }

    /** Todo lo necesario para volver a dejar la cámara como estaba. */
    record State(Mode mode, Point3D target, double yaw, double pitch, double distance,
                 Point3D planTarget, double planViewSize) {}

    State state() {
        return new State(mode, target, yaw, pitch, distance, planTarget, planViewSize);
    }

    void restore(State s) {
        mode = s.mode();
        target = s.target();
        yaw = s.yaw();
        pitch = s.pitch();
        distance = s.distance();
        planTarget = s.planTarget();
        planViewSize = s.planViewSize();
        update();
    }

    /** Metros por píxel en el punto que mira la cámara (para que mover/arrastrar siga al ratón). */
    private double metersPerPixel() {
        double d = mode == Mode.PLAN ? PLAN_DISTANCE : distance;
        return 2 * d * tanHalf / height;
    }

    void rotate(double dxPixels, double dyPixels) {
        if (mode == Mode.PLAN) return;
        yaw -= dxPixels * 0.008;
        pitch = clamp(pitch + dyPixels * 0.008, 0.03, Math.PI / 2 - 0.02); // no bajar del piso
        update();
    }

    void pan(double dxPixels, double dyPixels) {
        double k = metersPerPixel();
        Point3D move = right.multiply(-dxPixels * k).add(up.multiply(dyPixels * k));
        if (mode == Mode.PLAN) planTarget = planTarget.add(move.getX(), 0, move.getZ());
        else target = target.add(move);
        update();
    }

    /** factor < 1 acerca, > 1 aleja. */
    void zoom(double factor) {
        if (mode == Mode.PLAN) planViewSize = clamp(planViewSize * factor, 0.5, 60);
        else distance = clamp(distance * factor, 0.5, 100);
        update();
    }

    /** Encuadra ambas cámaras para que se vea completa la caja indicada (mundo). */
    void frame(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        Point3D center = new Point3D((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
        double sx = maxX - minX, sy = maxY - minY, sz = maxZ - minZ;
        double radius = Math.sqrt(sx * sx + sy * sy + sz * sz) / 2;
        // Vista 3D: en diagonal desde el sureste y arriba; 25 % extra para las cotas.
        target = center;
        distance = radius / Math.sin(Math.toRadians(FOV_3D) / 2) * 1.25;
        yaw = Math.atan2(0.6, 1);
        pitch = Math.asin(0.75 / Math.sqrt(0.36 + 0.5625 + 1));
        // Planta: margen de 1.6 m para que entren las cotas.
        double margin = 1.6, aspect = width / height;
        planTarget = new Point3D(center.getX(), 0, center.getZ());
        planViewSize = Math.max(sz + margin * 2, (sx + margin * 2) / aspect);
        update();
    }

    /** Recalcula la posición y orientación de la cámara de JavaFX. */
    void update() {
        Point3D upHint;
        double fov;
        if (mode == Mode.PLAN) {
            eye = planTarget.add(0, PLAN_DISTANCE, 0);
            forward = new Point3D(0, -1, 0);
            upHint = NORTH; // el norte queda arriba en la pantalla
            tanHalf = planViewSize / 2 / PLAN_DISTANCE;
            fov = Math.toDegrees(2 * Math.atan(tanHalf));
            camera.setNearClip(PLAN_DISTANCE - 20);
            camera.setFarClip(PLAN_DISTANCE + 5);
        } else {
            Point3D offset = new Point3D(Math.cos(pitch) * Math.sin(yaw), Math.sin(pitch), Math.cos(pitch) * Math.cos(yaw));
            eye = target.add(offset.multiply(distance));
            forward = target.subtract(eye).normalize();
            upHint = WORLD_UP;
            fov = FOV_3D;
            tanHalf = Math.tan(Math.toRadians(fov) / 2);
            camera.setNearClip(0.05);
            camera.setFarClip(500);
        }
        right = forward.crossProduct(upHint).normalize();
        up = right.crossProduct(forward).normalize();
        camera.setFieldOfView(fov);

        // Ejes de la cámara de JavaFX (x = derecha, y = ABAJO, z = adelante) en coordenadas de la escena.
        Point3D x = toScene(right), y = toScene(up.multiply(-1)), z = toScene(forward), t = toScene(eye);
        transform.setToTransform(
                x.getX(), y.getX(), z.getX(), t.getX(),
                x.getY(), y.getY(), z.getY(), t.getY(),
                x.getZ(), y.getZ(), z.getZ(), t.getZ());
    }

    /** Mundo (Y arriba) -> escena de JavaFX (Y abajo). */
    private static Point3D toScene(Point3D p) {
        return new Point3D(p.getX(), -p.getY(), -p.getZ());
    }

    /** Píxel de la pantalla donde se ve un punto del mundo, o null si está detrás de la cámara. */
    Point2D project(Point3D p) {
        Point3D v = p.subtract(eye);
        double zc = v.dotProduct(forward);
        if (zc < 0.06) return null;
        double ndcX = v.dotProduct(right) / (zc * tanHalf * width / height);
        double ndcY = v.dotProduct(up) / (zc * tanHalf);
        return new Point2D((ndcX + 1) / 2 * width, (1 - ndcY) / 2 * height);
    }

    /** Punto del piso (y = 0) bajo el píxel indicado, o null si el rayo no lo toca. */
    Point3D floorPoint(double sx, double sy) {
        double ndcX = sx / width * 2 - 1, ndcY = 1 - sy / height * 2;
        Point3D dir = forward.add(right.multiply(ndcX * tanHalf * width / height)).add(up.multiply(ndcY * tanHalf));
        if (dir.getY() > -1e-6) return null; // mira hacia arriba u horizontal
        double t = -eye.getY() / dir.getY();
        return eye.add(dir.multiply(t));
    }

    private static double clamp(double v, double min, double max) {
        return Math.min(Math.max(v, min), max);
    }
}
