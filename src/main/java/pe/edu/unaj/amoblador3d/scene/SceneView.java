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
import pe.edu.unaj.amoblador3d.design.Design;
import pe.edu.unaj.amoblador3d.design.Snapper;
import pe.edu.unaj.amoblador3d.model.CameraView;
import pe.edu.unaj.amoblador3d.model.Footprint;
import pe.edu.unaj.amoblador3d.model.Furniture;
import pe.edu.unaj.amoblador3d.model.Opening;
import pe.edu.unaj.amoblador3d.model.OpeningType;
import pe.edu.unaj.amoblador3d.model.RoomDims;
import pe.edu.unaj.amoblador3d.model.Surface;
import pe.edu.unaj.amoblador3d.model.WallSide;
import pe.edu.unaj.amoblador3d.util.Units;

/**
 * Vista 3D: une la habitación, las luces, los muebles y la capa de cotas, y atiende el ratón.
 * <ul>
 *   <li>Clic sobre un elemento: lo selecciona (clic en el vacío: deselecciona).</li>
 *   <li>Arrastrar un mueble: lo mueve sobre el piso (la cámara se queda quieta).</li>
 *   <li>3D: botón izquierdo gira, derecho mueve, rueda acerca. Planta: izquierdo o derecho mueven.</li>
 * </ul>
 * Convención: 1 unidad = 1 metro.
 */
public final class SceneView {
    private static final String ENTITY = "amoblador.entity";
    private static final double CLICK_TOLERANCE = 5; // píxeles: si el ratón se movió más, fue un arrastre
    /** Fondo: degradado vertical, más claro arriba, para dar sensación de profundidad. */
    private static final LinearGradient BACKGROUND = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#46505c")), new Stop(0.55, Color.web("#2b3036")), new Stop(1, Color.web("#1d2125")));

    /** Vista 3D (órbita), planta y recorrido en primera persona. */
    public enum Mode { THREE_D, PLAN, WALK }

    /** Fondo de noche: casi negro, con un poco de azul arriba. */
    private static final LinearGradient NIGHT = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#141b2b")), new Stop(1, Color.web("#07090d")));
    /** Recorrido: velocidad al caminar (m/s), giro con las flechas (rad/s) y "radio" de la persona (m). */
    private static final double WALK_SPEED = 1.4, TURN_SPEED = 1.8, BODY = 0.2;

    private final Design design;
    private final StackPane root = new StackPane();
    private final SubScene subScene;
    private final ViewCamera cam = new ViewCamera();
    private final RoomView room = new RoomView();
    private final LightView lights = new LightView();
    private final FurnitureView furniture;
    private final Overlay overlay = new Overlay();

    /** Lo seleccionado: un elemento cualquiera o varios muebles. */
    private List<Object> selection = List.of();
    private boolean magnet = true;
    private List<Snapper.Guide> guides = List.of();
    private boolean showDimensions = true;
    private boolean night;
    /** Teclas de movimiento apretadas en el recorrido. */
    private final Set<KeyCode> walkKeys = EnumSet.noneOf(KeyCode.class);
    private boolean running;
    private long lastFrame;
    private boolean dirty = true;

    // Herramienta de medir: A y B son los extremos (B = null mientras se elige el segundo punto).
    private boolean measuring;
    private Point3D measureA, measureB, measureHover;
    private Consumer<String> onMeasure = text -> {};

    /** (entidad, agregar): agregar = true con Shift o Ctrl (selección múltiple). */
    private BiConsumer<Object, Boolean> onSelect = (e, add) -> {};
    private Consumer<List<Furniture>> onBandSelect = list -> {};
    private Consumer<String> onNotice = text -> {};
    private Runnable onDrag = () -> {};
    private Runnable onDragEnd = () -> {};

    // Estado del ratón
    private double pressX, pressY, lastX, lastY;
    private Object pressEntity;
    private Furniture dragItem;
    private List<Furniture> dragGroup = List.of();
    private double dragDx, dragDz;
    private boolean dragMoved;
    // Selección con rectángulo (Shift + arrastrar)
    private boolean bandArmed, banding;
    private double bandX, bandY;

