package pe.edu.unaj.amoblador3d.scene;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import pe.edu.unaj.amoblador3d.catalog.CatalogEntry;
import pe.edu.unaj.amoblador3d.design.Design;
import pe.edu.unaj.amoblador3d.model.Furniture;

/**
 * Nodos 3D de los muebles. Al cambiar algo solo se reconstruye lo necesario: si cambió la
 * posición o el giro, se mueve el nodo; si cambiaron medidas, color o tipo, se vuelve a crear.
 */
final class FurnitureView {
    final Group group = new Group();
    private final ModelLibrary models;
    private final Consumer<String> onWarning;
    private final Map<Furniture, Holder> nodes = new IdentityHashMap<>();
    private final Set<String> warnedUrls = new HashSet<>();
    private Runnable onModelLoaded = () -> {};

    /** Nodo de un mueble con su posición y giro, y la "firma" de su forma actual. */
    private static final class Holder {
        final Group node = new Group();
        final Translate position = new Translate();
        final Rotate rotation = new Rotate(0, Rotate.Y_AXIS);
        String shape;

        Holder() {
            node.getTransforms().addAll(position, rotation);
        }
    }

    FurnitureView(ModelLibrary models, Consumer<String> onWarning) {
        this.models = models;
        this.onWarning = onWarning;
    }

    void setOnModelLoaded(Runnable r) {
        onModelLoaded = r;
    }

    void sync(Design design) {
        List<Furniture> items = design.furniture();
        Set<Furniture> alive = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        alive.addAll(items);
        nodes.entrySet().removeIf(e -> {
            if (alive.contains(e.getKey())) return false;
            group.getChildren().remove(e.getValue().node);
            return true;
        });
        for (Furniture f : items) {
            Holder h = nodes.get(f);
            if (h == null) {
                h = new Holder();
                SceneView.tag(h.node, f);
                nodes.put(f, h);
                group.getChildren().add(h.node);
            }
            String shape = f.type + "|" + f.width + "|" + f.depth + "|" + f.height + "|" + f.color;
            if (!shape.equals(h.shape)) {
                h.shape = shape;
                build(design.catalog().get(f.type), f, h);
            }
            h.position.setX(f.x);
            h.position.setZ(f.z);
            h.rotation.setAngle(f.rotation);
        }
    }

    /** Fuerza a reconstruir todos (p. ej. tras recargar el catálogo). */
    void invalidate() {
        for (Holder h : nodes.values()) h.shape = null;
    }

    private void build(CatalogEntry entry, Furniture f, Holder h) {
        switch (entry.kind()) {
            case PARAMETRIC -> {
                Group g = ParametricBuilder.build(f);
                h.node.getChildren().setAll(g != null ? g : ParametricBuilder.missing(f));
            }
            case MISSING -> h.node.getChildren().setAll(ParametricBuilder.missing(f));
            case MODEL -> {
                // Mientras el modelo carga, mostramos una caja translúcida con sus medidas.
                h.node.getChildren().setAll(ParametricBuilder.placeholder(f));
                String shape = h.shape;
                double w = f.width, d = f.depth, ht = f.height;
                models.load(entry.model()).whenComplete((data, error) -> Platform.runLater(() -> {
                    if (!shape.equals(h.shape)) return; // llegó tarde: el mueble ya cambió
                    if (error != null) {
                        if (warnedUrls.add(entry.model().toString())) {
                            Throwable cause = error.getCause() != null ? error.getCause() : error;
                            onWarning.accept("No se pudo cargar el modelo \"" + entry.name() + "\": " + cause.getMessage());
                        }
                        return;
                    }
                    h.node.getChildren().setAll(models.fit(data, w, d, ht, entry.modelRotation()));
                    onModelLoaded.run();
                }));
            }
        }
    }
}
