package pe.edu.unaj.amoblador3d.design;

import java.util.List;

public record FSHFField(String key, String label, Type type, double min, double max, double step, String unit, List<Option> options,
                    boolean slider) {
    public enum Type { NUMBER, SELECT, GALLERY, COLOR, CHECKBOX, SECTION }

    public FSHFField(String key, String label, Type type, double min, double max, double step, String unit, List<Option> options) {
        this(key, label, type, min, max, step, unit, options, false);
    }

    public record Option(String value, String label) {}

    public static FSHFField number(String key, String label, double min, double max, double step, String unit) {
        return new FSHFField(key, label, Type.NUMBER, min, max, step, unit, List.of());
    }

    public static FSHFField slider(String key, String label, double min, double max, double step, String unit) {
        return new FSHFField(key, label, Type.NUMBER, min, max, step, unit, List.of(), true);
    }

    public static FSHFField meters(String key, String label, double min, double max) {
        return slider(key, label, min, max, 0.01, "m");
    }

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

    public static FSHFField color(String key, String label, List<Option> presets) {
        return new FSHFField(key, label, Type.COLOR, 0, 0, 0, "", presets);
    }

    public static FSHFField checkbox(String key, String label) {
        return new FSHFField(key, label, Type.CHECKBOX, 0, 0, 0, "", List.of());
    }
}
