package pe.edu.unaj.amoblador3d.scene;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javafx.animation.AnimationTimer;
import javafx.geometry.Point3D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SnapshotParameters;
import javafx.scene.SubScene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.transform.Rotate;
import pe.edu.unaj.amoblador3d.design.FSHFDesign;
import pe.edu.unaj.amoblador3d.design.FSHFSnapper;
import pe.edu.unaj.amoblador3d.model.FSHFCameraView;
import pe.edu.unaj.amoblador3d.model.FSHFColumn;
import pe.edu.unaj.amoblador3d.model.FSHFFootprint;
import pe.edu.unaj.amoblador3d.model.FSHFFurniture;
import pe.edu.unaj.amoblador3d.model.FSHFOpening;
import pe.edu.unaj.amoblador3d.model.FSHFRoomDims;
import pe.edu.unaj.amoblador3d.model.FSHFSurface;
import pe.edu.unaj.amoblador3d.model.FSHFWallSide;
import pe.edu.unaj.amoblador3d.util.FSHFUnits;

public final class FSHFSceneView {
    private static final String ENTITY = "amoblador.entity";
    private static final double CLICK_TOLERANCE = 5;
    private static final LinearGradient BACKGROUND = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#46505c")), new Stop(0.55, Color.web("#2b3036")), new Stop(1, Color.web("#1d2125")));

    public enum Mode { THREE_D, PLAN, WALK }

    private static final LinearGradient NIGHT = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#141b2b")), new Stop(1, Color.web("#07090d")));
    private static final double WALK_SPEED = 1.4, TURN_SPEED = 1.8, BODY = 0.2;

    private final FSHFDesign design;
    private final StackPane root = new StackPane();
    private final SubScene subScene;
    private final FSHFViewCamera cam = new FSHFViewCamera();
    private final FSHFRoomView room = new FSHFRoomView();
    private final FSHFLightView lights = new FSHFLightView();
    private final FSHFFurnitureView furniture;
    private final FSHFOverlay overlay = new FSHFOverlay();

    private List<Object> selection = List.of();
    private boolean magnet = true;
    private List<FSHFSnapper.Guide> guides = List.of();
    private boolean showDimensions = true;
    private boolean night;
    private final Set<KeyCode> walkKeys = EnumSet.noneOf(KeyCode.class);
    private boolean running;
    private long lastFrame;
    private boolean dirty = true;

    private boolean measuring;
    private Point3D measureA, measureB, measureHover;
    private Consumer<String> onMeasure = text -> {};

    private BiConsumer<Object, Boolean> onSelect = (e, add) -> {};
    private Consumer<List<FSHFFurniture>> onBandSelect = list -> {};
    private Consumer<String> onNotice = text -> {};
    private Runnable onDrag = () -> {};
    private Runnable onDragEnd = () -> {};
    private Runnable onRendered = () -> {};

    private double pressX, pressY, lastX, lastY;
    private Object pressEntity;
    private FSHFFurniture dragItem;
    private List<FSHFFurniture> dragGroup = List.of();
    private double dragDx, dragDz;
    private boolean dragMoved;

    private boolean bandArmed, banding;
    private double bandX, bandY;

    private final FSHFFurnitureHandles handles = new FSHFFurnitureHandles();
    private FSHFFurnitureHandles.Kind hoverHandle;
    private FSHFFurniture hover;

