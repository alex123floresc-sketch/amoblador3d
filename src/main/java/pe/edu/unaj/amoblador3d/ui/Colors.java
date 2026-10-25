package pe.edu.unaj.amoblador3d.ui;

import java.util.Locale;
import javafx.scene.paint.Color;

final class Colors {
    private Colors() {}

    /** Color de JavaFX -> "#rrggbb" (el formato que se guarda en los proyectos). */
    static String toHex(Color c) {
        return String.format(Locale.ROOT, "#%02x%02x%02x",
                Math.round(c.getRed() * 255), Math.round(c.getGreen() * 255), Math.round(c.getBlue() * 255));
    }
}
