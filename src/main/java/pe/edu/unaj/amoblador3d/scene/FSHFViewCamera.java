package pe.edu.unaj.amoblador3d.scene;

import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.PerspectiveCamera;
import javafx.scene.transform.Affine;

final class FSHFViewCamera {
    enum Mode { THREE_D, PLAN, WALK }

    private static final double PLAN_DISTANCE = 1000;
    private static final double FOV_3D = 50, FOV_WALK = 70;
    private static final Point3D WORLD_UP = new Point3D(0, 1, 0);
    private static final Point3D NORTH = new Point3D(0, 0, -1);

    final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final Affine transform = new Affine();

    Mode mode = Mode.THREE_D;
    private double width = 1, height = 1;

    private Point3D target = new Point3D(2, 1, 1.75);
    private double yaw = Math.atan2(0.6, 1);
    private double pitch = Math.asin(0.75 / Math.sqrt(0.36 + 0.5625 + 1));
    private double distance = 10;

    private Point3D planTarget = new Point3D(2, 0, 1.75);
    private double planViewSize = 10;

    private Point3D walkEye = new Point3D(2, 1.6, 3);
    private double walkYaw, walkPitch;

    private Point3D eye = Point3D.ZERO, right = Point3D.ZERO, up = Point3D.ZERO, forward = Point3D.ZERO;
    private double tanHalf = 1;

