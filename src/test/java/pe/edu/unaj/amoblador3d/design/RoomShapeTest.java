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
import pe.edu.unaj.amoblador3d.catalog.Catalog;
import pe.edu.unaj.amoblador3d.estimate.Estimate;
import pe.edu.unaj.amoblador3d.estimate.PriceList;
import pe.edu.unaj.amoblador3d.estimate.Quantities;
import pe.edu.unaj.amoblador3d.model.Column;
import pe.edu.unaj.amoblador3d.model.Footprint;
import pe.edu.unaj.amoblador3d.model.Furniture;
import pe.edu.unaj.amoblador3d.model.Opening;
import pe.edu.unaj.amoblador3d.model.OpeningType;
import pe.edu.unaj.amoblador3d.model.Project;
import pe.edu.unaj.amoblador3d.model.RoomDims;
import pe.edu.unaj.amoblador3d.model.SurfaceMaterial;
import pe.edu.unaj.amoblador3d.model.WallSide;
import pe.edu.unaj.amoblador3d.persistence.Database;
import pe.edu.unaj.amoblador3d.persistence.ProjectJson;
import pe.edu.unaj.amoblador3d.persistence.ProjectRepository;
import pe.edu.unaj.amoblador3d.util.Outcome;

/** Columnas, muros internos, habitación en L, techo y zócalo. */
class RoomShapeTest {
    private Design design;

    /** 5 x 4 x 2.5 m, ventana al norte (1.5 a 2.7 m) y puerta al sur (0.5 a 1.4 m). */
    @BeforeEach
    void setUp() {
        design = new Design(new Catalog());
        Project p = new Project();
        p.room = new RoomDims(5, 4, 2.5, 0.15);
        p.openings.add(new Opening("window-1", OpeningType.WINDOW, WallSide.NORTE, 1.5, 1.2, 1, 0.9));
        p.openings.add(new Opening("door-1", OpeningType.DOOR, WallSide.SUR, 0.5, 0.9, 2.1, 0));
        design.load(p);
    }

    private static Column moved(Column c, double x, double z) {
        Column k = c.copy();
        k.x = x;
        k.z = z;
        return k;
    }

    @Test
    void losMueblesNoAtraviesanColumnas() {
        Column col = design.addColumn(Column.Kind.COLUMN).value();
        assertNull(design.changeColumn(col, moved(col, 2.5, 2)));
        Furniture table = design.addFurniture("mesa").value();
        assertFalse(Footprint.of(table).overlaps(col.footprint()), "se agrega lejos de la columna");
        Furniture over = table.copy();
        over.x = 2.5;
        over.z = 2;
        assertNotNull(design.checkFurniture(table, over));
        assertTrue(design.checkFurniture(table, over).contains("columna"), design.checkFurniture(table, over));
        // Y una columna no se puede poner encima de un mueble.
        assertNotNull(design.changeColumn(col, moved(col, table.x, table.z)));
    }

    @Test
    void unaColumnaNoTapaVentanasNiPuertas() {
        Column col = design.addColumn(Column.Kind.COLUMN).value();
        assertTrue(design.changeColumn(col, moved(col, 2, 0.15)).contains("Ventana"), "pegada a la pared norte, frente a la ventana");
        assertTrue(design.changeColumn(col, moved(col, 0.95, 3.6)).contains("Puerta"), "en el paso de la puerta");
        assertNull(design.changeColumn(col, moved(col, 4.85, 0.15)), "en la esquina noreste no tapa nada");
        // Tampoco se puede mover la ventana detrás de la columna.
        Opening window = design.openings().get(0);
        String err = Editors.of(design, window, f -> null).set("offset", 3.7);
        assertNotNull(err);
        assertTrue(err.contains("tapada"), err);
    }

    @Test
    void lasColumnasNoTapanLasLuces() {
        pe.edu.unaj.amoblador3d.model.Light bulb = design.addLight(pe.edu.unaj.amoblador3d.model.LightType.POINT).value();
        Column wall = design.addColumn(Column.Kind.WALL).value(); // se busca un lugar sin la bombilla del centro
        assertFalse(bulb.x > wall.minX() && bulb.x < wall.maxX() && bulb.z > wall.minZ() && bulb.z < wall.maxZ());
        // Llevarla dentro del muro: primero a su misma Z (fuera en X) y luego a su X.
        Editable editor = Editors.of(design, bulb, f -> null);
        assertNull(editor.set("x", wall.maxX() + 0.3 < 4.95 ? wall.maxX() + 0.3 : wall.minX() - 0.3));
        assertNull(editor.set("z", wall.z));
        String err = editor.set("x", wall.x);
        assertNotNull(err);
        assertTrue(err.contains("dentro"), err);
    }

