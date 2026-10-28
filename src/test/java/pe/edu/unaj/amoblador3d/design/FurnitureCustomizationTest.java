package pe.edu.unaj.amoblador3d.design;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.unaj.amoblador3d.catalog.Catalog;
import pe.edu.unaj.amoblador3d.catalog.CustomDesign;
import pe.edu.unaj.amoblador3d.model.Finish;
import pe.edu.unaj.amoblador3d.model.Furniture;
import pe.edu.unaj.amoblador3d.model.RoomDims;

/** Colores, acabado y tamaño de los muebles, y muebles del Diseñador en la habitación. */
class FurnitureCustomizationTest {
    private Catalog catalog;
    private Design design;

    private static CustomDesign shelf() {
        CustomDesign d = new CustomDesign();
        d.id = "diseno-repisa";
        d.name = "Repisa";
        CustomDesign.Piece p = new CustomDesign.Piece();
        p.width = 0.8;
        p.height = 0.03;
        p.depth = 0.25;
        p.y = 0.5;
        d.pieces.add(p);
        CustomDesign.Piece leg = p.copy();
        leg.y = 0;
        leg.width = 0.03;
        leg.height = 0.5;
        leg.paint = CustomDesign.Paint.ACCENT;
        d.pieces.add(leg);
        return d;
    }

    @BeforeEach
    void setUp() {
        catalog = new Catalog();
        catalog.setCustom(List.of(shelf()));
        design = new Design(catalog);
        design.loadDefault();
    }

    private Editable editor(Furniture f) {
        return Editors.of(design, f, x -> null);
    }

    private static boolean hasField(Editable e, String key) {
        return e.fields().stream().anyMatch(f -> f.key().equals(key));
    }

    @Test
    void colorSecundarioYAcabadoSeAplicanYSeDeshacen() {
        Furniture bed = design.addFurniture("cama").value();
        Editable e = editor(bed);
        assertTrue(hasField(e, "color2"), "la cama debe tener color de frazada");
        assertNull(e.set("color2", "#8c2f39"));
        assertNull(e.set("finish", Finish.BRILLANTE.id()));
        assertEquals("#8c2f39", bed.color2);
        assertEquals(Finish.BRILLANTE, bed.finish);
        assertNull(e.set("finish", "auto"));
        assertNull(bed.finish);
    }

    @Test
    void sePuedeCambiarElColorDeUnMuebleQueYaChoca() {
        Furniture bed = design.addFurniture("cama").value();
        // Al achicar mucho la habitación la cama queda chocando (se avisa, pero no se borra).
        design.setRoom(new RoomDims(1.5, 2.0, 2.5, 0.15));
        assertNotNull(design.checkFurniture(bed, bed));
        assertNull(editor(bed).set("color", "#ffffff"), "antes este cambio se rechazaba con «Choca con…»");
        assertEquals("#ffffff", bed.color);
    }

    @Test
    void agrandarYAchicarRespetaLosLimites() {
        Furniture chair = design.addFurniture("silla").value();
        Editable e = editor(chair);
        Editable.Action bigger = e.actions().stream().filter(a -> a.label().equals("+ 10 %")).findFirst().orElseThrow();
        for (int i = 0; i < 10; i++) bigger.run().get();
        assertTrue(chair.width <= 0.7 + 1e-9 && chair.height <= 1.2 + 1e-9, "no pasa del máximo de la silla");
        Editable.Action original = e.actions().stream().filter(a -> a.label().equals("Medidas originales")).findFirst().orElseThrow();
        assertNull(original.run().get());
        assertEquals(0.45, chair.width, 1e-9);
    }

    @Test
    void unMuebleDisenadoSeColocaConSusMedidasYColores() {
        Furniture f = design.addFurniture("diseno-repisa").value();
        assertEquals(0.8, f.width, 1e-9);
        assertEquals(0.53, f.height, 1e-9);
        assertEquals(new CustomDesign().color, f.color);
        Editable e = editor(f);
        assertTrue(hasField(e, "color") && hasField(e, "color2") && hasField(e, "finish"));
        assertNull(e.set("width", 1.2));
        assertEquals(1.2, f.width, 1e-9);
    }

    @Test
    void reemplazarPorUnDisenoConservaElLugar() {
        Furniture desk = design.addFurniture("escritorio").value();
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