    public FSHFSceneView(FSHFDesign design, FSHFModelLibrary models, Consumer<String> onWarning) {
        this.design = design;
        furniture = new FSHFFurnitureView(models, onWarning);
        furniture.setOnModelLoaded(this::redraw);

        Group world = new Group(room.group, furniture.group, lights.group);
        world.getTransforms().add(new Rotate(180, Rotate.X_AXIS));
        Group sceneRoot = new Group(world, cam.camera);
        subScene = new SubScene(sceneRoot, 100, 100, true, SceneAntialiasing.BALANCED);

        subScene.setFill(Color.TRANSPARENT);
        subScene.setCamera(cam.camera);

        overlay.canvas.setMouseTransparent(true);
        Pane canvasHolder = new Pane(overlay.canvas);
        canvasHolder.setMouseTransparent(true);
        root.getChildren().addAll(subScene, canvasHolder);
        root.setBackground(new Background(new BackgroundFill(BACKGROUND, null, null)));
        root.setMinSize(0, 0);
        root.setFocusTraversable(true);

        root.widthProperty().addListener((obs, o, n) -> resize());
        root.heightProperty().addListener((obs, o, n) -> resize());

        design.addListener(changes -> {
            if (changes.contains(FSHFDesign.Change.ROOM) || changes.contains(FSHFDesign.Change.SURFACES)) room.rebuild(design);
            if (changes.contains(FSHFDesign.Change.LIGHTS)) lights.rebuild(design);
            if (changes.contains(FSHFDesign.Change.FURNITURE)) furniture.sync(design);
            redraw();
        });
        room.rebuild(design);
        lights.rebuild(design);
        furniture.sync(design);

        installMouse();
        installWalkKeys();
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                double dt = lastFrame == 0 ? 0 : Math.min(0.05, (now - lastFrame) / 1e9);
                lastFrame = now;
                if (cam.mode == FSHFViewCamera.Mode.WALK && !walkKeys.isEmpty()) walkStep(dt);
                if (dirty) render();
            }
        }.start();
    }

    static void tag(Node node, Object entity) {
        node.getProperties().put(ENTITY, entity);
    }

    public Node node() {
        return root;
    }

    public void setOnSelect(BiConsumer<Object, Boolean> c) {
        onSelect = c;
    }

    public void setOnBandSelect(Consumer<List<FSHFFurniture>> c) {
        onBandSelect = c;
    }

    public void setOnNotice(Consumer<String> c) {
        onNotice = c;
    }

    public void setMagnet(boolean on) {
        magnet = on;
    }

    public void setOnDrag(Runnable r) {
        onDrag = r;
    }

    public void setOnDragEnd(Runnable r) {
        onDragEnd = r;
    }

    public void setOnMeasure(Consumer<String> c) {
        onMeasure = c;
    }

    public boolean isMeasuring() {
        return measuring;
    }

    public void setMeasuring(boolean on) {
        measuring = on;
        clearMeasure();
        subScene.setCursor(on ? Cursor.CROSSHAIR : Cursor.DEFAULT);
    }

    public void clearMeasure() {
        measureA = measureB = measureHover = null;
        redraw();
    }

    public void setSelection(List<Object> entities) {
        selection = List.copyOf(entities);
        redraw();
    }

    private FSHFFurniture handleTarget() {
        if (selection.size() != 1 || !(selection.get(0) instanceof FSHFFurniture f) || f.locked) return null;
        if (measuring || cam.mode == FSHFViewCamera.Mode.WALK || dragItem != null) return null;
        return design.furniture().contains(f) ? f : null;
    }

    private List<FSHFFurniture> selectedFurniture() {
        List<FSHFFurniture> list = new ArrayList<>();
        for (Object o : selection) if (o instanceof FSHFFurniture f) list.add(f);
        return list;
    }

    public Mode mode() {
        return switch (cam.mode) {
            case PLAN -> Mode.PLAN;
            case WALK -> Mode.WALK;
            default -> Mode.THREE_D;
        };
    }

    public void setMode(Mode mode) {
        cam.mode = switch (mode) {
            case PLAN -> FSHFViewCamera.Mode.PLAN;
            case WALK -> FSHFViewCamera.Mode.WALK;
            default -> FSHFViewCamera.Mode.THREE_D;
        };
        walkKeys.clear();
        cam.update();
        redraw();
    }

    public boolean isNight() {
        return night;
    }

    public void setNight(boolean on) {
        night = on;
        lights.setNight(on);
        lights.rebuild(design);
        root.setBackground(new Background(new BackgroundFill(on ? NIGHT : BACKGROUND, null, null)));
        redraw();
    }

    public boolean dimensionsVisible() {
        return showDimensions;
    }

    public void setDimensionsVisible(boolean visible) {
        showDimensions = visible;
        redraw();
    }

    public void frameRoom() {
        if (cam.mode == FSHFViewCamera.Mode.WALK) {
            startWalk();
            return;
        }
        FSHFRoomDims r = design.room();
        double t = r.wallThickness();
        cam.frame(-t, 0, -t, r.width() + t, r.height(), r.length() + t);
        redraw();
    }

    public void refreshFurniture() {
        furniture.invalidate();
        furniture.sync(design);
        redraw();
    }

    public double[] floorAt(double x, double y) {
        if (cam.mode == FSHFViewCamera.Mode.WALK && y < root.getHeight() / 2) return null;
        Point3D p = cam.floorPoint(x, y);
        return p == null ? null : new double[] {p.getX(), p.getZ()};
    }

    public void zoom(double factor) {
        cam.zoom(factor);
        redraw();
    }

    public void redraw() {
        dirty = true;
    }

    private void resize() {
        double w = root.getWidth(), h = root.getHeight();
        if (w <= 0 || h <= 0) return;
        subScene.setWidth(w);
        subScene.setHeight(h);
        overlay.canvas.setWidth(w);
        overlay.canvas.setHeight(h);
        cam.setViewport(w, h);
        redraw();
    }

    private void render() {
        dirty = false;
        FSHFWallSide selectedWall = selection.size() == 1 && selection.get(0) instanceof FSHFSurface s ? s.wallSide() : null;
        room.updateVisibility(cam.eye(), cam.mode == FSHFViewCamera.Mode.PLAN, cam.mode == FSHFViewCamera.Mode.WALK, selectedWall);
        overlay.draw(cam, design, lights, selection, showDimensions && cam.mode != FSHFViewCamera.Mode.WALK);
        overlay.drawGuides(guides);
        if (banding) overlay.drawBand(pressX, pressY, bandX, bandY);
        if (hover != null && !selection.contains(hover) && design.furniture().contains(hover)) overlay.drawHover(hover);
        FSHFFurniture editable = handleTarget();
        if (editable != null) {
            boolean plan = cam.mode == FSHFViewCamera.Mode.PLAN;
            overlay.drawHandles(editable, FSHFFurnitureHandles.of(editable, plan), handles.active() ? handles.kind() : hoverHandle);
        }
        if (measuring && measureA != null) overlay.drawMeasure(measureA, measureB != null ? measureB : measureHover);
        onRendered.run();
    }

    public void setOnRendered(Runnable r) {
        onRendered = r;
    }

    public javafx.geometry.Point2D screenAbove(FSHFFurniture f) {
        if (cam.mode == FSHFViewCamera.Mode.WALK || dragItem != null || handles.active()) return null;
        javafx.geometry.Point2D p = cam.project(new Point3D(f.x, f.height, f.z));
        if (p == null || p.getX() < 0 || p.getY() < 0 || p.getX() > root.getWidth() || p.getY() > root.getHeight()) return null;
        return p;
    }

    public WritableImage snapshot(double scale) {
        List<Object> previous = selection;
        boolean wasMeasuring = measuring;
        selection = List.of();
        measuring = false;
        render();
        double w = root.getWidth(), h = root.getHeight();
        int width = (int) Math.round(w * scale), height = (int) Math.round(h * scale);
        try {
            subScene.setWidth(width);
            subScene.setHeight(height);
            cam.setViewport(width, height);
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            WritableImage scene3d = subScene.snapshot(params, null);
            cam.setViewport(w, h);

            Canvas canvas = new Canvas(width, height);
            GraphicsContext g = canvas.getGraphicsContext2D();
            g.setFill(night ? NIGHT : BACKGROUND);
            g.fillRect(0, 0, width, height);
            g.drawImage(scene3d, 0, 0);
            g.save();
            g.scale(scale, scale);
            overlay.drawOn(g, cam, design, lights, List.of(), showDimensions && cam.mode != FSHFViewCamera.Mode.WALK);
            g.restore();
            return canvas.snapshot(null, null);
        } finally {
            subScene.setWidth(w);
            subScene.setHeight(h);
            cam.setViewport(w, h);
            selection = previous;
            measuring = wasMeasuring;
            redraw();
        }
    }

    public WritableImage planImage(double scale) {
        FSHFViewCamera.State state = cam.state();
        boolean dims = showDimensions;
        cam.mode = FSHFViewCamera.Mode.PLAN;
        frameRoom();
        showDimensions = true;
        try {
            return snapshot(scale);
        } finally {
            showDimensions = dims;
            cam.restore(state);
            redraw();
        }
    }

    public WritableImage viewImage(double scale) {
        if (cam.mode != FSHFViewCamera.Mode.PLAN) return snapshot(scale);
        FSHFViewCamera.State state = cam.state();
        cam.mode = FSHFViewCamera.Mode.THREE_D;
        frameRoom();
        try {
            return snapshot(scale);
        } finally {
            cam.restore(state);
            redraw();
        }
    }

    private double eyeHeight() {
        return Math.min(1.6, design.room().height() - 0.25);
    }

    private void startWalk() {
        for (FSHFOpening o : design.openings()) {
            if (o.type.isDoor()) {
                walkFromDoor(o);
                return;
            }
        }
        FSHFRoomDims r = design.room();
        cam.walkTo(r.width() / 2, eyeHeight(), r.length() - 0.5, 0);
        cam.setWalkEye(walkable(cam.walkEye(), cam.walkEye()));
        redraw();
    }

    public void walkFromDoor(FSHFOpening door) {
        cam.mode = FSHFViewCamera.Mode.WALK;
        FSHFWallFrame wall = FSHFWallFrame.of(door.wall, design.room());
        Point3D n = wall.interiorNormal();
        Point3D at = wall.point(door.offset + door.width / 2, 0, -0.6);
        cam.walkTo(at.getX(), eyeHeight(), at.getZ(), Math.atan2(-n.getX(), -n.getZ()));
        redraw();
    }

    private Point3D walkable(Point3D from, Point3D to) {
        for (Point3D p : new Point3D[] {to, new Point3D(to.getX(), to.getY(), from.getZ()), new Point3D(from.getX(), to.getY(), to.getZ())}) {
            if (free(p.getX(), p.getZ())) return p;
        }
        return free(from.getX(), from.getZ()) ? from : to;
    }

    private boolean free(double x, double z) {
        FSHFRoomDims r = design.room();
        if (x < BODY || z < BODY || x > r.width() - BODY || z > r.length() - BODY) return false;
        FSHFFootprint body = new FSHFFootprint(x, z, BODY, BODY, 0);
        for (FSHFFurniture f : design.furniture()) if (FSHFFootprint.of(f).overlaps(body)) return false;
        for (FSHFColumn c : design.columns()) if (c.footprint().overlaps(body)) return false;
        return true;
    }

    private static boolean isWalkKey(KeyCode k) {
        return switch (k) {
            case W, A, S, D, UP, DOWN, LEFT, RIGHT -> true;
            default -> false;
        };
    }

    private void installWalkKeys() {
        root.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (cam.mode != FSHFViewCamera.Mode.WALK || !isWalkKey(e.getCode()) || e.isShortcutDown() || e.isAltDown()) return;
            walkKeys.add(e.getCode());
            running = e.isShiftDown();
            e.consume();
        });
        root.addEventHandler(KeyEvent.KEY_RELEASED, e -> {
            if (walkKeys.remove(e.getCode())) e.consume();
            running = e.isShiftDown();
        });
        root.focusedProperty().addListener((obs, o, focused) -> {
            if (!focused) walkKeys.clear();
        });
    }

    private void walkStep(double dt) {
        double turn = (walkKeys.contains(KeyCode.LEFT) ? 1 : 0) - (walkKeys.contains(KeyCode.RIGHT) ? 1 : 0);
        if (turn != 0) cam.turn(turn * TURN_SPEED * dt);
        double ahead = (has(KeyCode.W, KeyCode.UP) ? 1 : 0)
                - (has(KeyCode.S, KeyCode.DOWN) ? 1 : 0);
        double side = (walkKeys.contains(KeyCode.D) ? 1 : 0) - (walkKeys.contains(KeyCode.A) ? 1 : 0);
        if (ahead != 0 || side != 0) {
            Point3D dir = cam.walkForward().multiply(ahead).add(cam.walkRight().multiply(side)).normalize();
            Point3D to = cam.walkEye().add(dir.multiply(WALK_SPEED * (running ? 2 : 1) * dt));
            cam.setWalkEye(walkable(cam.walkEye(), to));
        }
        redraw();
    }

    private boolean has(KeyCode a, KeyCode b) {
        return walkKeys.contains(a) || walkKeys.contains(b);
    }

    public FSHFCameraView currentView(String name) {
        double[] v = cam.values();
        FSHFCameraView.Kind kind = switch (cam.mode) {
            case PLAN -> FSHFCameraView.Kind.PLAN;
            case WALK -> FSHFCameraView.Kind.WALK;
            default -> FSHFCameraView.Kind.ORBIT;
        };
        return new FSHFCameraView(name, kind, v[0], v[1], v[2], v[3], v[4], v[5]);
    }

    public void showView(FSHFCameraView v) {
        FSHFViewCamera.Mode m = switch (v.kind()) {
            case PLAN -> FSHFViewCamera.Mode.PLAN;
            case WALK -> FSHFViewCamera.Mode.WALK;
            default -> FSHFViewCamera.Mode.THREE_D;
        };
        walkKeys.clear();
        cam.apply(m, v.x(), v.y(), v.z(), v.yaw(), v.pitch(), v.distance());
        redraw();
    }

    private static final double SNAP_WALL = 0.06, SNAP_CORNER = 0.1;

    private Point3D measurePoint(double sx, double sy) {
        Point3D p = cam.floorPoint(sx, sy);
        if (p == null) return null;
        FSHFRoomDims r = design.room();
        double x = p.getX(), z = p.getZ();
        double best = SNAP_CORNER;
        double[] corner = null;
        List<FSHFFootprint> shapes = new ArrayList<>();
        for (FSHFFurniture f : design.furniture()) shapes.add(FSHFFootprint.of(f));
        for (FSHFColumn col : design.columns()) shapes.add(col.footprint());
        for (FSHFFootprint shape : shapes) {
            for (double[] c : shape.corners()) {
                double d = Math.hypot(c[0] - x, c[1] - z);
                if (d < best) {
                    best = d;
                    corner = c;
                }
            }
        }
        if (corner != null) return new Point3D(corner[0], 0, corner[1]);
        return new Point3D(snap(x, r.width()), 0, snap(z, r.length()));
    }

    private static double snap(double v, double max) {
        if (Math.abs(v) < SNAP_WALL) return 0;
        if (Math.abs(v - max) < SNAP_WALL) return max;
        return v;
    }

    private void measureClick(double sx, double sy) {
        Point3D p = measurePoint(sx, sy);
        if (p == null) return;
        if (measureA == null || measureB != null) {
            measureA = p;
            measureB = null;
            measureHover = p;
            onMeasure.accept("Medir: haz clic en el segundo punto (Esc para terminar).");
        } else {
            measureB = p;
            onMeasure.accept(measureText(measureA, measureB));
        }
        redraw();
    }

    static String measureText(Point3D a, Point3D b) {
        return "Distancia: " + FSHFUnits.meters(a.distance(b)) + "   (X: " + FSHFUnits.meters(Math.abs(b.getX() - a.getX()))
                + " · Z: " + FSHFUnits.meters(Math.abs(b.getZ() - a.getZ())) + ")";
    }

    private void installMouse() {
        subScene.setOnMousePressed(e -> {
            root.requestFocus();
            pressX = lastX = e.getX();
            pressY = lastY = e.getY();
            pressEntity = pick(e.getPickResult());
            dragItem = null;
            dragMoved = false;
            bandArmed = banding = false;
            if (measuring) return;
            if (e.getButton() != MouseButton.PRIMARY) return;
            FSHFFurniture target = handleTarget();
            if (target != null) {
                FSHFFurnitureHandles.Kind k = FSHFFurnitureHandles.hit(cam, target, e.getX(), e.getY(), cam.mode == FSHFViewCamera.Mode.PLAN);
                if (k != null) {
                    handles.begin(k, target, e.getX(), e.getY());
                    redraw();
                    return;
                }
            }
            if (e.isShiftDown() || e.isShortcutDown()) {
                bandArmed = true;
                bandX = e.getX();
                bandY = e.getY();
                return;
            }
            if (pressEntity instanceof FSHFFurniture f) {
                Point3D p = cam.floorPoint(e.getX(), e.getY());
                if (p == null) return;

                List<FSHFFurniture> group = selectedFurniture();
                if (group.size() < 2 || !group.contains(f)) {
                    group = List.of(f);
                    onSelect.accept(f, false);
                }
                FSHFFurniture locked = design.firstLocked(group);
                if (locked != null) {
                    onNotice.accept(design.label(locked) + " está bloqueado: no se puede mover.");
                    return;
                }
                dragItem = f;
                dragGroup = group;
                dragDx = f.x - p.getX();
                dragDz = f.z - p.getZ();
                subScene.setCursor(Cursor.CLOSED_HAND);
            }
        });

        subScene.setOnMouseDragged(e -> {
            double dx = e.getX() - lastX, dy = e.getY() - lastY;
            lastX = e.getX();
            lastY = e.getY();
            if (handles.active()) {
                String text = handles.drag(design, cam, e.getX(), e.getY(), e.isShiftDown());
                if (text != null) onMeasure.accept(text);
                onDrag.run();
                redraw();
                return;
            }
            if (bandArmed) {
                bandX = e.getX();
                bandY = e.getY();
                banding = banding || Math.hypot(bandX - pressX, bandY - pressY) >= CLICK_TOLERANCE;
                redraw();
                return;
            }
            if (dragItem != null) {
                Point3D p = cam.floorPoint(e.getX(), e.getY());
                if (p == null) return;
                double beforeX = dragItem.x, beforeZ = dragItem.z;
                List<FSHFSnapper.Guide> g = design.drag(dragGroup, dragItem, p.getX() + dragDx, p.getZ() + dragDz, magnet && !e.isAltDown());
                guides = g == null ? List.of() : g;
                if (dragItem.x != beforeX || dragItem.z != beforeZ) {
                    dragMoved = true;
                    onDrag.run();
                }
                redraw();
                return;
            }
            boolean plan = cam.mode == FSHFViewCamera.Mode.PLAN;
            if (e.getButton() == MouseButton.PRIMARY && !plan) cam.rotate(dx, dy);
            else cam.pan(dx, dy);
            redraw();
        });

        subScene.setOnMouseReleased(e -> {
            if (handles.active()) {
                if (handles.end()) onDragEnd.run();
                redraw();
                return;
            }
            if (dragItem != null) {
                dragItem = null;
                dragGroup = List.of();
                guides = List.of();
                subScene.setCursor(Cursor.OPEN_HAND);
                redraw();
                if (dragMoved) onDragEnd.run();
                return;
            }
            boolean click = Math.hypot(e.getX() - pressX, e.getY() - pressY) < CLICK_TOLERANCE;
            if (bandArmed) {
                bandArmed = false;
                if (banding) {
                    banding = false;
                    onBandSelect.accept(furnitureInBand());
                    redraw();
                } else if (click) {
                    onSelect.accept(pressEntity, true);
                }
                return;
            }
            if (e.getButton() == MouseButton.PRIMARY && click) {
                if (measuring) measureClick(e.getX(), e.getY());
                else onSelect.accept(pressEntity, false);
            }
        });

        subScene.setOnScroll(e -> {
            if (e.getDeltaY() == 0) return;
            if (cam.mode == FSHFViewCamera.Mode.WALK) {
                Point3D step = cam.walkForward().multiply(e.getDeltaY() > 0 ? 0.3 : -0.3);
                cam.setWalkEye(walkable(cam.walkEye(), cam.walkEye().add(step)));
                redraw();
                return;
            }
            zoom(e.getDeltaY() > 0 ? 0.9 : 1 / 0.9);
        });

        subScene.setOnMouseExited(e -> {
            if (hover != null || hoverHandle != null) {
                hover = null;
                hoverHandle = null;
                redraw();
            }
        });

        subScene.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            if (measuring) {
                if (measureA != null && measureB == null) {
                    Point3D p = measurePoint(e.getX(), e.getY());
                    if (p != null) {
                        measureHover = p;
                        onMeasure.accept(measureText(measureA, p));
                        redraw();
                    }
                }
                return;
            }
            FSHFFurniture target = handleTarget();
            FSHFFurnitureHandles.Kind k = target == null ? null
                    : FSHFFurnitureHandles.hit(cam, target, e.getX(), e.getY(), cam.mode == FSHFViewCamera.Mode.PLAN);
            Object entity = k != null ? null : pick(e.getPickResult());
            FSHFFurniture over = entity instanceof FSHFFurniture f ? f : null;
            if (k != hoverHandle || over != hover) {
                hoverHandle = k;
                hover = over;
                redraw();
            }
            if (k != null) {
                subScene.setCursor(FSHFFurnitureHandles.cursor(k));
                return;
            }

            boolean object = entity != null && !(entity instanceof FSHFSurface);
            boolean movable = entity instanceof FSHFFurniture f && !f.locked;
            subScene.setCursor(movable ? Cursor.OPEN_HAND : object ? Cursor.HAND : Cursor.DEFAULT);
        });
    }

    private List<FSHFFurniture> furnitureInBand() {
        double x0 = Math.min(pressX, bandX), x1 = Math.max(pressX, bandX);
        double y0 = Math.min(pressY, bandY), y1 = Math.max(pressY, bandY);
        List<FSHFFurniture> inside = new ArrayList<>();
        for (FSHFFurniture f : design.furniture()) {
            javafx.geometry.Point2D s = cam.project(new Point3D(f.x, f.height / 2, f.z));
            if (s != null && s.getX() >= x0 && s.getX() <= x1 && s.getY() >= y0 && s.getY() <= y1) inside.add(f);
        }
        return inside;
    }

    private static Object pick(PickResult result) {
        for (Node n = result == null ? null : result.getIntersectedNode(); n != null; n = n.getParent()) {
            Object entity = n.getProperties().get(ENTITY);
            if (entity != null) return entity;
        }
        return null;
    }
}
