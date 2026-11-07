package pe.edu.unaj.amoblador3d.scene;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.LightBase;
import javafx.scene.Node;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SnapshotParameters;
import javafx.scene.SubScene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Box;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalog;
import pe.edu.unaj.amoblador3d.design.FSHFDesign;
import pe.edu.unaj.amoblador3d.model.FSHFHome;
import pe.edu.unaj.amoblador3d.model.FSHFProject;
import pe.edu.unaj.amoblador3d.model.FSHFRoomDims;
import pe.edu.unaj.amoblador3d.util.FSHFUnits;

public final class FSHFHomeView {
    private static final String SPACE = "amoblador.space";
    private static final double SNAP = 0.35;
    private static final double CLICK_TOLERANCE = 5;
    private static final LinearGradient BACKGROUND = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#46505c")), new Stop(0.55, Color.web("#2b3036")), new Stop(1, Color.web("#1d2125")));
    private static final Color SELECT = Color.web("#4f9cf9"), GUIDE = Color.web("#ff4fd8");
    private static final Font NAME_FONT = Font.font("System", FontWeight.BOLD, 13), INFO_FONT = Font.font("System", 11);

    private final FSHFCatalog catalog;
    private final FSHFModelLibrary models;
    private final StackPane root = new StackPane();
    private final SubScene subScene;
    private final FSHFViewCamera cam = new FSHFViewCamera();
    private final Group spacesGroup = new Group();
    private final Box ground = new Box(1, 0.06, 1);
    private final Canvas canvas = new Canvas();

    private FSHFHome home = new FSHFHome();
    private final List<SpaceNode> nodes = new ArrayList<>();
    private FSHFHome.Space selected;
    private int level;

    private Consumer<FSHFHome.Space> onSelect = s -> {};
    private Consumer<FSHFHome.Space> onOpen = s -> {};
    private Runnable onMoved = () -> {};
    private Consumer<String> onStatus = s -> {};

    private double pressX, pressY, lastX, lastY;
    private FSHFHome.Space dragging;
    private double dragDx, dragDz;
    private boolean moved;
    private List<double[]> guides = List.of();

    private record SpaceNode(FSHFHome.Space space, Group group, Translate position, Rotate turn, FSHFRoomView room) {}

    public FSHFHomeView(FSHFCatalog catalog, FSHFModelLibrary models) {
        this.catalog = catalog;
        this.models = models;
        ground.setMaterial(FSHFMaterials.PLINTH);
        ground.setMouseTransparent(true);

        javafx.scene.AmbientLight groundLight = new javafx.scene.AmbientLight(Color.gray(0.85));
        groundLight.getScope().add(ground);
        Group world = new Group(ground, groundLight, spacesGroup);
        world.getTransforms().add(new Rotate(180, Rotate.X_AXIS));
        subScene = new SubScene(new Group(world, cam.camera), 100, 100, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.TRANSPARENT);
        subScene.setCamera(cam.camera);
        canvas.setMouseTransparent(true);
        Pane canvasHolder = new Pane(canvas);
        canvasHolder.setMouseTransparent(true);
        root.getChildren().addAll(subScene, canvasHolder);
        root.setBackground(new Background(new BackgroundFill(BACKGROUND, null, null)));
        root.setMinSize(0, 0);
        root.widthProperty().addListener((obs, o, n) -> resize());
        root.heightProperty().addListener((obs, o, n) -> resize());
        installMouse();
    }

    public Node node() {
        return root;
    }

    public void setOnSelect(Consumer<FSHFHome.Space> c) {
        onSelect = c;
    }

    public void setOnOpen(Consumer<FSHFHome.Space> c) {
        onOpen = c;
    }

    public void setOnMoved(Runnable r) {
        onMoved = r;
    }

    public void setOnStatus(Consumer<String> c) {
        onStatus = c;
    }

    public boolean isPlan() {
        return cam.mode == FSHFViewCamera.Mode.PLAN;
    }

    public void setPlan(boolean plan) {
        cam.mode = plan ? FSHFViewCamera.Mode.PLAN : FSHFViewCamera.Mode.THREE_D;
        cam.update();
        updateLevels();
        frame();
    }

    public void select(FSHFHome.Space s) {
        selected = s;
        if (s != null && s.level != level) setLevel(s.level);
        render();
    }

    public int level() {
        return level;
    }

