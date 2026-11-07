package pe.edu.unaj.amoblador3d.design;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalog;
import pe.edu.unaj.amoblador3d.estimate.FSHFEstimate;
import pe.edu.unaj.amoblador3d.estimate.FSHFPriceList;
import pe.edu.unaj.amoblador3d.estimate.FSHFQuantities;
import pe.edu.unaj.amoblador3d.model.FSHFColumn;
import pe.edu.unaj.amoblador3d.model.FSHFFootprint;
import pe.edu.unaj.amoblador3d.model.FSHFFurniture;
import pe.edu.unaj.amoblador3d.model.FSHFOpening;
import pe.edu.unaj.amoblador3d.model.FSHFOpeningType;
import pe.edu.unaj.amoblador3d.model.FSHFProject;
import pe.edu.unaj.amoblador3d.model.FSHFRoomDims;
import pe.edu.unaj.amoblador3d.model.FSHFSurfaceMaterial;
import pe.edu.unaj.amoblador3d.model.FSHFWallSide;
import pe.edu.unaj.amoblador3d.persistence.FSHFDatabase;
import pe.edu.unaj.amoblador3d.persistence.FSHFProjectJson;
import pe.edu.unaj.amoblador3d.persistence.FSHFProjectRepository;
import pe.edu.unaj.amoblador3d.util.FSHFOutcome;

/** Columnas, muros internos, habitación en L, techo y zócalo. */
class FSHFRoomShapeTest {
    private FSHFDesign design;

    /** 5 x 4 x 2.5 m, ventana al norte (1.5 a 2.7 m) y puerta al sur (0.5 a 1.4 m). */
    @BeforeEach
    void setUp() {
        design = new FSHFDesign(new FSHFCatalog());
        FSHFProject p = new FSHFProject();
        p.room = new FSHFRoomDims(5, 4, 2.5, 0.15);
        p.openings.add(new FSHFOpening("window-1", FSHFOpeningType.WINDOW, FSHFWallSide.NORTE, 1.5, 1.2, 1, 0.9));
        p.openings.add(new FSHFOpening("door-1", FSHFOpeningType.DOOR, FSHFWallSide.SUR, 0.5, 0.9, 2.1, 0));
        design.load(p);
    }

    private static FSHFColumn moved(FSHFColumn c, double x, double z) {
        FSHFColumn k = c.copy();
        k.x = x;
        k.z = z;
        return k;
    }

    @Test
    void losMueblesNoAtraviesanColumnas() {
        FSHFColumn col = design.addColumn(FSHFColumn.Kind.COLUMN).value();
        assertNull(design.changeColumn(col, moved(col, 2.5, 2)));
        FSHFFurniture table = design.addFurniture("mesa").value();
        assertFalse(FSHFFootprint.of(table).overlaps(col.footprint()), "se agrega lejos de la columna");
        FSHFFurniture over = table.copy();
        over.x = 2.5;
        over.z = 2;
        assertNotNull(design.checkFurniture(table, over));
        assertTrue(design.checkFurniture(table, over).contains("columna"), design.checkFurniture(table, over));
        // Y una columna no se puede poner encima de un mueble.
        assertNotNull(design.changeColumn(col, moved(col, table.x, table.z)));
    }

    @Test
    void unaColumnaNoTapaVentanasNiPuertas() {
        FSHFColumn col = design.addColumn(FSHFColumn.Kind.COLUMN).value();
        assertTrue(design.changeColumn(col, moved(col, 2, 0.15)).contains("Ventana"), "pegada a la pared norte, frente a la ventana");
        assertTrue(design.changeColumn(col, moved(col, 0.95, 3.6)).contains("Puerta"), "en el paso de la puerta");
        assertNull(design.changeColumn(col, moved(col, 4.85, 0.15)), "en la esquina noreste no tapa nada");
        // Tampoco se puede mover la ventana detrás de la columna.
        FSHFOpening window = design.openings().get(0);
        String err = FSHFEditors.of(design, window, f -> null).set("offset", 3.7);
        assertNotNull(err);
        assertTrue(err.contains("tapada"), err);
    }

    @Test
    void lasColumnasNoTapanLasLuces() {
        pe.edu.unaj.amoblador3d.model.FSHFLight bulb = design.addLight(pe.edu.unaj.amoblador3d.model.FSHFLightType.POINT).value();
        FSHFColumn wall = design.addColumn(FSHFColumn.Kind.WALL).value(); // se busca un lugar sin la bombilla del centro
        assertFalse(bulb.x > wall.minX() && bulb.x < wall.maxX() && bulb.z > wall.minZ() && bulb.z < wall.maxZ());
        // Llevarla dentro del muro: primero a su misma Z (fuera en X) y luego a su X.
        FSHFEditable editor = FSHFEditors.of(design, bulb, f -> null);
        assertNull(editor.set("x", wall.maxX() + 0.3 < 4.95 ? wall.maxX() + 0.3 : wall.minX() - 0.3));
        assertNull(editor.set("z", wall.z));
        String err = editor.set("x", wall.x);
        assertNotNull(err);
        assertTrue(err.contains("dentro"), err);
    }

