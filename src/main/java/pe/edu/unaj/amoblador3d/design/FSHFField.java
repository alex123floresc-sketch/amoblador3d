package pe.edu.unaj.amoblador3d.design;

import java.util.List;

/**
 * Un campo editable del panel de propiedades. El panel dibuja el formulario a partir
 * de estos campos, así el mismo panel sirve para ventanas, puertas, luces y muebles.
 */
public record FSHFField(String key, String label, Type type, double min, double max, double step, String unit, List<Option> options,
                    boolean slider) {

    /**
     * GALLERY: una lista que se muestra como miniaturas (materiales); en COLOR, las opciones son colores
     * sugeridos; SECTION: solo un título que agrupa los campos que le siguen.
     */
    public enum Type { NUMBER, SELECT, GALLERY, COLOR, CHECKBOX, SECTION }

    public FSHFField(String key, String label, Type type, double min, double max, double step, String unit, List<Option> options) {
        this(key, label, type, min, max, step, unit, options, false);
    }

    public record Option(String value, String label) {}

    public static FSHFField number(String key, String label, double min, double max, double step, String unit) {
        return new FSHFField(key, label, Type.NUMBER, min, max, step, unit, List.of());
    }

    /** Número con un deslizador debajo: el cambio se ve en vivo mientras se arrastra. */
    public static FSHFField slider(String key, String label, double min, double max, double step, String unit) {
        return new FSHFField(key, label, Type.NUMBER, min, max, step, unit, List.of(), true);
    }

    /** Medida en metros, con deslizador. */
    public static FSHFField meters(String key, String label, double min, double max) {
        return slider(key, label, min, max, 0.01, "m");
    }

    /** Título de un grupo de campos ("Medidas", "Colores"...). */
    public static FSHFField section(String title) {
        return new FSHFField("section:" + title, title, Type.SECTION, 0, 0, 0, "", List.of());
    }

    public static FSHFField select(String key, String label, List<Option> options) {
        return new FSHFField(key, label, Type.SELECT, 0, 0, 0, "", options);
    }

    public static FSHFField gallery(String key, String label, List<Option> options) {
        return new FSHFField(key, label, Type.GALLERY, 0, 0, 0, "", options);
    }

    public static FSHFField color(String key, String label) {
        return color(key, label, List.of());
    }

    /** @param presets colores sugeridos: valor "#rrggbb" y nombre */
    public static FSHFField color(String key, String label, List<Option> presets) {
        return new FSHFField(key, label, Type.COLOR, 0, 0, 0, "", presets);
    }

    public static FSHFField checkbox(String key, String label) {
        return new FSHFField(key, label, Type.CHECKBOX, 0, 0, 0, "", List.of());
    }
}
