package pe.edu.unaj.amoblador3d.design;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalog;
import pe.edu.unaj.amoblador3d.model.FSHFFurniture;
import pe.edu.unaj.amoblador3d.model.FSHFOpening;
import pe.edu.unaj.amoblador3d.model.FSHFOpeningType;
import pe.edu.unaj.amoblador3d.model.FSHFProject;
import pe.edu.unaj.amoblador3d.model.FSHFRoomDims;
import pe.edu.unaj.amoblador3d.model.FSHFWallSide;
import pe.edu.unaj.amoblador3d.util.FSHFOutcome;

/** Tipos de puerta y escaleras. */
class FSHFDoorsAndStairsTest {
    private FSHFDesign design;

    @BeforeEach
    void setUp() {
        design = new FSHFDesign(new FSHFCatalog());
        design.load(new FSHFProject());
    }

    @Test
    void cadaTipoDePuertaSeAgregaConSusMedidas() {
        for (FSHFOpeningType t : FSHFOpeningType.doors()) {
            design.load(new FSHFProject());
            FSHFOutcome<FSHFOpening> out = design.addOpening(t);
            assertNull(out.error(), t.label());
            FSHFOpening o = out.value();
            assertEquals(t, o.type);
            assertEquals(t.defaultWidth(), o.width, 1e-9);
            assertEquals(0, o.sill, 1e-9);
            assertEquals(FSHFWallSide.SUR, o.wall, "las puertas empiezan por el sur");
        }
    }

    @Test
    void laPuertaPrincipalVaEnLaParedPedida() {
        FSHFOpening o = design.addOpening(FSHFOpeningType.ENTRY, List.of(FSHFWallSide.OESTE)).value();
        assertEquals(FSHFWallSide.OESTE, o.wall);
        // Un portón de 2,6 m no cabe en una pared de 2 m: avisa en vez de ponerlo en otra.
        design.load(projectOf(new FSHFRoomDims(4, 2, 2.5, 0.15)));
        FSHFOutcome<FSHFOpening> out = design.addOpening(FSHFOpeningType.GARAGE, List.of(FSHFWallSide.ESTE));
        assertNotNull(out.error());
        assertTrue(out.error().contains("esa pared"), out.error());
    }

    @Test
    void enUnaParedBajaLaPuertaSeAchica() {
        design.load(projectOf(new FSHFRoomDims(4, 3.5, 2.1, 0.15)));
        FSHFOpening o = design.addOpening(FSHFOpeningType.ENTRY).value();
        assertEquals(2.05, o.height, 1e-9);
    }

    @Test
    void elTipoDePuertaSeCambiaDesdeElPanel() {
        FSHFOpening door = design.addOpening(FSHFOpeningType.DOOR).value();
        FSHFEditable e = FSHFEditors.of(design, door, f -> null);
        assertEquals("door", e.get("type"));
        assertNull(e.set("type", "double"));
        assertEquals(FSHFOpeningType.DOUBLE, door.type);
        assertEquals(FSHFOpeningType.DOUBLE.defaultWidth(), door.width, 1e-9);
        assertNull(e.set("type", "passage"));
        assertEquals(FSHFOpeningType.PASSAGE, door.type);
        // Una ventana no ofrece tipos de puerta.
        FSHFOpening window = design.addOpening(FSHFOpeningType.WINDOW).value();
        assertTrue(FSHFEditors.of(design, window, f -> null).fields().stream().noneMatch(f -> f.key().equals("type")));
    }

    @Test
    void lasEscalerasLleganAlTecho() {
        design.load(projectOf(new FSHFRoomDims(5, 5, 2.7, 0.15)));
        for (String type : List.of("escalera", "escalera-l", "escalera-caracol")) {
            FSHFFurniture f = design.addFurniture(type).value();
            assertNotNull(f, type);
            assertEquals(2.7, f.height, 1e-9, type);
        }
        // La baranda no es escalera: conserva su alto.
        assertEquals(0.9, design.addFurniture("baranda").value().height, 1e-9);
        assertTrue(FSHFCatalog.isStair("escalera-l"));
    }

    private static FSHFProject projectOf(FSHFRoomDims dims) {
        FSHFProject p = new FSHFProject();
        p.room = dims;
        return p;
    }
}
