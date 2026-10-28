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
import pe.edu.unaj.amoblador3d.catalog.Catalog;
import pe.edu.unaj.amoblador3d.design.Design;
import pe.edu.unaj.amoblador3d.model.Home;
import pe.edu.unaj.amoblador3d.model.RoomDims;
import pe.edu.unaj.amoblador3d.util.Units;

/**
 * Vista de la casa completa: todos los ambientes juntos, en 3D o en planta, cada uno con sus
 * paredes, muebles y luces (las luces de un ambiente solo iluminan ese ambiente).
 *
 * <ul>
 *   <li>Clic en un ambiente: lo elige. Doble clic: lo abre para decorarlo.</li>
 *   <li>Arrastrar el ambiente elegido (en planta, cualquiera): lo mueve en el plano de la casa.
 *       Se pega muro con muro a los demás y nunca se encima con otro.</li>
 *   <li>3D: botón izquierdo gira, derecho mueve, rueda acerca. Planta: arrastrar el vacío mueve.</li>
 * </ul>
 */
public final class HomeView {
    private static final String SPACE = "amoblador.space";
    /** A esta distancia (m) un muro "se pega" al de otro ambiente. */
    private static final double SNAP = 0.35;
    private static final double CLICK_TOLERANCE = 5;
    private static final LinearGradient BACKGROUND = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#46505c")), new Stop(0.55, Color.web("#2b3036")), new Stop(1, Color.web("#1d2125")));
    private static final Color SELECT = Color.web("#4f9cf9"), GUIDE = Color.web("#ff4fd8");
    private static final Font NAME_FONT = Font.font("System", FontWeight.BOLD, 13), INFO_FONT = Font.font("System", 11);

    private final Catalog catalog;
    private final ModelLibrary models;
    private final StackPane root = new StackPane();
    private final SubScene subScene;
    private final ViewCamera cam = new ViewCamera();
    private final Group spacesGroup = new Group();
    private final Box ground = new Box(1, 0.06, 1);
    private final Canvas canvas = new Canvas();

    private Home home = new Home();
    private final List<SpaceNode> nodes = new ArrayList<>();
    private Home.Space selected;

    private Consumer<Home.Space> onSelect = s -> {};
    private Consumer<Home.Space> onOpen = s -> {};
    private Runnable onMoved = () -> {};
    private Consumer<String> onStatus = s -> {};

    // Ratón
    private double pressX, pressY, lastX, lastY;
    private Home.Space dragging;
    private double dragDx, dragDz;
    private boolean moved;
    private List<double[]> guides = List.of();

    /** Los nodos 3D de un ambiente. */
    private record SpaceNode(Home.Space space, Group group, Translate position, RoomView room) {}

