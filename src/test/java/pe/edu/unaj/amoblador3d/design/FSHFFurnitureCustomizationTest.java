package pe.edu.unaj.amoblador3d.design;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalog;
import pe.edu.unaj.amoblador3d.catalog.FSHFCustomDesign;
import pe.edu.unaj.amoblador3d.model.FSHFFinish;
import pe.edu.unaj.amoblador3d.model.FSHFFurniture;
import pe.edu.unaj.amoblador3d.model.FSHFRoomDims;

class FSHFFurnitureCustomizationTest {
    private FSHFCatalog catalog;
    private FSHFDesign design;

    private static FSHFCustomDesign shelf() {
        FSHFCustomDesign d = new FSHFCustomDesign();
        d.id = "diseno-repisa";
        d.name = "Repisa";
        FSHFCustomDesign.Piece p = new FSHFCustomDesign.Piece();
        p.width = 0.8;
        p.height = 0.03;
        p.depth = 0.25;
        p.y = 0.5;
        d.pieces.add(p);
        FSHFCustomDesign.Piece leg = p.copy();
        leg.y = 0;
        leg.width = 0.03;
        leg.height = 0.5;
        leg.paint = FSHFCustomDesign.Paint.ACCENT;
        d.pieces.add(leg);
        return d;
    }

    @BeforeEach
    void setUp() {
        catalog = new FSHFCatalog();
        catalog.setCustom(List.of(shelf()));
        design = new FSHFDesign(catalog);
        design.loadDefault();
    }

    private FSHFEditable editor(FSHFFurniture f) {
        return FSHFEditors.of(design, f, x -> null);
    }

    private static boolean hasField(FSHFEditable e, String key) {
        return e.fields().stream().anyMatch(f -> f.key().equals(key));
    }

    @Test
    void colorSecundarioYAcabadoSeAplicanYSeDeshacen() {
        FSHFFurniture bed = design.addFurniture("cama").value();
        FSHFEditable e = editor(bed);
        assertTrue(hasField(e, "color2"), "la cama debe tener color de frazada");
        assertNull(e.set("color2", "#8c2f39"));
        assertNull(e.set("finish", FSHFFinish.BRILLANTE.id()));
        assertEquals("#8c2f39", bed.color2);
        assertEquals(FSHFFinish.BRILLANTE, bed.finish);
        assertNull(e.set("finish", "auto"));
        assertNull(bed.finish);
    }

    @Test
    void sePuedeCambiarElColorDeUnMuebleQueYaChoca() {
        FSHFFurniture bed = design.addFurniture("cama").value();

        design.setRoom(new FSHFRoomDims(1.5, 2.0, 2.5, 0.15));
        assertNotNull(design.checkFurniture(bed, bed));
        assertNull(editor(bed).set("color", "#ffffff"), "antes este cambio se rechazaba con «Choca con…»");
        assertEquals("#ffffff", bed.color);
    }

    @Test
    void agrandarYAchicarRespetaLosLimites() {
        FSHFFurniture chair = design.addFurniture("silla").value();
        FSHFEditable e = editor(chair);
        FSHFEditable.Action bigger = e.actions().stream().filter(a -> a.label().equals("+ 10 %")).findFirst().orElseThrow();
        for (int i = 0; i < 10; i++) bigger.run().get();
        assertTrue(chair.width <= 0.7 + 1e-9 && chair.height <= 1.2 + 1e-9, "no pasa del máximo de la silla");
        FSHFEditable.Action original = e.actions().stream().filter(a -> a.label().equals("Medidas originales")).findFirst().orElseThrow();
        assertNull(original.run().get());
        assertEquals(0.45, chair.width, 1e-9);
    }

    @Test
    void unMuebleDisenadoSeColocaConSusMedidasYColores() {
        FSHFFurniture f = design.addFurniture("diseno-repisa").value();
        assertEquals(0.8, f.width, 1e-9);
        assertEquals(0.53, f.height, 1e-9);
        assertEquals(new FSHFCustomDesign().color, f.color);
        FSHFEditable e = editor(f);
        assertTrue(hasField(e, "color") && hasField(e, "color2") && hasField(e, "finish"));
        assertNull(e.set("width", 1.2));
        assertEquals(1.2, f.width, 1e-9);
    }

    @Test
    void reemplazarPorUnDisenoConservaElLugar() {
        FSHFFurniture desk = design.addFurniture("escritorio").value();
        double x = desk.x, z = desk.z;
        desk.color2 = "#123456";
        assertNull(design.replaceType(desk, "diseno-repisa"));
        assertEquals("diseno-repisa", desk.type);
        assertEquals(x, desk.x, 0.5);
        assertEquals(z, desk.z, 0.5);
        assertNull(desk.color2);
        assertTrue(desk.id.startsWith("diseno-repisa-"));
        assertNotNull(design.replaceType(desk, "no-existe"));
    }
}
