package pe.edu.unaj.amoblador3d.scene;

import java.util.HashMap;
import java.util.Map;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

/** Materiales compartidos (un PhongMaterial por color, así no se crean miles iguales). */
final class Materials {
    private static final Map<String, PhongMaterial> CACHE = new HashMap<>();

    static final PhongMaterial GRID = of(Color.rgb(0, 0, 0, 0.18));
    static final PhongMaterial PLINTH = of("#cfc9bf");
    static final PhongMaterial FRAME = shiny(Color.web("#fafafa"), 0.3, 20);
    static final PhongMaterial GLASS = glass();
    static final PhongMaterial DOOR = shiny(Color.web("#8b5a2b"), 0.2, 16);
    static final PhongMaterial METAL = shiny(Color.web("#c0c0c0"));
    static final PhongMaterial MISSING = of(Color.rgb(217, 83, 79, 0.55));
    static final PhongMaterial PLACEHOLDER = of(Color.rgb(154, 163, 173, 0.4));

    private Materials() {}

    static PhongMaterial of(String color) {
        return CACHE.computeIfAbsent(color, c -> new PhongMaterial(Color.web(c)));
    }

    static PhongMaterial of(Color color) {
        return CACHE.computeIfAbsent(color.toString(), c -> new PhongMaterial(color));
    }

    static PhongMaterial shiny(Color color) {
        return shiny(color, 0.9, 24);
    }

    static PhongMaterial shiny(Color color, double specular, double power) {
        PhongMaterial m = new PhongMaterial(color);
        m.setSpecularColor(Color.gray(specular));
        m.setSpecularPower(power);
        return m;
    }

    /** Vidrio translúcido con un reflejo marcado. */
    private static PhongMaterial glass() {
        PhongMaterial m = new PhongMaterial(Color.rgb(191, 227, 255, 0.3));
        m.setSpecularColor(Color.gray(1));
        m.setSpecularPower(96);
        return m;
    }

    /** Color un poco más oscuro (para cajones, puertas del closet...). */
    static PhongMaterial darker(String color, double factor) {
        Color c = Color.web(color);
        return of(Color.color(c.getRed() * factor, c.getGreen() * factor, c.getBlue() * factor));
    }

    /**
     * Material que "brilla" con su propio color sin importar las luces
     * (para los símbolos de bombillas, focos y sol).
     */
    static PhongMaterial glow(String color) {
        return CACHE.computeIfAbsent("glow:" + color, c -> {
            WritableImage img = new WritableImage(1, 1);
            img.getPixelWriter().setColor(0, 0, Color.web(color));
            PhongMaterial m = new PhongMaterial(Color.BLACK);
            m.setSelfIlluminationMap(img);
            return m;
        });
    }
}