    @Test
    void habitacionEnLDescuentaPisoYConservaElPerimetro() {
        Quantities before = Quantities.of(design.snapshot());
        Outcome<Column> out = design.addNotch(Column.Corner.SURESTE, 2, 1.5);
        assertFalse(out.failed(), out.error());
        Column notch = out.value();
        assertEquals(4, notch.x, 1e-9);
        assertEquals(3.25, notch.z, 1e-9);
        Quantities q = Quantities.of(design.snapshot());
        assertEquals(20 - 3, q.floorArea(), 1e-9);
        assertEquals(before.perimeter(), q.perimeter(), 1e-9, "una L tiene el mismo perímetro que el rectángulo");
        // La pared este pierde 1.5 m y la sur 2 m; las dos caras del recorte suman lo mismo.
        assertEquals(before.wallNet().get(WallSide.ESTE) - 1.5 * 2.5, q.wallNet().get(WallSide.ESTE), 1e-9);
        assertEquals(before.wallNet().get(WallSide.SUR) - 2 * 2.5, q.wallNet().get(WallSide.SUR), 1e-9);
        assertEquals((2 + 1.5) * 2.5, q.columnFacesTotal(), 1e-9);
        assertEquals(before.wallNetTotal() + before.columnFacesTotal(), q.wallNetTotal() + q.columnFacesTotal(), 1e-9);
        // Al agrandar la habitación el recorte sigue en su esquina.
        design.setRoom(new RoomDims(6, 5, 2.5, 0.15));
        assertEquals(5, notch.x, 1e-9);
        assertEquals(4.25, notch.z, 1e-9);
        // Un recorte que no deja habitación no se acepta.
        assertTrue(design.addNotch(Column.Corner.NOROESTE, 5.8, 1).failed());
    }

    @Test
    void unMuroInternoSueltoSumaSuContornoAlZocaloYSusCarasALaPintura() {
        Column wall = design.addColumn(Column.Kind.WALL).value(); // 1.5 x 0.12
        assertNull(design.changeColumn(wall, moved(wall, 2.5, 2)));
        Quantities q = Quantities.of(design.snapshot());
        double outline = 2 * (1.5 + 0.12);
        assertEquals(outline * 2.5, q.columnFacesTotal(), 1e-9);
        assertEquals(18 + outline - 0.9, q.baseboard(), 1e-9);
        assertEquals(20 - 1.5 * 0.12, q.floorArea(), 1e-9);
        // Pegado a la pared oeste: esa cara ya no se ve ni lleva zócalo.
        Column turned = wall.copy();
        turned.width = 0.12;
        turned.depth = 1.5;
        turned.x = 0.06;
        turned.z = 2;
        assertNull(design.changeColumn(wall, turned));
        q = Quantities.of(design.snapshot());
        assertEquals(18 + 2 * 0.12 - 0.9, q.baseboard(), 1e-9);
    }

    @Test
    void elTechoYElZocaloEntranEnElPresupuesto() {
        assertNotNull(Editors.of(design, design.ceiling(), f -> null).set("material", SurfaceMaterial.LADRILLO.id()),
                "el ladrillo no va en el techo");
        assertNull(Editors.of(design, design.ceiling(), f -> null).set("material", SurfaceMaterial.MADERA.id()));
        Estimate e = Estimate.of(design.snapshot(), new Catalog(), new PriceList());
        assertTrue(e.lines().stream().anyMatch(l -> l.detail().startsWith("Techo") && l.priceKey().equals(
                PriceList.Key.material(SurfaceMaterial.MADERA))));
        assertTrue(e.lines().stream().anyMatch(l -> l.priceKey().equals(PriceList.BASEBOARD)));
        // Sin zócalo no se cobra zócalo.
        assertNull(Editors.of(design, design.baseboard(), f -> null).set("height", 0.0));
        e = Estimate.of(design.snapshot(), new Catalog(), new PriceList());
        assertTrue(e.lines().stream().noneMatch(l -> l.priceKey().equals(PriceList.BASEBOARD)));
    }

    @Test
    void todoSeGuardaYSeLeeIgual(@TempDir Path dir) throws Exception {
        String plain = ProjectJson.write(design.snapshot());
        assertTrue(plain.contains("\"version\": 1"), "sin columnas sigue siendo versión 1");
        design.addColumn(Column.Kind.COLUMN);
        design.addNotch(Column.Corner.NOROESTE, 1, 1);
        Editors.of(design, design.ceiling(), f -> null).set("color", "#123456");
        Editors.of(design, design.baseboard(), f -> null).set("height", 0.12);
        Project p = design.snapshot();
        String json = ProjectJson.write(p);
        assertTrue(json.contains("\"version\": 2"));
        assertEquals(json, ProjectJson.write(ProjectJson.read(json)));
        try (Database db = new Database(dir.resolve("forma.db"))) {
            ProjectRepository repo = new ProjectRepository(db);
            long id = repo.save(null, "En L", p, null);
            assertEquals(json, ProjectJson.write(repo.load(id)));
        }
        List<String> warnings = new Design(new Catalog()).load(ProjectJson.read(json));
        assertTrue(warnings.isEmpty(), warnings.toString());
    }
}