    public SceneView(Design design, ModelLibrary models, Consumer<String> onWarning) {
        this.design = design;
        furniture = new FurnitureView(models, onWarning);
        furniture.setOnModelLoaded(this::redraw);

        // El mundo se gira 180° en X: así dentro de él Y apunta hacia arriba, como en el proyecto.
        Group world = new Group(room.group, furniture.group, lights.group);
        world.getTransforms().add(new Rotate(180, Rotate.X_AXIS));
        Group sceneRoot = new Group(world, cam.camera);
        subScene = new SubScene(sceneRoot, 100, 100, true, SceneAntialiasing.BALANCED);
        // El degradado va detrás de la escena 3D (que es transparente).
        subScene.setFill(Color.TRANSPARENT);
        subScene.setCamera(cam.camera);

        overlay.canvas.setMouseTransparent(true);
        Pane canvasHolder = new Pane(overlay.canvas);
        canvasHolder.setMouseTransparent(true);
        root.getChildren().addAll(subScene, canvasHolder);
        root.setBackground(new Background(new BackgroundFill(BACKGROUND, null, null)));
        root.setMinSize(0, 0);
        root.setFocusTraversable(true);

        // Ajustar el tamaño al del contenedor.
        root.widthProperty().addListener((obs, o, n) -> resize());
        root.heightProperty().addListener((obs, o, n) -> resize());

        design.addListener(changes -> {
            if (changes.contains(Design.Change.ROOM) || changes.contains(Design.Change.SURFACES)) room.rebuild(design);
            if (changes.contains(Design.Change.LIGHTS)) lights.rebuild(design);
            if (changes.contains(Design.Change.FURNITURE)) furniture.sync(design);
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
                if (cam.mode == ViewCamera.Mode.WALK && !walkKeys.isEmpty()) walkStep(dt);
                if (dirty) render();
            }
        }.start();
    }

    /** Marca un nodo como parte de una entidad (abertura, luz o mueble) para poder seleccionarla con el ratón. */
    static void tag(Node node, Object entity) {
        node.getProperties().put(ENTITY, entity);
    }

    public Node node() {
        return root;
    }

    public void setOnSelect(BiConsumer<Object, Boolean> c) {
        onSelect = c;
    }

    /** Muebles dentro del rectángulo dibujado con Shift + arrastrar. */
    public void setOnBandSelect(Consumer<List<Furniture>> c) {
        onBandSelect = c;
    }

    /** Avisos breves para la barra de estado (p. ej. "está bloqueado"). */
    public void setOnNotice(Consumer<String> c) {
        onNotice = c;
    }

    /** Imán: al arrastrar, los muebles se pegan a paredes y a otros muebles. */
    public void setMagnet(boolean on) {
        magnet = on;
    }

    public void setOnDrag(Runnable r) {
        onDrag = r;
    }

    public void setOnDragEnd(Runnable r) {
        onDragEnd = r;
    }

    /** Recibe el texto de la medición ("Distancia: 2,35 m") cada vez que cambia. */
    public void setOnMeasure(Consumer<String> c) {
        onMeasure = c;
    }

    public boolean isMeasuring() {
        return measuring;
    }

    /**
     * Activa o desactiva la herramienta de medir: cada par de clics sobre el piso mide la
     * distancia entre ellos. Los puntos se pegan a las paredes y a las esquinas de los muebles.
     */
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

