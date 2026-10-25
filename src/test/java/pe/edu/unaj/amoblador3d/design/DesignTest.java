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
import pe.edu.unaj.amoblador3d.catalog.Catalog;
import pe.edu.unaj.amoblador3d.model.Footprint;
import pe.edu.unaj.amoblador3d.model.Furniture;
import pe.edu.unaj.amoblador3d.model.Light;
import pe.edu.unaj.amoblador3d.model.LightType;
import pe.edu.unaj.amoblador3d.model.Opening;
import pe.edu.unaj.amoblador3d.model.OpeningType;
import pe.edu.unaj.amoblador3d.model.RoomDims;
import pe.edu.unaj.amoblador3d.model.Surface;
import pe.edu.unaj.amoblador3d.model.SurfaceMaterial;
import pe.edu.unaj.amoblador3d.model.WallSide;
import pe.edu.unaj.amoblador3d.util.Outcome;

class DesignTest {
    private Design design;
    private final List<String> warnings = new ArrayList<>();
    private final List<Set<Design.Change>> changes = new ArrayList<>();

    @BeforeEach
    void setUp() {
        design = new Design(new Catalog());
        design.setWarningHandler(warnings::add);
        design.loadDefault();
        design.addListener(changes::add);
    }

    private Editable editor(Object entity) {
        return Editors.of(design, entity, f -> null);
    }

    @Test
    void habitacionPorDefectoTieneVentanaPuertaBombillaYSol() {
        assertEquals(1, design.windows().size());
        assertEquals(2, design.openings().size());
        assertEquals(List.of(LightType.POINT, LightType.SUN), design.lights().stream().map(l -> l.type).toList());
    }

    @Test
    void losMueblesNuevosNoChocanEntreSiNiConLaPuerta() {
        for (String type : List.of("cama", "mesa", "silla", "closet", "escritorio")) {
            Outcome<Furniture> out = design.addFurniture(type);
            assertFalse(out.failed(), type + ": " + out.error());
        }
        List<Furniture> all = design.furniture();
        for (Furniture f : all) {
            assertNull(design.checkFurniture(f, f), design.label(f));
            assertTrue(Footprint.of(f).insideRoom(design.room().width(), design.room().length()));
        }
    }

    @Test
    void siDerechoNoCabeSePruebaGirado() {
        // Habitación de 1 m de ancho: la mesa (1.2 x 0.8 m) solo entra girada 90°.
        design.setRoom(new RoomDims(1, 3, 2.5, 0.15));
        Outcome<Furniture> table = design.addFurniture("mesa");
        assertFalse(table.failed(), table.error());
        assertEquals(90, table.value().rotation, 1e-9);
        assertNull(design.checkFurniture(table.value(), table.value()));
    }

    @Test
    void siNoCabeElMuebleSeExplicaPorQue() {
        design.setRoom(new RoomDims(1.2, 1.2, 2.5, 0.15));
        Outcome<Furniture> out = design.addFurniture("cama");
        assertTrue(out.failed());
        assertTrue(out.error().contains("No hay espacio"), out.error());
    }

    @Test
    void arrastrarContraLaParedDeslizaElMueble() {
        Furniture f = design.addFurniture("mesa").value();
        // Pedir una posición fuera de la habitación lo deja pegado a la pared, no fuera.
        design.moveFurniture(f, -5, f.z);
        assertEquals(f.width / 2, f.x, 1e-9);
    }

    @Test
    void unaPuertaNoPuedeQuedarBloqueadaPorUnMueble() {
        Opening door = design.openings().stream().filter(o -> o.type == OpeningType.DOOR).findFirst().orElseThrow();
        Footprint zone = design.doorZone(door);
        Furniture f = design.addFurniture("silla").value();
        String err = editor(f).set("x", zone.x());
        if (err == null) err = editor(f).set("z", zone.z());
        assertNotNull(err, "la silla no debería poder ponerse en el paso de la puerta");
    }

