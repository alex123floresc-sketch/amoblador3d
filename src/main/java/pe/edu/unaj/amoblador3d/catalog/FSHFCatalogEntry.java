package pe.edu.unaj.amoblador3d.catalog;

import java.net.URL;

public record FSHFCatalogEntry(
        String id,
        String name,
        Kind kind,
        Source source,
        Size defaults,
        String defaultColor,
        Range widthRange,
        Range depthRange,
        Range heightRange,
        URL model,
        double modelRotation,
        String category,
        String accentLabel,
        FSHFCustomDesign design) {
    public enum Kind { PARAMETRIC, MODEL, CUSTOM, MISSING }

    public enum Source { BUILTIN, USER }

    public record Size(double width, double depth, double height) {}

    public record Range(double min, double max) {}

    public static final Range MODEL_WIDTH = new Range(0.05, 6);
    public static final Range MODEL_DEPTH = new Range(0.05, 6);
    public static final Range MODEL_HEIGHT = new Range(0.05, 4);

    public static FSHFCatalogEntry model(String id, String name, Source source, Size size, URL model, double rotation,
                                     String category) {
        return new FSHFCatalogEntry(id, name, Kind.MODEL, source, size, null, MODEL_WIDTH, MODEL_DEPTH, MODEL_HEIGHT, model,
                rotation, category, null, null);
    }

    public static FSHFCatalogEntry custom(FSHFCustomDesign d) {
        FSHFCustomDesign.Bounds b = d.bounds();
        Size size = new Size(clampSize(b.width(), MODEL_WIDTH), clampSize(b.depth(), MODEL_DEPTH), clampSize(b.height(), MODEL_HEIGHT));
        return new FSHFCatalogEntry(d.id, d.name, Kind.CUSTOM, Source.USER, size, d.color, MODEL_WIDTH, MODEL_DEPTH, MODEL_HEIGHT,
                null, 0, d.category, d.uses(FSHFCustomDesign.Paint.ACCENT) ? "Color secundario" : null, d);
    }

    private static double clampSize(double v, Range r) {
        return Math.round(Math.min(Math.max(v, r.min()), r.max()) * 100) / 100.0;
    }

    public boolean hasMainColor() {
        return kind == Kind.PARAMETRIC || (kind == Kind.CUSTOM && design.uses(FSHFCustomDesign.Paint.MAIN));
    }

    public static FSHFCatalogEntry missing(String id) {
        return new FSHFCatalogEntry(id, "Falta \"" + id + "\"", Kind.MISSING, Source.BUILTIN, new Size(1, 1, 1), null,
                MODEL_WIDTH, MODEL_DEPTH, MODEL_HEIGHT, null, 0, FSHFCatalog.IMPORTED, null, null);
    }
}
