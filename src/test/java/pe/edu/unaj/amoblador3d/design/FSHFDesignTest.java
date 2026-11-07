package pe.edu.unaj.amoblador3d.design;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalog;
import pe.edu.unaj.amoblador3d.model.FSHFFootprint;
import pe.edu.unaj.amoblador3d.model.FSHFFurniture;
import pe.edu.unaj.amoblador3d.model.FSHFLight;
import pe.edu.unaj.amoblador3d.model.FSHFLightType;
import pe.edu.unaj.amoblador3d.model.FSHFOpening;
import pe.edu.unaj.amoblador3d.model.FSHFOpeningType;
import pe.edu.unaj.amoblador3d.model.FSHFRoomDims;
import pe.edu.unaj.amoblador3d.model.FSHFSurface;
import pe.edu.unaj.amoblador3d.model.FSHFSurfaceMaterial;
import pe.edu.unaj.amoblador3d.model.FSHFWallSide;
import pe.edu.unaj.amoblador3d.util.FSHFOutcome;

class FSHFDesignTest {
    private FSHFDesign design;
    private final List<String> warnings = new ArrayList<>();
    private final List<Set<FSHFDesign.Change>> changes = new ArrayList<>();

    @BeforeEach
    void setUp() {
        design = new FSHFDesign(new FSHFCatalog());
        design.setWarningHandler(warnings::add);
        design.loadDefault();
        design.addListener(changes::add);
    }

    private FSHFEditable editor(Object entity) {
        return FSHFEditors.of(design, entity, f -> null);
    }

    @Test
    void habitacionPorDefectoTieneVentanaPuertaBombillaYSol() {
        assertEquals(1, design.windows().size());
        assertEquals(2, design.openings().size());
        assertEquals(List.of(FSHFLightType.POINT, FSHFLightType.SUN), design.lights().stream().map(l -> l.type).toList());
    }

    @Test
    void losMueblesNuevosNoChocanEntreSiNiConLaPuerta() {
        for (String type : List.of("cama", "mesa", "silla", "closet", "escritorio")) {
            FSHFOutcome<FSHFFurniture> out = design.addFurniture(type);
            assertFalse(out.failed(), type + ": " + out.error());
        }
        List<FSHFFurniture> all = design.furniture();
        for (FSHFFurniture f : all) {
            assertNull(design.checkFurniture(f, f), design.label(f));
            assertTrue(FSHFFootprint.of(f).insideRoom(design.room().width(), design.room().length()));
        }
    }

    @Test
    void siDerechoNoCabeSePruebaGirado() {
        design.setRoom(new FSHFRoomDims(1, 3, 2.5, 0.15));
        FSHFOutcome<FSHFFurniture> table = design.addFurniture("mesa");
        assertFalse(table.failed(), table.error());
        assertEquals(90, table.value().rotation, 1e-9);
        assertNull(design.checkFurniture(table.value(), table.value()));
    }

    @Test
    void siNoCabeElMuebleSeExplicaPorQue() {
        design.setRoom(new FSHFRoomDims(1.2, 1.2, 2.5, 0.15));
        FSHFOutcome<FSHFFurniture> out = design.addFurniture("cama");
        assertTrue(out.failed());
        assertTrue(out.error().contains("No hay espacio"), out.error());
    }

    @Test
    void arrastrarContraLaParedDeslizaElMueble() {
        FSHFFurniture f = design.addFurniture("mesa").value();

        design.moveFurniture(f, -5, f.z);
        assertEquals(f.width / 2, f.x, 1e-9);
    }

    @Test
    void unaPuertaNoPuedeQuedarBloqueadaPorUnMueble() {
        FSHFOpening door = design.openings().stream().filter(o -> o.type == FSHFOpeningType.DOOR).findFirst().orElseThrow();
        FSHFFootprint zone = design.doorZone(door);
        FSHFFurniture f = design.addFurniture("silla").value();
        String err = editor(f).set("x", zone.x());
        if (err == null) err = editor(f).set("z", zone.z());
        assertNotNull(err, "la silla no debería poder ponerse en el paso de la puerta");
    }