    FSHFViewCamera() {
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

    record State(Mode mode, Point3D target, double yaw, double pitch, double distance,
                 Point3D planTarget, double planViewSize, Point3D walkEye, double walkYaw, double walkPitch) {}

    State state() {
        return new State(mode, target, yaw, pitch, distance, planTarget, planViewSize, walkEye, walkYaw, walkPitch);
    }

    void restore(State s) {
        mode = s.mode();
        target = s.target();
        yaw = s.yaw();
        pitch = s.pitch();
        distance = s.distance();
        planTarget = s.planTarget();
        planViewSize = s.planViewSize();
        walkEye = s.walkEye();
        walkYaw = s.walkYaw();
        walkPitch = s.walkPitch();
        update();
    }

    double[] values() {
        return switch (mode) {
            case THREE_D -> new double[] {target.getX(), target.getY(), target.getZ(), yaw, pitch, distance};
            case PLAN -> new double[] {planTarget.getX(), 0, planTarget.getZ(), 0, 0, planViewSize};
            case WALK -> new double[] {walkEye.getX(), walkEye.getY(), walkEye.getZ(), walkYaw, walkPitch, 0};
        };
    }

    void apply(Mode m, double x, double y, double z, double yawValue, double pitchValue, double dist) {
        mode = m;
        switch (m) {
            case THREE_D -> {
                target = new Point3D(x, y, z);
                yaw = yawValue;
                pitch = clamp(pitchValue, 0.03, Math.PI / 2 - 0.02);
                distance = clamp(dist, 0.5, 100);
            }
            case PLAN -> {
                planTarget = new Point3D(x, 0, z);
                planViewSize = clamp(dist, 0.5, 60);
            }
            case WALK -> {
                walkEye = new Point3D(x, y, z);
                walkYaw = yawValue;
                walkPitch = clamp(pitchValue, -1.2, 1.2);
            }
        }
        update();
    }

    Point3D walkEye() {
        return walkEye;
    }

    void walkTo(double x, double eyeHeight, double z, double yawRadians) {
        walkEye = new Point3D(x, eyeHeight, z);
        walkYaw = yawRadians;
        walkPitch = 0;
        update();
    }

    void setWalkEye(Point3D eye) {
        walkEye = eye;
        update();
    }

    Point3D walkForward() {
        return new Point3D(-Math.sin(walkYaw), 0, -Math.cos(walkYaw));
    }

    Point3D walkRight() {
        return new Point3D(Math.cos(walkYaw), 0, -Math.sin(walkYaw));
    }

    void turn(double radians) {
        walkYaw += radians;
        update();
    }

    private double metersPerPixel() {
        double d = mode == Mode.PLAN ? PLAN_DISTANCE : distance;
        return 2 * d * tanHalf / height;
    }

    void rotate(double dxPixels, double dyPixels) {
        if (mode == Mode.PLAN) return;
        if (mode == Mode.WALK) {
            walkYaw -= dxPixels * 0.005;
            walkPitch = clamp(walkPitch - dyPixels * 0.005, -1.2, 1.2);
            update();
            return;
        }
        yaw -= dxPixels * 0.008;
        pitch = clamp(pitch + dyPixels * 0.008, 0.03, Math.PI / 2 - 0.02);
        update();
    }

    void pan(double dxPixels, double dyPixels) {
        if (mode == Mode.WALK) {
            rotate(dxPixels, dyPixels);
            return;
        }
        double k = metersPerPixel();
        Point3D move = right.multiply(-dxPixels * k).add(up.multiply(dyPixels * k));
        if (mode == Mode.PLAN) planTarget = planTarget.add(move.getX(), 0, move.getZ());
        else target = target.add(move);
        update();
    }

    void zoom(double factor) {
        if (mode == Mode.WALK) return;
        if (mode == Mode.PLAN) planViewSize = clamp(planViewSize * factor, 0.5, 60);
        else distance = clamp(distance * factor, 0.5, 100);
        update();
    }

    void frame(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        Point3D center = new Point3D((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
        double sx = maxX - minX, sy = maxY - minY, sz = maxZ - minZ;
        double radius = Math.sqrt(sx * sx + sy * sy + sz * sz) / 2;

        target = center;
        distance = radius / Math.sin(Math.toRadians(FOV_3D) / 2) * 1.25;
        yaw = Math.atan2(0.6, 1);
        pitch = Math.asin(0.75 / Math.sqrt(0.36 + 0.5625 + 1));

        double margin = 1.6, aspect = width / height;
        planTarget = new Point3D(center.getX(), 0, center.getZ());
        planViewSize = Math.max(sz + margin * 2, (sx + margin * 2) / aspect);
        update();
    }

    void update() {
        Point3D upHint;
        double fov;
        if (mode == Mode.PLAN) {
            eye = planTarget.add(0, PLAN_DISTANCE, 0);
            forward = new Point3D(0, -1, 0);
            upHint = NORTH;
            tanHalf = planViewSize / 2 / PLAN_DISTANCE;
            fov = Math.toDegrees(2 * Math.atan(tanHalf));
            camera.setNearClip(PLAN_DISTANCE - 20);
            camera.setFarClip(PLAN_DISTANCE + 5);
        } else if (mode == Mode.WALK) {
            eye = walkEye;
            double c = Math.cos(walkPitch);
            forward = new Point3D(-Math.sin(walkYaw) * c, Math.sin(walkPitch), -Math.cos(walkYaw) * c);
            upHint = WORLD_UP;
            fov = FOV_WALK;
            tanHalf = Math.tan(Math.toRadians(fov) / 2);
            camera.setNearClip(0.03);
            camera.setFarClip(200);
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

        Point3D x = toScene(right), y = toScene(up.multiply(-1)), z = toScene(forward), t = toScene(eye);
        transform.setToTransform(
                x.getX(), y.getX(), z.getX(), t.getX(),
                x.getY(), y.getY(), z.getY(), t.getY(),
                x.getZ(), y.getZ(), z.getZ(), t.getZ());
    }

    private static Point3D toScene(Point3D p) {
        return new Point3D(p.getX(), -p.getY(), -p.getZ());
    }

    Point2D project(Point3D p) {
        Point3D v = p.subtract(eye);
        double zc = v.dotProduct(forward);
        if (zc < 0.06) return null;
        double ndcX = v.dotProduct(right) / (zc * tanHalf * width / height);
        double ndcY = v.dotProduct(up) / (zc * tanHalf);
        return new Point2D((ndcX + 1) / 2 * width, (1 - ndcY) / 2 * height);
    }

    Point3D floorPoint(double sx, double sy) {
        return planePoint(sx, sy, 0);
    }

    Point3D planePoint(double sx, double sy, double y) {
        double ndcX = sx / width * 2 - 1, ndcY = 1 - sy / height * 2;
        Point3D dir = forward.add(right.multiply(ndcX * tanHalf * width / height)).add(up.multiply(ndcY * tanHalf));
        if (dir.getY() > -1e-6) return null;
        double t = (y - eye.getY()) / dir.getY();
        return t < 0 ? null : eye.add(dir.multiply(t));
    }

    private static double clamp(double v, double min, double max) {
        return Math.min(Math.max(v, min), max);
    }
}