    private List<Furniture> selectedFurniture() {
        List<Furniture> list = new ArrayList<>();
        for (Object o : selection) if (o instanceof Furniture f) list.add(f);
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
            case PLAN -> ViewCamera.Mode.PLAN;
            case WALK -> ViewCamera.Mode.WALK;
            default -> ViewCamera.Mode.THREE_D;
        };
        walkKeys.clear();
        cam.update();
        redraw();
    }

    public boolean isNight() {
        return night;
    }

    /** Modo noche: sin sol, poca luz ambiental y fondo oscuro (solo se ve; no cambia el proyecto). */
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

    /** Encuadra la habitación completa (incluye el grosor de los muros). */
    public void frameRoom() {
        if (cam.mode == ViewCamera.Mode.WALK) {
            startWalk();
            return;
        }
        RoomDims r = design.room();
        double t = r.wallThickness();
        cam.frame(-t, 0, -t, r.width() + t, r.height(), r.length() + t);
        redraw();
    }

    /** Vuelve a construir los muebles (tras recargar el catálogo). */
    public void refreshFurniture() {
        furniture.invalidate();
        furniture.sync(design);
        redraw();
    }

    /** Acerca (factor < 1) o aleja (factor > 1) la cámara, como la rueda del ratón. */
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
        WallSide selectedWall = selection.size() == 1 && selection.get(0) instanceof Surface s ? s.wallSide() : null;
        room.updateVisibility(cam.eye(), cam.mode == ViewCamera.Mode.PLAN, selectedWall);
        overlay.draw(cam, design, lights, selection, showDimensions && cam.mode != ViewCamera.Mode.WALK);
        overlay.drawGuides(guides);
        if (banding) overlay.drawBand(pressX, pressY, bandX, bandY);
        if (measuring && measureA != null) overlay.drawMeasure(measureA, measureB != null ? measureB : measureHover);
    }

    /**
     * Imagen de la vista actual (escena 3D + cotas), {@code scale} veces más grande
     * que la pantalla. Sin el contorno de selección.
     */
    public WritableImage snapshot(double scale) {
        List<Object> previous = selection;
        boolean wasMeasuring = measuring;
        selection = List.of();
        measuring = false;
        render();
        double w = root.getWidth(), h = root.getHeight();
        int width = (int) Math.round(w * scale), height = (int) Math.round(h * scale);
        try {
            // La escena 3D se dibuja directamente al tamaño final. (Antes se escalaba la imagen
            // de la pantalla y JavaFX agrandaba también la distancia a las luces: las paredes
            // salían más oscuras que en pantalla.)
            subScene.setWidth(width);
            subScene.setHeight(height);
            cam.setViewport(width, height);
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            WritableImage scene3d = subScene.snapshot(params, null);
            cam.setViewport(w, h);

            // Fondo + escena 3D + cotas (dibujadas con la cámara del tamaño de la pantalla, escaladas).
            Canvas canvas = new Canvas(width, height);
            GraphicsContext g = canvas.getGraphicsContext2D();
            g.setFill(night ? NIGHT : BACKGROUND);
            g.fillRect(0, 0, width, height);
            g.drawImage(scene3d, 0, 0);
            g.save();
            g.scale(scale, scale);
            overlay.drawOn(g, cam, design, lights, List.of(), showDimensions && cam.mode != ViewCamera.Mode.WALK);
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

    /** Imagen de la planta completa con cotas (para el informe), sin cambiar lo que ve el usuario. */
    public WritableImage planImage(double scale) {
        ViewCamera.State state = cam.state();
        boolean dims = showDimensions;
        cam.mode = ViewCamera.Mode.PLAN;
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

    /** Imagen 3D: la vista actual si se está en 3D; si no, la vista 3D encuadrada. */
    public WritableImage viewImage(double scale) {
        if (cam.mode != ViewCamera.Mode.PLAN) return snapshot(scale); // vista 3D o recorrido: lo que se ve
        ViewCamera.State state = cam.state();
        cam.mode = ViewCamera.Mode.THREE_D;
        frameRoom();
        try {
            return snapshot(scale);
        } finally {
            cam.restore(state);
            redraw();
        }
    }

    // ---------------------------------------------------------------- Recorrido

    /** Altura de los ojos: 1,60 m, o menos si el techo es bajo. */
    private double eyeHeight() {
        return Math.min(1.6, design.room().height() - 0.25);
    }

    /** Empieza el recorrido en la primera puerta (o en el centro si no hay puertas). */
    private void startWalk() {
        for (Opening o : design.openings()) {
            if (o.type == OpeningType.DOOR) {
                walkFromDoor(o);
                return;
            }
        }
        RoomDims r = design.room();
        cam.walkTo(r.width() / 2, eyeHeight(), r.length() - 0.5, 0);
        cam.setWalkEye(walkable(cam.walkEye(), cam.walkEye()));
        redraw();
    }

    /** Recorrido parado en una puerta, mirando hacia dentro de la habitación. */
    public void walkFromDoor(Opening door) {
        cam.mode = ViewCamera.Mode.WALK;
        WallFrame wall = WallFrame.of(door.wall, design.room());
        Point3D n = wall.interiorNormal();
        Point3D at = wall.point(door.offset + door.width / 2, 0, -0.6); // 60 cm dentro de la habitación
        cam.walkTo(at.getX(), eyeHeight(), at.getZ(), Math.atan2(-n.getX(), -n.getZ()));
        redraw();
    }

    /**
     * Hasta dónde se puede caminar de {@code from} hacia {@code to} sin atravesar paredes ni
     * muebles. Si choca, intenta deslizarse solo en X o solo en Z (como al arrastrar muebles).
     */
    private Point3D walkable(Point3D from, Point3D to) {
        for (Point3D p : new Point3D[] {to, new Point3D(to.getX(), to.getY(), from.getZ()), new Point3D(from.getX(), to.getY(), to.getZ())}) {
            if (free(p.getX(), p.getZ())) return p;
        }
        return free(from.getX(), from.getZ()) ? from : to; // si ya estaba "dentro" de algo, que pueda salir
    }

    private boolean free(double x, double z) {
        RoomDims r = design.room();
        if (x < BODY || z < BODY || x > r.width() - BODY || z > r.length() - BODY) return false;
        Footprint body = new Footprint(x, z, BODY, BODY, 0);
        for (Furniture f : design.furniture()) if (Footprint.of(f).overlaps(body)) return false;
        return true;
    }

    private static boolean isWalkKey(KeyCode k) {
        return switch (k) {
            case W, A, S, D, UP, DOWN, LEFT, RIGHT -> true;
            default -> false;
        };
    }

    /** W/S o ↑/↓ avanzan, A/D se mueven de costado, ←/→ giran; con Shift se corre. */
    private void installWalkKeys() {
        root.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (cam.mode != ViewCamera.Mode.WALK || !isWalkKey(e.getCode()) || e.isShortcutDown() || e.isAltDown()) return;
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

    // ---------------------------------------------------------------- Vistas guardadas

    /** La vista actual con un nombre, para guardarla en el proyecto. */
    public CameraView currentView(String name) {
        double[] v = cam.values();
        CameraView.Kind kind = switch (cam.mode) {
            case PLAN -> CameraView.Kind.PLAN;
            case WALK -> CameraView.Kind.WALK;
            default -> CameraView.Kind.ORBIT;
        };
        return new CameraView(name, kind, v[0], v[1], v[2], v[3], v[4], v[5]);
    }

    /** Lleva la cámara a una vista guardada (también cambia de modo si hace falta). */
    public void showView(CameraView v) {
        ViewCamera.Mode m = switch (v.kind()) {
            case PLAN -> ViewCamera.Mode.PLAN;
            case WALK -> ViewCamera.Mode.WALK;
            default -> ViewCamera.Mode.THREE_D;
        };
        walkKeys.clear();
        cam.apply(m, v.x(), v.y(), v.z(), v.yaw(), v.pitch(), v.distance());
        redraw();
    }

    // ---------------------------------------------------------------- Medir

    private static final double SNAP_WALL = 0.06, SNAP_CORNER = 0.1;

    /** Punto del piso bajo el ratón, pegado a una esquina de mueble o a una pared si está cerca. */
    private Point3D measurePoint(double sx, double sy) {
        Point3D p = cam.floorPoint(sx, sy);
        if (p == null) return null;
        RoomDims r = design.room();
        double x = p.getX(), z = p.getZ();
        double best = SNAP_CORNER;
        double[] corner = null;
        for (Furniture f : design.furniture()) {
            for (double[] c : Footprint.of(f).corners()) {
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
        return "Distancia: " + Units.meters(a.distance(b)) + "   (X: " + Units.meters(Math.abs(b.getX() - a.getX()))
                + " · Z: " + Units.meters(Math.abs(b.getZ() - a.getZ())) + ")";
    }

    // ---------------------------------------------------------------- Ratón

    private void installMouse() {
        subScene.setOnMousePressed(e -> {
            root.requestFocus(); // quita el foco de los campos de texto (y confirma lo escrito)
            pressX = lastX = e.getX();
            pressY = lastY = e.getY();
            pressEntity = pick(e.getPickResult());
            dragItem = null;
            dragMoved = false;
            bandArmed = banding = false;
            if (measuring) return; // al medir, el ratón no selecciona ni arrastra muebles
            if (e.getButton() != MouseButton.PRIMARY) return;
            if (e.isShiftDown() || e.isShortcutDown()) {
                // Clic: agrega o quita de la selección. Arrastrar: rectángulo de selección.
                bandArmed = true;
                bandX = e.getX();
                bandY = e.getY();
                return;
            }
            if (pressEntity instanceof Furniture f) {
                // Diferencia entre el punto del piso clicado y el centro del mueble, para que
                // no "salte" al centro del cursor al empezar a arrastrar.
                Point3D p = cam.floorPoint(e.getX(), e.getY());
                if (p == null) return;
                // Si el mueble es parte de una selección múltiple, se arrastra todo el grupo.
                List<Furniture> group = selectedFurniture();
                if (group.size() < 2 || !group.contains(f)) {
                    group = List.of(f);
                    onSelect.accept(f, false);
                }
                Furniture locked = design.firstLocked(group);
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
                List<Snapper.Guide> g = design.drag(dragGroup, dragItem, p.getX() + dragDx, p.getZ() + dragDz, magnet && !e.isAltDown());
                guides = g == null ? List.of() : g;
                if (dragItem.x != beforeX || dragItem.z != beforeZ) {
                    dragMoved = true;
                    onDrag.run();
                }
                redraw();
                return;
            }
            boolean plan = cam.mode == ViewCamera.Mode.PLAN;
            if (e.getButton() == MouseButton.PRIMARY && !plan) cam.rotate(dx, dy);
            else cam.pan(dx, dy);
            redraw();
        });

        subScene.setOnMouseReleased(e -> {
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
            if (cam.mode == ViewCamera.Mode.WALK) {
                // En el recorrido la rueda avanza o retrocede 30 cm.
                Point3D step = cam.walkForward().multiply(e.getDeltaY() > 0 ? 0.3 : -0.3);
                cam.setWalkEye(walkable(cam.walkEye(), cam.walkEye().add(step)));
                redraw();
                return;
            }
            zoom(e.getDeltaY() > 0 ? 0.9 : 1 / 0.9);
        });

        // Cambiar el cursor al pasar sobre algo seleccionable.
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
            Object entity = pick(e.getPickResult());
            // Las paredes y el piso también se seleccionan, pero la mano solo aparece sobre objetos.
            boolean object = entity != null && !(entity instanceof Surface);
            boolean movable = entity instanceof Furniture f && !f.locked;
            subScene.setCursor(movable ? Cursor.OPEN_HAND : object ? Cursor.HAND : Cursor.DEFAULT);
        });
    }

    /** Muebles cuyo centro (a media altura) se ve dentro del rectángulo de selección. */
    private List<Furniture> furnitureInBand() {
        double x0 = Math.min(pressX, bandX), x1 = Math.max(pressX, bandX);
        double y0 = Math.min(pressY, bandY), y1 = Math.max(pressY, bandY);
        List<Furniture> inside = new ArrayList<>();
        for (Furniture f : design.furniture()) {
            javafx.geometry.Point2D s = cam.project(new Point3D(f.x, f.height / 2, f.z));
            if (s != null && s.getX() >= x0 && s.getX() <= x1 && s.getY() >= y0 && s.getY() <= y1) inside.add(f);
        }
        return inside;
    }

    /** Sube por los padres del nodo clicado hasta encontrar a qué entidad pertenece. */
    private static Object pick(PickResult result) {
        for (Node n = result == null ? null : result.getIntersectedNode(); n != null; n = n.getParent()) {
            Object entity = n.getProperties().get(ENTITY);
            if (entity != null) return entity;
        }
        return null;
    }
}
