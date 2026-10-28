package pe.edu.unaj.amoblador3d.design;

import java.util.List;

/**
 * Un campo editable del panel de propiedades. El panel dibuja el formulario a partir
 * de estos campos, así el mismo panel sirve para ventanas, puertas, luces y muebles.
 */
public record Field(String key, String label, Type type, double min, double max, double step, String unit, List<Option> options,
                    boolean slider) {

    /**
     * GALLERY: una lista que se muestra como miniaturas (materiales); en COLOR, las opciones son colores
     * sugeridos; SECTION: solo un título que agrupa los campos que le siguen.
     */
    public enum Type { NUMBER, SELECT, GALLERY, COLOR, CHECKBOX, SECTION }

    public Field(String key, String label, Type type, double min, double max, double step, String unit, List<Option> options) {
        this(key, label, type, min, max, step, unit, options, false);
    }

    public record Option(String value, String label) {}

    public static Field number(String key, String label, double min, double max, double step, String unit) {
        return new Field(key, label, Type.NUMBER, min, max, step, unit, List.of());
    }

    /** Número con un deslizador debajo: el cambio se ve en vivo mientras se arrastra. */
    public static Field slider(String key, String label, double min, double max, double step, String unit) {
        return new Field(key, label, Type.NUMBER, min, max, step, unit, List.of(), true);
    }

    /** Medida en metros, con deslizador. */
    public static Field meters(String key, String label, double min, double max) {
        return slider(key, label, min, max, 0.01, "m");
    }

    /** Título de un grupo de campos ("Medidas", "Colores"...). */
    public static Field section(String title) {
        return new Field("section:" + title, title, Type.SECTION, 0, 0, 0, "", List.of());
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
