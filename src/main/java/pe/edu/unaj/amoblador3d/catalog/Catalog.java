package pe.edu.unaj.amoblador3d.catalog;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import pe.edu.unaj.amoblador3d.catalog.CatalogEntry.Kind;
import pe.edu.unaj.amoblador3d.catalog.CatalogEntry.Range;
import pe.edu.unaj.amoblador3d.catalog.CatalogEntry.Size;
import pe.edu.unaj.amoblador3d.catalog.CatalogEntry.Source;

/**
 * Registro central del catálogo: los muebles paramétricos (construidos con cajas,
 * ver {@code scene.ParametricBuilder}), los hechos en el Diseñador de muebles ({@link CustomDesign})
 * y los modelos 3D (.glb/.gltf) incluidos e importados.
 */
public final class Catalog {
    public static final String BEDROOM = "Dormitorio", LIVING = "Sala", DINING = "Comedor", KITCHEN = "Cocina",
            BATHROOM = "Baño", OFFICE = "Oficina", DECORATION = "Decoración", MY_DESIGNS = "Mis diseños",
            IMPORTED = "Importados";
    /** Grupos del catálogo, en el orden en que se muestran. */
    public static final List<String> CATEGORIES =
            List.of(MY_DESIGNS, BEDROOM, LIVING, DINING, KITCHEN, BATHROOM, OFFICE, DECORATION, IMPORTED);

    private final Map<String, CatalogEntry> entries = new LinkedHashMap<>();

    public Catalog() {
        parametric("cama", "Cama", BEDROOM, new Size(1.4, 1.9, 0.9), "#8b6b4a", "Frazada", r(0.8, 2.2), r(1.8, 2.3), r(0.5, 1.4));
        parametric("mesa-noche", "Mesa de noche", BEDROOM, new Size(0.45, 0.4, 0.55), "#8b6b4a", "Frentes de cajones", r(0.3, 0.7), r(0.3, 0.6), r(0.4, 0.8));
        parametric("comoda", "Cómoda", BEDROOM, new Size(1.0, 0.45, 0.85), "#d9c6a5", "Frentes de cajones", r(0.6, 2), r(0.35, 0.6), r(0.6, 1.3));
        parametric("closet", "Closet", BEDROOM, new Size(1.2, 0.6, 2.0), "#e8e2d6", "Puertas", r(0.5, 3), r(0.4, 0.8), r(1.2, 2.6));
        parametric("sillon", "Sofá", LIVING, new Size(2.0, 0.9, 0.85), "#6f7f8f", "Cojines", r(1.2, 3.2), r(0.7, 1.1), r(0.7, 1.1));
        parametric("rack-tv", "Mueble de TV", LIVING, new Size(1.6, 0.45, 1.25), "#3b3b3b", "Puertas", r(1, 2.6), r(0.35, 0.6), r(0.9, 1.6));
        parametric("librero", "Librero", LIVING, new Size(0.8, 0.3, 1.8), "#a0764f", "Fondo", r(0.4, 2.4), r(0.2, 0.5), r(0.6, 2.4));
        parametric("mesa", "Mesa", DINING, new Size(1.2, 0.8, 0.75), "#a0764f", "Patas", r(0.4, 3), r(0.4, 1.5), r(0.4, 1.1));
        parametric("silla", "Silla", DINING, new Size(0.45, 0.5, 0.9), "#6d4c33", "Asiento", r(0.35, 0.7), r(0.35, 0.7), r(0.7, 1.2));
        parametric("cocina", "Mueble de cocina", KITCHEN, new Size(1.8, 0.6, 0.9), "#f4f1ea", "Encimera", r(0.6, 4), r(0.5, 0.7), r(0.8, 1));
        parametric("refrigeradora", "Refrigeradora", KITCHEN, new Size(0.7, 0.7, 1.8), "#d9dcdf", "Tiradores", r(0.5, 1), r(0.55, 0.8), r(1.2, 2.1));
        parametric("inodoro", "Inodoro", BATHROOM, new Size(0.4, 0.7, 0.75), "#ffffff", "Asiento", r(0.35, 0.5), r(0.6, 0.8), r(0.65, 0.85));
        parametric("lavatorio", "Lavatorio", BATHROOM, new Size(0.55, 0.45, 0.85), "#ffffff", "Grifería", r(0.4, 0.8), r(0.35, 0.6), r(0.75, 0.95));
        parametric("escritorio", "Escritorio", OFFICE, new Size(1.2, 0.6, 0.75), "#d9c6a5", "Frentes de cajones", r(0.8, 2.4), r(0.45, 0.9), r(0.65, 0.9));
    }