    @Test
    void alAchicarLaHabitacionLasAberturasQueNoCabenSeEliminanConAviso() {
        FSHFOpening window = design.windows().get(0);
        editor(window).set("width", 3.5);
        List<String> w = design.setRoom(new FSHFRoomDims(2, 2, 2.5, 0.15));
        assertTrue(design.openings().stream().allMatch(o -> o.width <= design.wallLength(o.wall) - 0.1 + 1e-9));
        for (FSHFOpening o : design.openings()) assertNull(design.validateOpening(o, o, false), design.label(o));
        assertNotNull(w);
    }

    @Test
    void sinVentanasElSolSeElimina() {
        design.remove(design.windows().get(0));
        assertTrue(design.lights().stream().noneMatch(l -> l.type == FSHFLightType.SUN));
        assertFalse(warnings.isEmpty());
        assertTrue(design.addLight(FSHFLightType.SUN).failed());
    }

    @Test
    void lasAberturasNoSeSuperponen() {
        FSHFOpening window = design.windows().get(0);
        FSHFOpening second = design.addOpening(FSHFOpeningType.WINDOW).value();

        if (second.wall == window.wall) {
            assertNotNull(editor(second).set("offset", window.offset));
        } else {
            assertNull(editor(second).set("wall", window.wall.id()));
            assertTrue(second.offset >= window.offset + window.width + FSHFDesign.MARGIN - 1e-9
                    || second.offset + second.width <= window.offset - FSHFDesign.MARGIN + 1e-9);
        }
    }

    @Test
    void elPisoYLasParedesSonSeleccionablesPeroNoSeEliminan() {
        FSHFSurface floor = design.floor();
        assertTrue(design.entities().contains(floor));
        assertEquals(floor, design.findById("piso"));
        assertEquals(design.wall(FSHFWallSide.ESTE), design.findById("pared-este"));
        design.remove(floor);
        assertTrue(design.entities().contains(floor));
    }

    @Test
    void cambiarMaterialTomaSuColorYAvisaALaVista() {
        FSHFSurface norte = design.wall(FSHFWallSide.NORTE);
        assertNull(editor(norte).set("material", "ladrillo"));
        assertEquals(FSHFSurfaceMaterial.LADRILLO, norte.material);
        assertEquals(FSHFSurfaceMaterial.LADRILLO.defaultColor(), norte.color);
        assertTrue(changes.get(changes.size() - 1).contains(FSHFDesign.Change.SURFACES));
        assertNull(editor(norte).set("color", "#123456"));
        assertEquals("#123456", norte.color);
    }

    @Test
    void laAlfombraNoSePuedePonerEnUnaPared() {
        assertNotNull(editor(design.wall(FSHFWallSide.SUR)).set("material", "alfombra"));
        assertEquals(FSHFSurface.DEFAULT_WALL, design.wall(FSHFWallSide.SUR).material);
        assertNull(editor(design.floor()).set("material", "alfombra"));
    }

    @Test
    void aplicarATodasLasParedesCopiaMaterialYColor() {
        FSHFSurface oeste = design.wall(FSHFWallSide.OESTE);
        editor(oeste).set("material", "papel-tapiz");
        editor(oeste).set("color", "#aabbcc");
        FSHFEditable.Action all = editor(oeste).actions().get(0);
        assertNull(all.run().get());
        for (FSHFWallSide side : FSHFWallSide.values()) {
            assertEquals(FSHFSurfaceMaterial.PAPEL_TAPIZ, design.wall(side).material);
            assertEquals("#aabbcc", design.wall(side).color);
        }
        assertEquals(FSHFSurface.DEFAULT_FLOOR, design.floor().material, "el piso no cambia");
    }

    @Test
    void losCamposNumericosRespetanSusLimites() {
        FSHFLight bulb = design.lights().get(0);
        assertNotNull(editor(bulb).set("y", 50.0));
        assertNotNull(editor(bulb).set("intensity", -1.0));
        assertNull(editor(bulb).set("intensity", 10.0));
        assertEquals(10.0, bulb.intensity);
    }
}