    public void setLevel(int l) {
        level = Math.max(0, l);
        if (selected != null && selected.level != level) selected = null;
        updateLevels();
        render();
    }

    public void show(FSHFHome h) {
        home = h;
        spacesGroup.getChildren().clear();
        nodes.clear();
        if (selected != null && !h.spaces.contains(selected)) selected = null;
        for (FSHFHome.Space s : h.spaces) {
            FSHFDesign d = new FSHFDesign(catalog);

            FSHFProject p = s.project.copy();
            p.openings.addAll(h.passagesInto(s));
            d.load(p);
            FSHFRoomView room = new FSHFRoomView();
            room.rebuild(d);
            FSHFFurnitureView furniture = new FSHFFurnitureView(models, msg -> { });
            furniture.setOnModelLoaded(this::render);
            furniture.sync(d);
            FSHFLightView lights = new FSHFLightView();
            lights.rebuild(d);
            Translate position = new Translate();
            Rotate turn = new Rotate(0, Rotate.Y_AXIS);
            Group g = new Group(room.group, furniture.group, lights.group);
            g.getTransforms().addAll(position, turn);
            g.getProperties().put(SPACE, s);

            for (Node n : lights.group.getChildren()) {
                if (n instanceof LightBase light) light.getScope().add(g);

                else if (n.getBoundsInParent().getMinY() > 0.05) n.setVisible(false);
            }

            hidePlinth(room.group);
            spacesGroup.getChildren().add(g);
            SpaceNode node = new SpaceNode(s, g, position, turn, room);
            place(node);
            nodes.add(node);
        }
        if (level >= h.levels()) level = h.levels() - 1;
        updateLevels();
        updateGround();
        render();
    }

    private void place(SpaceNode n) {
        FSHFHome.Space s = n.space();
        double[] m = s.matrix();
        n.position().setX(m[2]);
        n.position().setY(home.elevation(s.level));
        n.position().setZ(m[5]);

        n.turn().setAngle(-s.rotation);
    }

    private void updateLevels() {
        boolean plan = cam.mode == FSHFViewCamera.Mode.PLAN;
        for (SpaceNode n : nodes) {
            int l = n.space().level;
            n.group().setVisible(plan ? l == level : l <= level);
            n.group().setMouseTransparent(l != level);
        }
    }

    private static void hidePlinth(Group g) {
        for (Node n : g.getChildren()) {
            if (n instanceof Box b && b.getMaterial() == FSHFMaterials.PLINTH) b.setVisible(false);
        }
    }

    public void moved(FSHFHome.Space s) {
        for (SpaceNode n : nodes) if (n.space() == s) place(n);
        updateGround();
        render();
    }

    public void frame() {
        double[] b = bounds();
        double y0 = home.elevation(level), h = y0 + home.levelHeight(level);
        cam.frame(b[0], isPlan() ? y0 : 0, b[1], b[2], h, b[3]);
        render();
    }

    public void zoom(double factor) {
        cam.zoom(factor);
        render();
    }

    private double[] bounds() {
        if (home.spaces.isEmpty()) return new double[] {0, 0, 4, 4};
        double[] b = {Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        for (FSHFHome.Space s : home.spaces) {
            double[] o = s.outer();
            b[0] = Math.min(b[0], o[0]);
            b[1] = Math.min(b[1], o[1]);
            b[2] = Math.max(b[2], o[2]);
            b[3] = Math.max(b[3], o[3]);
        }
        return b;
    }

    private void updateGround() {
        double[] b = bounds();
        double m = 0.6;
        ground.setWidth(b[2] - b[0] + 2 * m);
        ground.setDepth(b[3] - b[1] + 2 * m);
        ground.setTranslateX((b[0] + b[2]) / 2);
        ground.setTranslateZ((b[1] + b[3]) / 2);
        ground.setTranslateY(-0.05);
    }

    private void resize() {
        double w = root.getWidth(), h = root.getHeight();
        if (w <= 0 || h <= 0) return;
        subScene.setWidth(w);
        subScene.setHeight(h);
        canvas.setWidth(w);
        canvas.setHeight(h);
        cam.setViewport(w, h);
        render();
    }

    public WritableImage snapshot(double scale) {
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.web("#2b3036"));
        params.setTransform(javafx.scene.transform.Transform.scale(scale, scale));
        return root.snapshot(params, null);
    }

