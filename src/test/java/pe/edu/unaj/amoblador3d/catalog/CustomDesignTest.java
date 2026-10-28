package pe.edu.unaj.amoblador3d.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import pe.edu.unaj.amoblador3d.catalog.CustomDesign.Paint;
import pe.edu.unaj.amoblador3d.catalog.CustomDesign.Piece;
import pe.edu.unaj.amoblador3d.catalog.CustomDesign.Shape;
import pe.edu.unaj.amoblador3d.model.Finish;

class CustomDesignTest {
    static CustomDesign stool() {
        CustomDesign d = new CustomDesign();
        d.id = "diseno-test";
        d.name = "Banquito";
        d.category = Catalog.LIVING;
        d.color = "#6d4c33";
        d.color2 = "#5b7fa6";
        d.finish = Finish.MATE;
        Piece seat = new Piece();
        seat.shape = Shape.CYLINDER;
        seat.y = 0.4;
        seat.width = 0.35;
        seat.height = 0.05;
        seat.depth = 0.35;
        seat.paint = Paint.ACCENT;
        d.pieces.add(seat);
        for (int i = 0; i < 3; i++) {
            Piece leg = new Piece();
            leg.x = i * 0.1 - 0.1;
            leg.width = 0.03;
            leg.height = 0.4;
            leg.depth = 0.03;
            leg.paint = i == 2 ? Paint.OWN : Paint.MAIN;
            leg.color = "#c0c0c0";
            leg.finish = i == 2 ? Finish.METALICO : null;
            d.pieces.add(leg);
        }
        return d;
    }

    @Test
    void jsonIdaYVueltaConservaLasPiezas() {
        CustomDesign d = stool();
        CustomDesign back = CustomDesign.fromJson(d.toJson());
        assertEquals(d.toJson(), back.toJson());
        assertEquals(4, back.pieces.size());
        assertEquals(Finish.METALICO, back.pieces.get(3).finish);
        assertEquals(Paint.OWN, back.pieces.get(3).paint);
    }

    @Test
    void lasMedidasSonLasDeLaCajaQueEnvuelveLasPiezas() {
        CustomDesign.Bounds b = stool().bounds();
        assertEquals(0.35, b.width(), 1e-9);
        assertEquals(0.45, b.height(), 1e-9);
        assertEquals(0.35, b.depth(), 1e-9);
        CatalogEntry e = CatalogEntry.custom(stool());
        assertEquals(CatalogEntry.Kind.CUSTOM, e.kind());
        assertEquals(0.45, e.defaults().height(), 1e-9);
        assertEquals("Color secundario", e.accentLabel()); // el asiento usa el secundario
        assertTrue(e.hasMainColor());
    }

    @Test
    void validaNombrePiezasYMedidas() {
        CustomDesign d = stool();
        assertNull(d.validate());
        d.name = "  ";
        assertNotNull(d.validate());
        d = stool();
        d.pieces.clear();
        assertNotNull(d.validate());
        d = stool();
        d.pieces.get(0).width = 0;
        assertNotNull(d.validate());
        d = stool();
        d.pieces.get(1).y = -1;
        assertNotNull(d.validate());
        d = stool();
        d.pieces.get(3).color = "rojo";
        assertNotNull(d.validate());
    }

    @Test
    void unJsonDanadoNoSeAcepta() {
        var j = stool().toJson();
        j.getAsJsonArray("pieces").get(0).getAsJsonObject().addProperty("shape", "piramide");
        assertThrows(IllegalArgumentException.class, () -> CustomDesign.fromJson(j));
        var k = stool().toJson();
        k.remove("id");
        assertThrows(IllegalArgumentException.class, () -> CustomDesign.fromJson(k));
    }

    @Test
    void elCatalogoMuestraLosDisenosEnSuCategoriaYLosPuedeReemplazar() {
        Catalog catalog = new Catalog();
        catalog.setCustom(List.of(stool()));
        assertEquals(CatalogEntry.Kind.CUSTOM, catalog.get("diseno-test").kind());
        assertEquals(1, catalog.search(Catalog.LIVING, "banqu").stream().filter(e -> e.id().equals("diseno-test")).count());
        catalog.setCustom(List.of());
        assertEquals(CatalogEntry.Kind.MISSING, catalog.get("diseno-test").kind());
        // Un diseño no puede ocultar a un mueble paramétrico con el mismo id.
        CustomDesign fake = stool();
        fake.id = "cama";
        catalog.setCustom(List.of(fake));
        assertEquals(CatalogEntry.Kind.PARAMETRIC, catalog.get("cama").kind());
    }
}