    public HomeView(Catalog catalog, ModelLibrary models) {
        this.catalog = catalog;
        this.models = models;
        ground.setMaterial(Materials.PLINTH);
        ground.setMouseTransparent(true);
        // La base de la casa no recibe las luces de los ambientes: tiene su propia luz.
        javafx.scene.AmbientLight groundLight = new javafx.scene.AmbientLight(Color.gray(0.85));
        groundLight.getScope().add(ground);
        Group world = new Group(ground, groundLight, spacesGroup);
        world.getTransforms().add(new Rotate(180, Rotate.X_AXIS)); // Y hacia arriba, como en cada ambiente
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

    public void setOnSelect(Consumer<Home.Space> c) {
        onSelect = c;
    }

    /** Doble clic sobre un ambiente: abrirlo para decorarlo. */
    public void setOnOpen(Consumer<Home.Space> c) {
        onOpen = c;
    }

    /** Al soltar un ambiente que se movió (para registrarlo en deshacer). */
    public void setOnMoved(Runnable r) {
        onMoved = r;
    }

    /** Textos para la barra de estado mientras se arrastra. */
    public void setOnStatus(Consumer<String> c) {
        onStatus = c;
    }

    public boolean isPlan() {
        return cam.mode == ViewCamera.Mode.PLAN;
    }

    public void setPlan(boolean plan) {
        cam.mode = plan ? ViewCamera.Mode.PLAN : ViewCamera.Mode.THREE_D;
        cam.update();
        frame();
    }

    public void select(Home.Space s) {
        selected = s;
        render();
    }

    /** Vuelve a construir todos los ambientes. */
    public void show(Home h) {
        home = h;
        spacesGroup.getChildren().clear();
        nodes.clear();
        if (selected != null && !h.spaces.contains(selected)) selected = null;
        for (Home.Space s : h.spaces) {
            Design d = new Design(catalog);
            d.load(s.project);
            RoomView room = new RoomView();
            room.rebuild(d);
            FurnitureView furniture = new FurnitureView(models, msg -> { });
            furniture.setOnModelLoaded(this::render);
            furniture.sync(d);
            LightView lights = new LightView();
            lights.rebuild(d);
            Translate position = new Translate(s.x, 0, s.z);
            Group g = new Group(room.group, furniture.group, lights.group);
            g.getTransforms().add(position);
            g.getProperties().put(SPACE, s);
            // Las luces de cada ambiente solo lo iluminan a él (JavaFX ilumina con pocas luces a la vez).
            for (Node n : lights.group.getChildren()) {
                if (n instanceof LightBase light) light.getScope().add(g);
                // Los símbolos de bombillas y focos (en el aire) estorban al ver la casa; la mancha del sol se queda.
                else if (n.getBoundsInParent().getMinY() > 0.05) n.setVisible(false);
            }
            // La base de cada habitación se reemplaza por una sola base para toda la casa.
            hidePlinth(room.group);
            spacesGroup.getChildren().add(g);
            nodes.add(new SpaceNode(s, g, position, room));
        }
        updateGround();
        render();
    }

    private static void hidePlinth(Group g) {
        for (Node n : g.getChildren()) {
            if (n instanceof Box b && b.getMaterial() == Materials.PLINTH) b.setVisible(false);
        }
    }

    /** Actualiza la posición de un ambiente sin reconstruirlo. */
    public void moved(Home.Space s) {
        for (SpaceNode n : nodes) {
            if (n.space() == s) {
                n.position().setX(s.x);
                n.position().setZ(s.z);
            }
        }
        updateGround();
        render();
    }

    /** Encuadra la casa completa. */
    public void frame() {
        double[] b = bounds();
        double h = 2.5;
        for (Home.Space s : home.spaces) h = Math.max(h, s.project.room.height());
        cam.frame(b[0], 0, b[1], b[2], h, b[3]);
        render();
    }

    public void zoom(double factor) {
        cam.zoom(factor);
        render();
    }

    /** {minX, minZ, maxX, maxZ} de toda la casa, con los muros. */
    private double[] bounds() {
        if (home.spaces.isEmpty()) return new double[] {0, 0, 4, 4};
        double[] b = {Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        for (Home.Space s : home.spaces) {
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

    /** Imagen de la vista, {@code scale} veces el tamaño de la pantalla (para la miniatura del proyecto). */
    public WritableImage snapshot(double scale) {
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.web("#2b3036"));
        params.setTransform(javafx.scene.transform.Transform.scale(scale, scale));
        return root.snapshot(params, null);
    }

    // ---------------------------------------------------------------- Dibujo

    private void render() {
        boolean plan = cam.mode == ViewCamera.Mode.PLAN;
        Point3D eye = cam.eye();
        for (SpaceNode n : nodes) {
            // Cada habitación oculta las paredes que tapan su interior, vista desde la cámara.
            n.room().updateVisibility(eye.subtract(n.space().x, 0, n.space().z), plan, false, null);
        }
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        g.setLineDashes();
        for (Home.Space s : home.spaces) {
            RoomDims r = s.project.room;
            if (s == selected) {
                double[] o = s.outer();
                g.setStroke(SELECT);
                g.setLineWidth(3);
                polygon(g, new double[][] {{o[0], o[1]}, {o[2], o[1]}, {o[2], o[3]}, {o[0], o[3]}});
            }
            Point2D c = cam.project(new Point3D(s.x + r.width() / 2, 0.05, s.z + r.length() / 2));
            if (c == null) continue;
            String info = Units.number(Units.round(r.width() * r.length())) + " m²  ·  " + Units.number(r.width()) + " × "
                    + Units.number(r.length()) + " m";
            label(g, c, s.name, plan || s == selected ? info : null, s == selected);
        }
        g.setStroke(GUIDE);
        g.setLineWidth(2.5);
        g.setLineDashes(10, 6);
        for (double[] line : guides) {
            Point2D a = cam.project(new Point3D(line[0], 0.03, line[1])), b = cam.project(new Point3D(line[2], 0.03, line[3]));
            if (a != null && b != null) g.strokeLine(a.getX(), a.getY(), b.getX(), b.getY());
        }
        g.setLineDashes();
    }

    private void polygon(GraphicsContext g, double[][] pts) {
        for (int i = 0; i < pts.length; i++) {
            double[] a = pts[i], b = pts[(i + 1) % pts.length];
            Point2D pa = cam.project(new Point3D(a[0], 0.03, a[1])), pb = cam.project(new Point3D(b[0], 0.03, b[1]));
            if (pa != null && pb != null) g.strokeLine(pa.getX(), pa.getY(), pb.getX(), pb.getY());
        }
    }

    /** Cartel con el nombre y (si {@code info} no es null) el área del ambiente. */
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

    // ---------------------------------------------------------------- Ratón

    private Home.Space pick(javafx.scene.input.PickResult r) {
        for (Node n = r == null ? null : r.getIntersectedNode(); n != null; n = n.getParent()) {
            Object s = n.getProperties().get(SPACE);
            if (s instanceof Home.Space space) return space;
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
            Home.Space s = pick(e.getPickResult());
            // En 3D solo se arrastra el ambiente ya elegido (así arrastrar sobre la casa la gira).
            if (s != null && (isPlan() || s == selected)) {
                Point3D p = cam.floorPoint(e.getX(), e.getY());
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
                Point3D p = cam.floorPoint(e.getX(), e.getY());
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
                Home.Space s = dragging;
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
                Home.Space s = pick(e.getPickResult());
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
            Home.Space s = pick(e.getPickResult());
            subScene.setCursor(s == null ? Cursor.DEFAULT : (isPlan() || s == selected) ? Cursor.OPEN_HAND : Cursor.HAND);
        });
    }

    /**
     * Lleva el ambiente lo más cerca posible de (x, z): con imán, sus muros se pegan a los de los
     * demás (y se alinean sus bordes). Si así se encimaría con otro, prueba moverlo solo en X o en Z.
     */
    private void drag(Home.Space s, double x, double z, boolean magnet) {
        double t = s.project.room.wallThickness(), w = s.project.room.width() + 2 * t, l = s.project.room.length() + 2 * t;
        List<double[]> lines = new ArrayList<>();
        if (magnet) {
            double minX = x - t, minZ = z - t;
            double bestX = SNAP, bestZ = SNAP, snapX = 0, snapZ = 0;
            double[] gx = null, gz = null;
            for (Home.Space o : home.spaces) {
                if (o == s) continue;
                double[] b = o.outer();
                // Bordes en X: pegado por fuera (muro con muro) o alineado.
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
            double nx = Units.round(p[0]), nz = Units.round(p[1]);
            if (nx == s.x && nz == s.z) break;
            if (home.collision(s, nx, nz) != null) continue;
            s.x = nx;
            s.z = nz;
            moved = true;
            moved(s);
            break;
        }
        guides = lines;
        Home.Space other = home.collision(s, Units.round(x), Units.round(z));
        onStatus.accept(s.name + ": (" + Units.number(s.x) + "; " + Units.number(s.z) + ") m"
                + (other != null ? "   · se encimaría con " + other.name : magnet ? "   (Alt: sin imán)" : ""));
        render();
    }
}