    /**
     * Color secundario "automático" de un mueble paramétrico (el que se ve mientras el usuario no
     * elige otro): algunos tienen uno fijo (la frazada azul, la encimera de piedra...) y el resto
     * usa el principal un poco más oscuro. Debe coincidir con {@code scene.ParametricBuilder}.
     */
    public static String autoAccent(String type, String main) {
        return switch (type) {
            case "cama" -> "#5b7fa6";
            case "cocina" -> "#bdb7ad";
            case "inodoro" -> "#f0f0f0";
            case "refrigeradora", "lavatorio" -> "#c0c0c0";
            case "mesa" -> main;
            default -> darker(main, 0.88);
        };
    }

    private static String darker(String hex, double k) {
        if (hex == null || !hex.matches("^#[0-9a-fA-F]{6}$")) return "#808080";
        int rgb = Integer.parseInt(hex.substring(1), 16);
        int r = (int) Math.round((rgb >> 16 & 255) * k), g = (int) Math.round((rgb >> 8 & 255) * k), b = (int) Math.round((rgb & 255) * k);
        return String.format("#%02x%02x%02x", r, g, b);
    }

    private static Range r(double min, double max) {
        return new Range(min, max);
    }

    private void parametric(String id, String name, String category, Size size, String color, String accent,
                            Range w, Range d, Range h) {
        entries.put(id, new CatalogEntry(id, name, Kind.PARAMETRIC, Source.BUILTIN, size, color, w, d, h, null, 0, category,
                accent, null));
    }

    /** Devuelve la entrada del catálogo; si ya no existe, una caja roja de reemplazo. */
    public CatalogEntry get(String id) {
        CatalogEntry e = entries.get(id);
        return e != null ? e : CatalogEntry.missing(id);
    }

    public boolean contains(String id) {
        return entries.containsKey(id);
    }

    public List<CatalogEntry> list(Kind kind) {
        List<CatalogEntry> result = new ArrayList<>();
        for (CatalogEntry e : entries.values()) if (e.kind() == kind) result.add(e);
        return result;
    }

    /**
     * Muebles de una categoría cuyo nombre contiene el texto buscado (sin distinguir
     * mayúsculas ni tildes: "comoda" encuentra "Cómoda"). Los paramétricos van primero, luego los
     * diseños propios y al final los modelos 3D.
     */
    public List<CatalogEntry> search(String category, String text) {
        String q = fold(text == null ? "" : text.trim());
        List<CatalogEntry> result = new ArrayList<>();
        for (Kind kind : List.of(Kind.PARAMETRIC, Kind.CUSTOM, Kind.MODEL)) {
            for (CatalogEntry e : entries.values()) {
                if (e.kind() == kind && e.category().equals(category) && fold(e.name()).contains(q)) result.add(e);
            }
        }
        return result;
    }

    private static String fold(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    /** Reemplaza los modelos 3D por los indicados (incluidos + importados). */
    public void setModels(List<CatalogEntry> models) {
        entries.values().removeIf(e -> e.kind() == Kind.MODEL);
        for (CatalogEntry m : models) {
            // Un modelo no puede ocultar a un mueble paramétrico con el mismo id.
            if (!entries.containsKey(m.id())) entries.put(m.id(), m);
        }
    }

    /** Reemplaza los muebles hechos en el Diseñador por los indicados. */
    public void setCustom(List<CustomDesign> designs) {
        entries.values().removeIf(e -> e.kind() == Kind.CUSTOM);
        for (CustomDesign d : designs) {
            if (!entries.containsKey(d.id)) entries.put(d.id, CatalogEntry.custom(d));
        }
    }
}
