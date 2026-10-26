package pe.edu.unaj.amoblador3d.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import pe.edu.unaj.amoblador3d.estimate.PriceList;

class CatalogTest {
    @Test
    void laBusquedaIgnoraMayusculasYTildes() {
        Catalog c = new Catalog();
        assertEquals(List.of("comoda"), c.search(Catalog.BEDROOM, "COMODA").stream().map(CatalogEntry::id).toList());
        assertEquals(List.of("lavatorio"), c.search(Catalog.BATHROOM, "lavat").stream().map(CatalogEntry::id).toList());
        assertTrue(c.search(Catalog.KITCHEN, "cama").isEmpty());
        assertEquals(4, c.search(Catalog.BEDROOM, "").size());
    }

    @Test
    void todoMuebleTieneCategoriaValidaYPrecio() {
        Catalog c = new Catalog();
        List<String> warnings = new ArrayList<>();
        c.setModels(BuiltinCatalog.load(warnings));
        assertTrue(warnings.isEmpty(), warnings.toString());
        for (CatalogEntry.Kind kind : List.of(CatalogEntry.Kind.PARAMETRIC, CatalogEntry.Kind.MODEL)) {
            for (CatalogEntry e : c.list(kind)) {
                assertTrue(Catalog.CATEGORIES.contains(e.category()), e.id() + ": " + e.category());
                if (kind == CatalogEntry.Kind.PARAMETRIC) {
                    assertTrue(PriceList.defaultPrice(PriceList.Key.furniture(e.id())) > 0, e.id() + " sin precio");
                }
            }
        }
        // Los modelos incluidos usan el campo "category" de catalog.json.
        assertEquals(Catalog.LIVING, c.get("sofa").category());
    }
}
