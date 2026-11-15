package pe.edu.unaj.amoblador3d.scene;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalogEntry;
import pe.edu.unaj.amoblador3d.design.FSHFDesign;
import pe.edu.unaj.amoblador3d.model.FSHFFurniture;

/**
 * Nodo 3D de cada mueble del ambiente; se reconstruye cuando cambian sus datos o termina
 * de cargar su modelo.
 */
final class FSHFFurnitureView {
    final Group group = new Group();
    private final FSHFModelLibrary models;
    private final Consumer<String> onWarning;
    private final Map<FSHFFurniture, Holder> nodes = new IdentityHashMap<>();
    private final Set<String> warnedUrls = new HashSet<>();
    private Runnable onModelLoaded = () -> {};

    // Sombra de contacto suave (mancha oscura con bordes difuminados) bajo cada mueble.
    private static final double SHADOW_T = 0.004;
    private static final PhongMaterial SHADOW_MAT = createShadowMaterial();

    private static PhongMaterial createShadowMaterial() {
        int n = 96;
        WritableImage img = new WritableImage(n, n);
        PixelWriter pw = img.getPixelWriter();
        double c = (n - 1) / 2.0;
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                double dx = (x - c) / c, dz = (y - c) / c;
                double dist = Math.min(1, Math.sqrt(dx * dx + dz * dz));
                double a = Math.pow(1 - dist, 1.7) * 0.42;
                pw.setColor(x, y, Color.rgb(18, 18, 20, a));
            }
        }
        PhongMaterial m = new PhongMaterial(Color.rgb(18, 18, 20));
        m.setDiffuseMap(img);
        return m;
    }

    private static final class Holder {
        final Group node = new Group();
        final Group content = new Group();
        final Box shadow = new Box(1, SHADOW_T, 1);
        final Translate position = new Translate();
        final Rotate rotation = new Rotate(0, Rotate.Y_AXIS);
        String shape;

        Holder() {
            node.getTransforms().addAll(position, rotation);
            shadow.setMaterial(SHADOW_MAT);
            shadow.setMouseTransparent(true);
            shadow.setTranslateY(SHADOW_T / 2 + 0.002);
            node.getChildren().addAll(shadow, content);
        }
    }

    FSHFFurnitureView(FSHFModelLibrary models, Consumer<String> onWarning) {
        this.models = models;
        this.onWarning = onWarning;
    }

    void setOnModelLoaded(Runnable r) {
        onModelLoaded = r;
    }

    void sync(FSHFDesign design) {
        List<FSHFFurniture> items = design.furniture();
        Set<FSHFFurniture> alive = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        alive.addAll(items);
        nodes.entrySet().removeIf(e -> {
            if (alive.contains(e.getKey())) return false;
            group.getChildren().remove(e.getValue().node);
            return true;
        });
        for (FSHFFurniture f : items) {
            Holder h = nodes.get(f);
            if (h == null) {
                h = new Holder();
                FSHFSceneView.tag(h.node, f);
                nodes.put(f, h);
                group.getChildren().add(h.node);
            }
            String shape = f.appearance();
            if (!shape.equals(h.shape)) {
                h.shape = shape;
                build(design.catalog().get(f.type), f, h);
            }
            h.position.setX(f.x);
            h.position.setZ(f.z);
            h.rotation.setAngle(f.rotation);
            h.shadow.setWidth(Math.max(f.width * 1.08, 0.2));
            h.shadow.setDepth(Math.max(f.depth * 1.08, 0.2));
        }
    }

    void invalidate() {
        for (Holder h : nodes.values()) h.shape = null;
    }

    private void build(FSHFCatalogEntry entry, FSHFFurniture f, Holder h) {
        switch (entry.kind()) {
            case PARAMETRIC -> {
                Group g = FSHFParametricBuilder.build(f);
                h.content.getChildren().setAll(g != null ? g : FSHFParametricBuilder.missing(f));
            }
            case CUSTOM -> h.content.getChildren().setAll(FSHFFurnitureShapes.fit(entry.design(), f));
            case MISSING -> h.content.getChildren().setAll(FSHFParametricBuilder.missing(f));
            case MODEL -> {
                h.content.getChildren().setAll(FSHFParametricBuilder.placeholder(f));
                String shape = h.shape;
                double w = f.width, d = f.depth, ht = f.height;
                String tint = f.color;
                pe.edu.unaj.amoblador3d.model.FSHFFinish finish = f.finish;
                models.load(entry.model()).whenComplete((data, error) -> Platform.runLater(() -> {
                    if (!shape.equals(h.shape)) return;
                    if (error != null) {
                        if (warnedUrls.add(entry.model().toString())) {
                            Throwable cause = error.getCause() != null ? error.getCause() : error;
                            onWarning.accept("No se pudo cargar el modelo \"" + entry.name() + "\": " + cause.getMessage());
                        }
                        return;
                    }
                    h.content.getChildren().setAll(models.fit(data, w, d, ht, entry.modelRotation(), tint, finish));
                    onModelLoaded.run();
                }));
            }
        }
    }
}
