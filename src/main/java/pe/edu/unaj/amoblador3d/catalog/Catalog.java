package pe.edu.unaj.amoblador3d.catalog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import pe.edu.unaj.amoblador3d.catalog.CatalogEntry.Kind;
import pe.edu.unaj.amoblador3d.catalog.CatalogEntry.Range;
import pe.edu.unaj.amoblador3d.catalog.CatalogEntry.Size;
import pe.edu.unaj.amoblador3d.catalog.CatalogEntry.Source;

/**
 * Registro central del catálogo: los muebles paramétricos (construidos con cajas,
 * ver {@code scene.ParametricBuilder}) más los modelos 3D (.glb/.gltf) incluidos e importados.
 */
public final class Catalog {
    private final Map<String, CatalogEntry> entries = new LinkedHashMap<>();

    public Catalog() {
        parametric("cama", "Cama", new Size(1.4, 1.9, 0.9), "#8b6b4a", r(0.8, 2.2), r(1.8, 2.3), r(0.5, 1.4));
        parametric("mesa", "Mesa", new Size(1.2, 0.8, 0.75), "#a0764f", r(0.4, 3), r(0.4, 1.5), r(0.4, 1.1));
        parametric("silla", "Silla", new Size(0.45, 0.5, 0.9), "#6d4c33", r(0.35, 0.7), r(0.35, 0.7), r(0.7, 1.2));
        parametric("closet", "Closet", new Size(1.2, 0.6, 2.0), "#e8e2d6", r(0.5, 3), r(0.4, 0.8), r(1.2, 2.6));
        parametric("escritorio", "Escritorio", new Size(1.2, 0.6, 0.75), "#d9c6a5", r(0.8, 2.4), r(0.45, 0.9), r(0.65, 0.9));
    }

    private static Range r(double min, double max) {
        return new Range(min, max);
    }

    private void parametric(String id, String name, Size size, String color, Range w, Range d, Range h) {
        entries.put(id, new CatalogEntry(id, name, Kind.PARAMETRIC, Source.BUILTIN, size, color, w, d, h, null, 0));
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

    /** Reemplaza los modelos 3D por los indicados (incluidos + importados). */
    public void setModels(List<CatalogEntry> models) {
        entries.values().removeIf(e -> e.kind() == Kind.MODEL);
        for (CatalogEntry m : models) {
            // Un modelo no puede ocultar a un mueble paramétrico con el mismo id.
            if (!entries.containsKey(m.id())) entries.put(m.id(), m);
        }
    }
}