    @Test
    void alAchicarLaHabitacionLasAberturasQueNoCabenSeEliminanConAviso() {
        Opening window = design.windows().get(0);
        editor(window).set("width", 3.5);
        List<String> w = design.setRoom(new RoomDims(2, 2, 2.5, 0.15));
        assertTrue(design.openings().stream().allMatch(o -> o.width <= design.wallLength(o.wall) - 0.1 + 1e-9));
        for (Opening o : design.openings()) assertNull(design.validateOpening(o, o, false), design.label(o));
        assertNotNull(w);
    }

    @Test
    void sinVentanasElSolSeElimina() {
        design.remove(design.windows().get(0));
        assertTrue(design.lights().stream().noneMatch(l -> l.type == LightType.SUN));
        assertFalse(warnings.isEmpty());
        assertTrue(design.addLight(LightType.SUN).failed());
    }

    @Test
    void lasAberturasNoSeSuperponen() {
        Opening window = design.windows().get(0);
        Opening second = design.addOpening(OpeningType.WINDOW).value();
        // Mover la segunda encima de la primera debe fallar.
        if (second.wall == window.wall) {
            assertNotNull(editor(second).set("offset", window.offset));
        } else {
            assertNull(editor(second).set("wall", window.wall.id()));
            assertTrue(second.offset >= window.offset + window.width + Design.MARGIN - 1e-9
                    || second.offset + second.width <= window.offset - Design.MARGIN + 1e-9);
        }
    }

    @Test
    void elPisoYLasParedesSonSeleccionablesPeroNoSeEliminan() {
        Surface floor = design.floor();
        assertTrue(design.entities().contains(floor));
        assertEquals(floor, design.findById("piso"));
        assertEquals(design.wall(WallSide.ESTE), design.findById("pared-este"));
        design.remove(floor);
        assertTrue(design.entities().contains(floor));
    }

    @Test
    void cambiarMaterialTomaSuColorYAvisaALaVista() {
        Surface norte = design.wall(WallSide.NORTE);
        assertNull(editor(norte).set("material", "ladrillo"));
        assertEquals(SurfaceMaterial.LADRILLO, norte.material);
        assertEquals(SurfaceMaterial.LADRILLO.defaultColor(), norte.color);
        assertTrue(changes.get(changes.size() - 1).contains(Design.Change.SURFACES));
        assertNull(editor(norte).set("color", "#123456"));
        assertEquals("#123456", norte.color);
    }

    @Test
    void laAlfombraNoSePuedePonerEnUnaPared() {
        assertNotNull(editor(design.wall(WallSide.SUR)).set("material", "alfombra"));
        assertEquals(Surface.DEFAULT_WALL, design.wall(WallSide.SUR).material);
        assertNull(editor(design.floor()).set("material", "alfombra"));
    }

    @Test
    void aplicarATodasLasParedesCopiaMaterialYColor() {
        Surface oeste = design.wall(WallSide.OESTE);
        editor(oeste).set("material", "papel-tapiz");
        editor(oeste).set("color", "#aabbcc");
        Editable.Action all = editor(oeste).actions().get(0);
        assertNull(all.run().get());
        for (WallSide side : WallSide.values()) {
            assertEquals(SurfaceMaterial.PAPEL_TAPIZ, design.wall(side).material);
            assertEquals("#aabbcc", design.wall(side).color);
        }
        assertEquals(Surface.DEFAULT_FLOOR, design.floor().material, "el piso no cambia");
    }

    @Test
    void losCamposNumericosRespetanSusLimites() {
        Light bulb = design.lights().get(0);
        assertNotNull(editor(bulb).set("y", 50.0));
        assertNotNull(editor(bulb).set("intensity", -1.0));
        assertNull(editor(bulb).set("intensity", 10.0));
        assertEquals(10.0, bulb.intensity);
    }
}
