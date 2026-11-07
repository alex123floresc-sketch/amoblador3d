package pe.edu.unaj.amoblador3d.scene;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SnapshotParameters;
import javafx.scene.SubScene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalogEntry;
import pe.edu.unaj.amoblador3d.model.FSHFFurniture;

public final class FSHFThumbnails {
    public static final int WIDTH = 116, HEIGHT = 80;

    private final FSHFModelLibrary models;
    private final Map<String, Image> cache = new HashMap<>();

    public FSHFThumbnails(FSHFModelLibrary models) {
        this.models = models;
    }

    public void clear() {
        cache.clear();
    }

    public Image of(FSHFCatalogEntry e, Consumer<Image> onReady) {
        String key = e.id() + "|" + (e.design() != null ? e.design().toJson().toString().hashCode() : "");
        Image cached = cache.get(key);
        if (cached != null) return cached;
        FSHFCatalogEntry.Size s = e.defaults();
        FSHFFurniture f = new FSHFFurniture("thumb", e.id(), 0, 0, 0, s.width(), s.depth(), s.height(), e.defaultColor());
        switch (e.kind()) {
            case PARAMETRIC -> {
                Group g = FSHFParametricBuilder.build(f);
                if (g == null) return null;
                Image img = render(g, s);
                cache.put(key, img);
                return img;
            }
            case CUSTOM -> {
                Image img = render(FSHFFurnitureShapes.fit(e.design(), f), s);
                cache.put(key, img);
                return img;
            }
            case MODEL -> {
                models.load(e.model()).thenAccept(data -> Platform.runLater(() -> {
                    Image img = render(models.fit(data, s.width(), s.depth(), s.height(), e.modelRotation(), null, null), s);
                    cache.put(key, img);
                    onReady.accept(img);
                }));
                return null;
            }
            default -> {
                return null;
            }
        }
    }

    static Image render(Group furniture, FSHFCatalogEntry.Size s) {
        Group world = new Group(furniture);
        world.getTransforms().add(new Rotate(180, Rotate.X_AXIS));
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setFieldOfView(30);
        camera.setNearClip(0.01);
        camera.setFarClip(100);
        double size = Math.max(s.width(), Math.max(s.depth(), s.height() * 1.3));
        camera.getTransforms().addAll(new Translate(0, -s.height() / 2, 0), new Rotate(-35, Rotate.Y_AXIS),
                new Rotate(-24, Rotate.X_AXIS), new Translate(0, 0, -size * 2.35));
        PointLight key = new PointLight(Color.gray(0.9));
        key.getTransforms().add(new Translate(-size * 2, -size * 3, -size * 3));
        PointLight fill = new PointLight(Color.gray(0.3));
        fill.getTransforms().add(new Translate(size * 3, -size, size * 2));
        AmbientLight ambient = new AmbientLight(Color.gray(0.5));
        SubScene sub = new SubScene(new Group(world, key, fill, ambient, camera), WIDTH * 2, HEIGHT * 2, true,
                SceneAntialiasing.BALANCED);
        sub.setCamera(camera);
        sub.setFill(Color.TRANSPARENT);

        javafx.scene.Scene holder = new javafx.scene.Scene(new Group(sub), WIDTH * 2, HEIGHT * 2, true);
        holder.setFill(Color.TRANSPARENT);
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        return sub.snapshot(params, null);
    }
}
