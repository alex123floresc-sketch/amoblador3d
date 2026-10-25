package pe.edu.unaj.amoblador3d.scene;

import java.util.function.Consumer;
import javafx.animation.AnimationTimer;
import javafx.geometry.Point3D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SnapshotParameters;
import javafx.scene.SubScene;
import javafx.scene.image.WritableImage;
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
import javafx.scene.transform.Scale;
import pe.edu.unaj.amoblador3d.design.Design;
import pe.edu.unaj.amoblador3d.model.Furniture;
import pe.edu.unaj.amoblador3d.model.RoomDims;
import pe.edu.unaj.amoblador3d.model.Surface;

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

    public enum Mode { THREE_D, PLAN }

    private final Design design;
    private final StackPane root = new StackPane();
    private final SubScene subScene;
    private final ViewCamera cam = new ViewCamera();
    private final RoomView room = new RoomView();
    private final LightView lights = new LightView();
    private final FurnitureView furniture;
    private final Overlay overlay = new Overlay();

    private Object selected;
    private boolean showDimensions = true;
    private boolean dirty = true;

    private Consumer<Object> onSelect = e -> {};
    private Runnable onDrag = () -> {};
    private Runnable onDragEnd = () -> {};

    // Estado del ratón
    private double pressX, pressY, lastX, lastY;
    private Object pressEntity;
    private Furniture dragItem;
    private double dragDx, dragDz;
    private boolean dragMoved;

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
        new AnimationTimer() {
            @Override
            public void handle(long now) {
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

    public void setOnSelect(Consumer<Object> c) {
        onSelect = c;
    }

    public void setOnDrag(Runnable r) {
        onDrag = r;
    }

    public void setOnDragEnd(Runnable r) {
        onDragEnd = r;
    }

    public void setSelected(Object entity) {
        selected = entity;
        redraw();
    }

    public Mode mode() {
        return cam.mode == ViewCamera.Mode.PLAN ? Mode.PLAN : Mode.THREE_D;
    }

    public void setMode(Mode mode) {
        cam.mode = mode == Mode.PLAN ? ViewCamera.Mode.PLAN : ViewCamera.Mode.THREE_D;
        cam.update();
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
        room.updateVisibility(cam.eye(), cam.mode == ViewCamera.Mode.PLAN);
        overlay.draw(cam, design, lights, selected, showDimensions);
    }

    /**
     * Imagen de la vista actual (escena 3D + cotas), {@code scale} veces más grande
     * que la pantalla. Sin el contorno de selección.
     */
    public WritableImage snapshot(double scale) {
        Object previous = selected;
        selected = null;
        render();
        SnapshotParameters params = new SnapshotParameters();
        params.setTransform(new Scale(scale, scale));
        params.setFill(Color.web("#2b3036"));
        WritableImage image = root.snapshot(params, null);
        selected = previous;
        redraw();
        return image;
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
            if (e.getButton() == MouseButton.PRIMARY && pressEntity instanceof Furniture f) {
                // Diferencia entre el punto del piso clicado y el centro del mueble, para que
                // no "salte" al centro del cursor al empezar a arrastrar.
                Point3D p = cam.floorPoint(e.getX(), e.getY());
                if (p != null) {
                    dragItem = f;
                    dragDx = f.x - p.getX();
                    dragDz = f.z - p.getZ();
                    onSelect.accept(f);
                    subScene.setCursor(Cursor.CLOSED_HAND);
                }
            }
        });

        subScene.setOnMouseDragged(e -> {
            double dx = e.getX() - lastX, dy = e.getY() - lastY;
            lastX = e.getX();
            lastY = e.getY();
            if (dragItem != null) {
                Point3D p = cam.floorPoint(e.getX(), e.getY());
                if (p != null && design.moveFurniture(dragItem, p.getX() + dragDx, p.getZ() + dragDz)) {
                    dragMoved = true;
                    onDrag.run();
                }
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
                subScene.setCursor(Cursor.OPEN_HAND);
                if (dragMoved) onDragEnd.run();
                return;
            }
            if (e.getButton() == MouseButton.PRIMARY && Math.hypot(e.getX() - pressX, e.getY() - pressY) < CLICK_TOLERANCE) {
                onSelect.accept(pressEntity);
            }
        });

        subScene.setOnScroll(e -> {
            if (e.getDeltaY() == 0) return;
            cam.zoom(e.getDeltaY() > 0 ? 0.9 : 1 / 0.9);
            redraw();
        });

        // Cambiar el cursor al pasar sobre algo seleccionable.
        subScene.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            Object entity = pick(e.getPickResult());
            // Las paredes y el piso también se seleccionan, pero la mano solo aparece sobre objetos.
            boolean object = entity != null && !(entity instanceof Surface);
            subScene.setCursor(entity instanceof Furniture ? Cursor.OPEN_HAND : object ? Cursor.HAND : Cursor.DEFAULT);
        });
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
