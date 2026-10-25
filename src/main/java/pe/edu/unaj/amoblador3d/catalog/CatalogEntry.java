package pe.edu.unaj.amoblador3d.catalog;

import java.net.URL;

/**
 * Un mueble del catálogo.
 *
 * @param defaults      medidas iniciales (m)
 * @param defaultColor  color inicial (solo paramétricos; null en modelos 3D)
 * @param model         ubicación del archivo .glb/.gltf (solo modelos 3D)
 * @param modelRotation grados para girar el modelo de modo que su frente mire a +Z
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
        double modelRotation) {

    public enum Kind { PARAMETRIC, MODEL, MISSING }

    /** De dónde viene: incluido en la aplicación o importado por el usuario. */
    public enum Source { BUILTIN, USER }

    public record Size(double width, double depth, double height) {}

    public record Range(double min, double max) {}

    /** Los modelos 3D pueden tener cualquier tamaño razonable. */
    public static final Range MODEL_WIDTH = new Range(0.05, 6);
    public static final Range MODEL_DEPTH = new Range(0.05, 6);
    public static final Range MODEL_HEIGHT = new Range(0.05, 4);

    public static CatalogEntry model(String id, String name, Source source, Size size, URL model, double rotation) {
        return new CatalogEntry(id, name, Kind.MODEL, source, size, null, MODEL_WIDTH, MODEL_DEPTH, MODEL_HEIGHT, model, rotation);
    }

    /**
     * Si un proyecto usa un mueble que ya no está en el catálogo, se muestra como una
     * caja roja con sus medidas para no perder la distribución.
     */
    public static CatalogEntry missing(String id) {
        return new CatalogEntry(id, "Falta \"" + id + "\"", Kind.MISSING, Source.BUILTIN, new Size(1, 1, 1), null,
                MODEL_WIDTH, MODEL_DEPTH, MODEL_HEIGHT, null, 0);
    }
}