    private void render() {
        boolean plan = cam.mode == FSHFViewCamera.Mode.PLAN;
        Point3D eye = cam.eye();
        for (SpaceNode n : nodes) {
            FSHFHome.Space s = n.space();
            double[] local = s.toLocal(eye.getX(), eye.getZ());
            Point3D localEye = new Point3D(local[0], eye.getY() - home.elevation(s.level), local[1]);
            n.room().updateVisibility(localEye, plan || s.level < level, false, null);
        }
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        g.setLineDashes();
        double y = home.elevation(level);
        for (FSHFHome.Space s : home.spaces) {
            if (s.level != level) continue;
            FSHFRoomDims r = s.project.room;
            if (s == selected) {
                double[] o = s.outer();
                g.setStroke(SELECT);
                g.setLineWidth(3);
                polygon(g, new double[][] {{o[0], o[1]}, {o[2], o[1]}, {o[2], o[3]}, {o[0], o[3]}}, y);
            }
            Point2D c = cam.project(new Point3D(s.x + s.planWidth() / 2, y + 0.05, s.z + s.planLength() / 2));
            if (c == null) continue;
            String info = FSHFUnits.number(FSHFUnits.round(r.width() * r.length())) + " m²  ·  " + FSHFUnits.number(r.width()) + " × "
                    + FSHFUnits.number(r.length()) + " m";
            label(g, c, s.name, plan || s == selected ? info : null, s == selected);
        }
        g.setStroke(GUIDE);
        g.setLineWidth(2.5);
        g.setLineDashes(10, 6);
        for (double[] line : guides) {
            Point2D a = cam.project(new Point3D(line[0], y + 0.03, line[1])), b = cam.project(new Point3D(line[2], y + 0.03, line[3]));
            if (a != null && b != null) g.strokeLine(a.getX(), a.getY(), b.getX(), b.getY());
        }
        g.setLineDashes();
    }

    private void polygon(GraphicsContext g, double[][] pts, double y) {
        for (int i = 0; i < pts.length; i++) {
            double[] a = pts[i], b = pts[(i + 1) % pts.length];
            Point2D pa = cam.project(new Point3D(a[0], y + 0.03, a[1])), pb = cam.project(new Point3D(b[0], y + 0.03, b[1]));
            if (pa != null && pb != null) g.strokeLine(pa.getX(), pa.getY(), pb.getX(), pb.getY());
        }
    }

    private static void label(GraphicsContext g, Point2D at, String name, String info, boolean active) {
        double w = Math.max(name.length() * 8.2, info == null ? 0 : info.length() * 6.2) + 20, h = info == null ? 22 : 38;
        double x = at.getX() - w / 2, y = at.getY() - h / 2;
        g.setFill(active ? Color.web("#2f6fcaee") : Color.web("#15181bdd"));
        g.fillRoundRect(x, y, w, h, 10, 10);
        if (active) {
            g.setStroke(Color.web("#8cc2ff"));
            g.setLineWidth(1.5);
            g.strokeRoundRect(x, y, w, h, 10, 10);
        }
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.setFill(Color.WHITE);
        g.setFont(NAME_FONT);
        g.fillText(name, at.getX(), y + (info == null ? h / 2 : 12));
        if (info == null) return;
        g.setFill(Color.web("#c4cbd3"));
        g.setFont(INFO_FONT);
        g.fillText(info, at.getX(), y + 27);
    }

    private Point3D floorPoint(double x, double y) {
        return cam.planePoint(x, y, home.elevation(level));
    }

    private FSHFHome.Space pick(javafx.scene.input.PickResult r) {
        for (Node n = r == null ? null : r.getIntersectedNode(); n != null; n = n.getParent()) {
            Object s = n.getProperties().get(SPACE);
            if (s instanceof FSHFHome.Space space) return space;
        }
        return null;
    }

