package pe.edu.unaj.amoblador3d.catalog;

import java.net.URL;

/**
 * Un mueble del catálogo.
 *
 * @param defaults      medidas iniciales (m)
 * @param defaultColor  color inicial (paramétricos y diseños propios; null en modelos 3D)
 * @param model         ubicación del archivo .glb/.gltf (solo modelos 3D)
 * @param modelRotation grados para girar el modelo de modo que su frente mire a +Z
 * @param category      grupo en el que se muestra ("Dormitorio", "Cocina"... ver {@link Catalog#CATEGORIES})
 * @param accentLabel   qué pinta el color secundario ("Frazada", "Cojines"...), o null si no tiene
 * @param design        piezas del mueble (solo diseños propios)
 */
public record CatalogEntry(
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
        CustomDesign design) {

    /** PARAMETRIC: hecho con código; MODEL: archivo glTF; CUSTOM: hecho en el Diseñador de muebles. */
    public enum Kind { PARAMETRIC, MODEL, CUSTOM, MISSING }

    /** De dónde viene: incluido en la aplicación o importado por el usuario. */
    public enum Source { BUILTIN, USER }

    public record Size(double width, double depth, double height) {}

    public record Range(double min, double max) {}

    /** Los modelos 3D pueden tener cualquier tamaño razonable. */
    public static final Range MODEL_WIDTH = new Range(0.05, 6);
    public static final Range MODEL_DEPTH = new Range(0.05, 6);
    public static final Range MODEL_HEIGHT = new Range(0.05, 4);

    public static CatalogEntry model(String id, String name, Source source, Size size, URL model, double rotation,
                                     String category) {
        return new CatalogEntry(id, name, Kind.MODEL, source, size, null, MODEL_WIDTH, MODEL_DEPTH, MODEL_HEIGHT, model,
                rotation, category, null, null);
    }

    /** Mueble del Diseñador: sus medidas iniciales son las de sus piezas. */
    public static CatalogEntry custom(CustomDesign d) {
        CustomDesign.Bounds b = d.bounds();
        Size size = new Size(clampSize(b.width(), MODEL_WIDTH), clampSize(b.depth(), MODEL_DEPTH), clampSize(b.height(), MODEL_HEIGHT));
        return new CatalogEntry(d.id, d.name, Kind.CUSTOM, Source.USER, size, d.color, MODEL_WIDTH, MODEL_DEPTH, MODEL_HEIGHT,
                null, 0, d.category, d.uses(CustomDesign.Paint.ACCENT) ? "Color secundario" : null, d);
    }

    private static double clampSize(double v, Range r) {
        return Math.round(Math.min(Math.max(v, r.min()), r.max()) * 100) / 100.0;
    }

    /** true si se le puede cambiar el color principal (los modelos 3D solo se tiñen). */
    public boolean hasMainColor() {
        return kind == Kind.PARAMETRIC || (kind == Kind.CUSTOM && design.uses(CustomDesign.Paint.MAIN));
    }

    /**
     * Si un proyecto usa un mueble que ya no está en el catálogo, se muestra como una
     * caja roja con sus medidas para no perder la distribución.
     */
    public static CatalogEntry missing(String id) {
        return new CatalogEntry(id, "Falta \"" + id + "\"", Kind.MISSING, Source.BUILTIN, new Size(1, 1, 1), null,
                MODEL_WIDTH, MODEL_DEPTH, MODEL_HEIGHT, null, 0, Catalog.IMPORTED, null, null);
    }
}
