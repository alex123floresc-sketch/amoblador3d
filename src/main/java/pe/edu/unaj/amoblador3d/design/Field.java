package pe.edu.unaj.amoblador3d.design;

import java.util.List;

/**
 * Un campo editable del panel de propiedades. El panel dibuja el formulario a partir
 * de estos campos, así el mismo panel sirve para ventanas, puertas, luces y muebles.
 */
public record Field(String key, String label, Type type, double min, double max, double step, String unit, List<Option> options) {

    /** GALLERY: una lista que se muestra como miniaturas (materiales); en COLOR, las opciones son colores sugeridos. */
    public enum Type { NUMBER, SELECT, GALLERY, COLOR, CHECKBOX }

    public record Option(String value, String label) {}

    public static Field number(String key, String label, double min, double max, double step, String unit) {
        return new Field(key, label, Type.NUMBER, min, max, step, unit, List.of());
    }

    public static Field meters(String key, String label, double min, double max) {
        return number(key, label, min, max, 0.01, "m");
    }

    public static Field select(String key, String label, List<Option> options) {
        return new Field(key, label, Type.SELECT, 0, 0, 0, "", options);
    }

    public static Field gallery(String key, String label, List<Option> options) {
        return new Field(key, label, Type.GALLERY, 0, 0, 0, "", options);
    }

    public static Field color(String key, String label) {
        return color(key, label, List.of());
    }

    /** @param presets colores sugeridos: valor "#rrggbb" y nombre */
    public static Field color(String key, String label, List<Option> presets) {
        return new Field(key, label, Type.COLOR, 0, 0, 0, "", presets);
    }

    public static Field checkbox(String key, String label) {
        return new Field(key, label, Type.CHECKBOX, 0, 0, 0, "", List.of());
    }
}