    private void installMouse() {
        subScene.setOnMousePressed(e -> {
            root.requestFocus();
            pressX = lastX = e.getX();
            pressY = lastY = e.getY();
            dragging = null;
            moved = false;
            if (e.getButton() != MouseButton.PRIMARY) return;
            FSHFHome.Space s = pick(e.getPickResult());

            if (s != null && (isPlan() || s == selected)) {
                Point3D p = floorPoint(e.getX(), e.getY());
                if (p == null) return;
                dragging = s;
                dragDx = s.x - p.getX();
                dragDz = s.z - p.getZ();
                subScene.setCursor(Cursor.CLOSED_HAND);
            }
        });
        subScene.setOnMouseDragged(e -> {
            double dx = e.getX() - lastX, dy = e.getY() - lastY;
            lastX = e.getX();
            lastY = e.getY();
            if (dragging != null) {
                Point3D p = floorPoint(e.getX(), e.getY());
                if (p == null) return;
                drag(dragging, p.getX() + dragDx, p.getZ() + dragDz, !e.isAltDown());
                return;
            }
            if (e.getButton() == MouseButton.PRIMARY && !isPlan()) cam.rotate(dx, dy);
            else cam.pan(dx, dy);
            render();
        });
        subScene.setOnMouseReleased(e -> {
            boolean click = Math.hypot(e.getX() - pressX, e.getY() - pressY) < CLICK_TOLERANCE;
            if (dragging != null) {
                FSHFHome.Space s = dragging;
                dragging = null;
                guides = List.of();
                subScene.setCursor(Cursor.DEFAULT);
                render();
                if (moved) {
                    onMoved.run();
                    return;
                }
                if (!click) return;
            }
            if (e.getButton() == MouseButton.PRIMARY && click) {
                FSHFHome.Space s = pick(e.getPickResult());
                if (e.getClickCount() >= 2 && s != null) {
                    onOpen.accept(s);
                    return;
                }
                selected = s;
                onSelect.accept(s);
                render();
            }
        });
        subScene.setOnScroll(e -> {
            if (e.getDeltaY() != 0) zoom(e.getDeltaY() > 0 ? 0.9 : 1 / 0.9);
        });
        subScene.setOnMouseMoved(e -> {
            FSHFHome.Space s = pick(e.getPickResult());
            subScene.setCursor(s == null ? Cursor.DEFAULT : (isPlan() || s == selected) ? Cursor.OPEN_HAND : Cursor.HAND);
        });
    }

    private void drag(FSHFHome.Space s, double x, double z, boolean magnet) {
        double t = s.project.room.wallThickness(), w = s.planWidth() + 2 * t, l = s.planLength() + 2 * t;
        List<double[]> lines = new ArrayList<>();
        if (magnet) {
            double minX = x - t, minZ = z - t;
            double bestX = SNAP, bestZ = SNAP, snapX = 0, snapZ = 0;
            double[] gx = null, gz = null;
            for (FSHFHome.Space o : home.spaces) {
                if (o == s || o.level != s.level) continue;
                double[] b = o.outer();

                for (double[] pair : new double[][] {{minX, b[2]}, {minX + w, b[0]}, {minX, b[0]}, {minX + w, b[2]}}) {
                    double d = pair[1] - pair[0];
                    if (Math.abs(d) < bestX) {
                        bestX = Math.abs(d);
                        snapX = d;
                        gx = new double[] {pair[1], Math.min(b[1], minZ), pair[1], Math.max(b[3], minZ + l)};
                    }
                }
                for (double[] pair : new double[][] {{minZ, b[3]}, {minZ + l, b[1]}, {minZ, b[1]}, {minZ + l, b[3]}}) {
                    double d = pair[1] - pair[0];
                    if (Math.abs(d) < bestZ) {
                        bestZ = Math.abs(d);
                        snapZ = d;
                        gz = new double[] {Math.min(b[0], minX), pair[1], Math.max(b[2], minX + w), pair[1]};
                    }
                }
            }
            x += snapX;
            z += snapZ;
            if (gx != null) lines.add(gx);
            if (gz != null) lines.add(gz);
        }
        double[][] tries = {{x, z}, {x, s.z}, {s.x, z}};
        for (double[] p : tries) {
            double nx = FSHFUnits.round(p[0]), nz = FSHFUnits.round(p[1]);
            if (nx == s.x && nz == s.z) break;
            if (home.collision(s, nx, nz) != null) continue;
            s.x = nx;
            s.z = nz;
            moved = true;
            moved(s);
            break;
        }
        guides = lines;
        FSHFHome.Space other = home.collision(s, FSHFUnits.round(x), FSHFUnits.round(z));
        onStatus.accept(s.name + ": (" + FSHFUnits.number(s.x) + "; " + FSHFUnits.number(s.z) + ") m"
                + (other != null ? "   · se encimaría con " + other.name : magnet ? "   (Alt: sin imán)" : ""));
        render();
    }
}