    @Test
    void habitacionEnLDescuentaPisoYConservaElPerimetro() {
        FSHFQuantities before = FSHFQuantities.of(design.snapshot());
        FSHFOutcome<FSHFColumn> out = design.addNotch(FSHFColumn.Corner.SURESTE, 2, 1.5);
        assertFalse(out.failed(), out.error());
        FSHFColumn notch = out.value();
        assertEquals(4, notch.x, 1e-9);
        assertEquals(3.25, notch.z, 1e-9);
        FSHFQuantities q = FSHFQuantities.of(design.snapshot());
        assertEquals(20 - 3, q.floorArea(), 1e-9);
        assertEquals(before.perimeter(), q.perimeter(), 1e-9, "una L tiene el mismo perímetro que el rectángulo");
        // La pared este pierde 1.5 m y la sur 2 m; las dos caras del recorte suman lo mismo.
        assertEquals(before.wallNet().get(FSHFWallSide.ESTE) - 1.5 * 2.5, q.wallNet().get(FSHFWallSide.ESTE), 1e-9);
        assertEquals(before.wallNet().get(FSHFWallSide.SUR) - 2 * 2.5, q.wallNet().get(FSHFWallSide.SUR), 1e-9);
        assertEquals((2 + 1.5) * 2.5, q.columnFacesTotal(), 1e-9);
        assertEquals(before.wallNetTotal() + before.columnFacesTotal(), q.wallNetTotal() + q.columnFacesTotal(), 1e-9);
        // Al agrandar la habitación el recorte sigue en su esquina.
        design.setRoom(new FSHFRoomDims(6, 5, 2.5, 0.15));
        assertEquals(5, notch.x, 1e-9);
        assertEquals(4.25, notch.z, 1e-9);
        // Un recorte que no deja habitación no se acepta.
        assertTrue(design.addNotch(FSHFColumn.Corner.NOROESTE, 5.8, 1).failed());
    }

    @Test
    void unMuroInternoSueltoSumaSuContornoAlZocaloYSusCarasALaPintura() {
        FSHFColumn wall = design.addColumn(FSHFColumn.Kind.WALL).value(); // 1.5 x 0.12
        assertNull(design.changeColumn(wall, moved(wall, 2.5, 2)));
        FSHFQuantities q = FSHFQuantities.of(design.snapshot());
        double outline = 2 * (1.5 + 0.12);
        assertEquals(outline * 2.5, q.columnFacesTotal(), 1e-9);
        assertEquals(18 + outline - 0.9, q.baseboard(), 1e-9);
        assertEquals(20 - 1.5 * 0.12, q.floorArea(), 1e-9);
        // Pegado a la pared oeste: esa cara ya no se ve ni lleva zócalo.
        FSHFColumn turned = wall.copy();
        turned.width = 0.12;
        turned.depth = 1.5;
        turned.x = 0.06;
        turned.z = 2;
        assertNull(design.changeColumn(wall, turned));
        q = FSHFQuantities.of(design.snapshot());
        assertEquals(18 + 2 * 0.12 - 0.9, q.baseboard(), 1e-9);
    }

    @Test
    void elTechoYElZocaloEntranEnElPresupuesto() {
        assertNotNull(FSHFEditors.of(design, design.ceiling(), f -> null).set("material", FSHFSurfaceMaterial.LADRILLO.id()),
                "el ladrillo no va en el techo");
        assertNull(FSHFEditors.of(design, design.ceiling(), f -> null).set("material", FSHFSurfaceMaterial.MADERA.id()));
        FSHFEstimate e = FSHFEstimate.of(design.snapshot(), new FSHFCatalog(), new FSHFPriceList());
        assertTrue(e.lines().stream().anyMatch(l -> l.detail().startsWith("Techo") && l.priceKey().equals(
                FSHFPriceList.Key.material(FSHFSurfaceMaterial.MADERA))));
        assertTrue(e.lines().stream().anyMatch(l -> l.priceKey().equals(FSHFPriceList.BASEBOARD)));
        // Sin zócalo no se cobra zócalo.
        assertNull(FSHFEditors.of(design, design.baseboard(), f -> null).set("height", 0.0));
        e = FSHFEstimate.of(design.snapshot(), new FSHFCatalog(), new FSHFPriceList());
        assertTrue(e.lines().stream().noneMatch(l -> l.priceKey().equals(FSHFPriceList.BASEBOARD)));
    }

    @Test
    void todoSeGuardaYSeLeeIgual(@TempDir Path dir) throws Exception {
        String plain = FSHFProjectJson.write(design.snapshot());
        assertTrue(plain.contains("\"version\": 1"), "sin columnas sigue siendo versión 1");
        design.addColumn(FSHFColumn.Kind.COLUMN);
        design.addNotch(FSHFColumn.Corner.NOROESTE, 1, 1);
        FSHFEditors.of(design, design.ceiling(), f -> null).set("color", "#123456");
        FSHFEditors.of(design, design.baseboard(), f -> null).set("height", 0.12);
        FSHFProject p = design.snapshot();
        String json = FSHFProjectJson.write(p);
        assertTrue(json.contains("\"version\": 2"));
        assertEquals(json, FSHFProjectJson.write(FSHFProjectJson.read(json)));
        try (FSHFDatabase db = new FSHFDatabase(dir.resolve("forma.db"))) {
            FSHFProjectRepository repo = new FSHFProjectRepository(db);
            long id = repo.save(null, "En L", p, null);
            assertEquals(json, FSHFProjectJson.write(repo.load(id)));
        }
        List<String> warnings = new FSHFDesign(new FSHFCatalog()).load(FSHFProjectJson.read(json));
        assertTrue(warnings.isEmpty(), warnings.toString());
    }
}
