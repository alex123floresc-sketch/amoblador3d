package pe.edu.unaj.amoblador3d.design;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.unaj.amoblador3d.catalog.Catalog;
import pe.edu.unaj.amoblador3d.model.Furniture;
import pe.edu.unaj.amoblador3d.model.Opening;
import pe.edu.unaj.amoblador3d.model.OpeningType;
import pe.edu.unaj.amoblador3d.model.Project;
import pe.edu.unaj.amoblador3d.model.RoomDims;
import pe.edu.unaj.amoblador3d.model.WallSide;
import pe.edu.unaj.amoblador3d.util.Outcome;

/** Tipos de puerta y escaleras. */
class DoorsAndStairsTest {
    private Design design;

    @BeforeEach
    void setUp() {
        design = new Design(new Catalog());
        design.load(new Project());
    }

    @Test
    void cadaTipoDePuertaSeAgregaConSusMedidas() {
        for (OpeningType t : OpeningType.doors()) {
            design.load(new Project());
            Outcome<Opening> out = design.addOpening(t);
            assertNull(out.error(), t.label());
            Opening o = out.value();
            assertEquals(t, o.type);
            assertEquals(t.defaultWidth(), o.width, 1e-9);
            assertEquals(0, o.sill, 1e-9);
            assertEquals(WallSide.SUR, o.wall, "las puertas empiezan por el sur");
        }
    }

    @Test
    void laPuertaPrincipalVaEnLaParedPedida() {
        Opening o = design.addOpening(OpeningType.ENTRY, List.of(WallSide.OESTE)).value();
        assertEquals(WallSide.OESTE, o.wall);
        // Un portón de 2,6 m no cabe en una pared de 2 m: avisa en vez de ponerlo en otra.
        design.load(projectOf(new RoomDims(4, 2, 2.5, 0.15)));
        Outcome<Opening> out = design.addOpening(OpeningType.GARAGE, List.of(WallSide.ESTE));
        assertNotNull(out.error());
        assertTrue(out.error().contains("esa pared"), out.error());
    }

    @Test
    void enUnaParedBajaLaPuertaSeAchica() {
        design.load(projectOf(new RoomDims(4, 3.5, 2.1, 0.15)));
        Opening o = design.addOpening(OpeningType.ENTRY).value();
        assertEquals(2.05, o.height, 1e-9);
    }

    @Test
    void elTipoDePuertaSeCambiaDesdeElPanel() {
        Opening door = design.addOpening(OpeningType.DOOR).value();
        Editable e = Editors.of(design, door, f -> null);
        assertEquals("door", e.get("type"));
        assertNull(e.set("type", "double"));
        assertEquals(OpeningType.DOUBLE, door.type);
        assertEquals(OpeningType.DOUBLE.defaultWidth(), door.width, 1e-9);
        assertNull(e.set("type", "passage"));
        assertEquals(OpeningType.PASSAGE, door.type);
        // Una ventana no ofrece tipos de puerta.
        Opening window = design.addOpening(OpeningType.WINDOW).value();
        assertTrue(Editors.of(design, window, f -> null).fields().stream().noneMatch(f -> f.key().equals("type")));
    }

    @Test
    void lasEscalerasLleganAlTecho() {
        design.load(projectOf(new RoomDims(5, 5, 2.7, 0.15)));
        for (String type : List.of("escalera", "escalera-l", "escalera-caracol")) {
            Furniture f = design.addFurniture(type).value();
            assertNotNull(f, type);
            assertEquals(2.7, f.height, 1e-9, type);
        }
        // La baranda no es escalera: conserva su alto.
        assertEquals(0.9, design.addFurniture("baranda").value().height, 1e-9);
        assertTrue(Catalog.isStair("escalera-l"));
    }

    private static Project projectOf(RoomDims dims) {
        Project p = new Project();
        p.room = dims;
        return p;
    }
}
