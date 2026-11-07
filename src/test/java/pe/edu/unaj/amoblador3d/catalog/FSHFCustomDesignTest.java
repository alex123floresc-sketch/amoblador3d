package pe.edu.unaj.amoblador3d.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import pe.edu.unaj.amoblador3d.catalog.FSHFCustomDesign.Paint;
import pe.edu.unaj.amoblador3d.catalog.FSHFCustomDesign.Piece;
import pe.edu.unaj.amoblador3d.catalog.FSHFCustomDesign.Shape;
import pe.edu.unaj.amoblador3d.model.FSHFFinish;

class FSHFCustomDesignTest {
    static FSHFCustomDesign stool() {
        FSHFCustomDesign d = new FSHFCustomDesign();
        d.id = "diseno-test";
        d.name = "Banquito";
        d.category = FSHFCatalog.LIVING;
        d.color = "#6d4c33";
        d.color2 = "#5b7fa6";
        d.finish = FSHFFinish.MATE;
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
            leg.finish = i == 2 ? FSHFFinish.METALICO : null;
            d.pieces.add(leg);
        }
        return d;
    }

    @Test
    void jsonIdaYVueltaConservaLasPiezas() {
        FSHFCustomDesign d = stool();
        FSHFCustomDesign back = FSHFCustomDesign.fromJson(d.toJson());
        assertEquals(d.toJson(), back.toJson());
        assertEquals(4, back.pieces.size());
        assertEquals(FSHFFinish.METALICO, back.pieces.get(3).finish);
        assertEquals(Paint.OWN, back.pieces.get(3).paint);
    }

    @Test
    void lasMedidasSonLasDeLaCajaQueEnvuelveLasPiezas() {
        FSHFCustomDesign.Bounds b = stool().bounds();
        assertEquals(0.35, b.width(), 1e-9);
        assertEquals(0.45, b.height(), 1e-9);
        assertEquals(0.35, b.depth(), 1e-9);
        FSHFCatalogEntry e = FSHFCatalogEntry.custom(stool());
        assertEquals(FSHFCatalogEntry.Kind.CUSTOM, e.kind());
        assertEquals(0.45, e.defaults().height(), 1e-9);
        assertEquals("Color secundario", e.accentLabel());
        assertTrue(e.hasMainColor());
    }

    @Test
    void validaNombrePiezasYMedidas() {
        FSHFCustomDesign d = stool();
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
        assertThrows(IllegalArgumentException.class, () -> FSHFCustomDesign.fromJson(j));
        var k = stool().toJson();
        k.remove("id");
        assertThrows(IllegalArgumentException.class, () -> FSHFCustomDesign.fromJson(k));
    }

    @Test
    void elCatalogoMuestraLosDisenosEnSuCategoriaYLosPuedeReemplazar() {
        FSHFCatalog catalog = new FSHFCatalog();
        catalog.setCustom(List.of(stool()));
        assertEquals(FSHFCatalogEntry.Kind.CUSTOM, catalog.get("diseno-test").kind());
        assertEquals(1, catalog.search(FSHFCatalog.LIVING, "banqu").stream().filter(e -> e.id().equals("diseno-test")).count());
        catalog.setCustom(List.of());
        assertEquals(FSHFCatalogEntry.Kind.MISSING, catalog.get("diseno-test").kind());

        FSHFCustomDesign fake = stool();
        fake.id = "cama";
        catalog.setCustom(List.of(fake));
        assertEquals(FSHFCatalogEntry.Kind.PARAMETRIC, catalog.get("cama").kind());
    }
}
