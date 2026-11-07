package pe.edu.unaj.amoblador3d.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import pe.edu.unaj.amoblador3d.estimate.FSHFPriceList;

class FSHFCatalogTest {
    @Test
    void laBusquedaIgnoraMayusculasYTildes() {
        FSHFCatalog c = new FSHFCatalog();
        assertEquals(List.of("comoda"), c.search(FSHFCatalog.BEDROOM, "COMODA").stream().map(FSHFCatalogEntry::id).toList());
        assertEquals(List.of("lavatorio"), c.search(FSHFCatalog.BATHROOM, "lavat").stream().map(FSHFCatalogEntry::id).toList());
        assertTrue(c.search(FSHFCatalog.KITCHEN, "cama").isEmpty());
        assertEquals(4, c.search(FSHFCatalog.BEDROOM, "").size());
    }

    @Test
    void todoMuebleTieneCategoriaValidaYPrecio() {
        FSHFCatalog c = new FSHFCatalog();
        List<String> warnings = new ArrayList<>();
        c.setModels(FSHFBuiltinCatalog.load(warnings));
        assertTrue(warnings.isEmpty(), warnings.toString());
        for (FSHFCatalogEntry.Kind kind : List.of(FSHFCatalogEntry.Kind.PARAMETRIC, FSHFCatalogEntry.Kind.MODEL)) {
            for (FSHFCatalogEntry e : c.list(kind)) {
                assertTrue(FSHFCatalog.CATEGORIES.contains(e.category()), e.id() + ": " + e.category());
                if (kind == FSHFCatalogEntry.Kind.PARAMETRIC) {
                    assertTrue(FSHFPriceList.defaultPrice(FSHFPriceList.Key.furniture(e.id())) > 0, e.id() + " sin precio");
                }
            }
        }

        assertEquals(FSHFCatalog.LIVING, c.get("sofa").category());
    }
}
