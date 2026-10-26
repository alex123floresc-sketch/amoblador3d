package pe.edu.unaj.amoblador3d.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pe.edu.unaj.amoblador3d.catalog.Catalog;
import pe.edu.unaj.amoblador3d.design.Design;
import pe.edu.unaj.amoblador3d.estimate.PriceList;
import pe.edu.unaj.amoblador3d.model.LightType;
import pe.edu.unaj.amoblador3d.model.Project;
import pe.edu.unaj.amoblador3d.model.Surface;
import pe.edu.unaj.amoblador3d.model.SurfaceMaterial;
import pe.edu.unaj.amoblador3d.model.WallSide;

class PersistenceTest {
    @TempDir
    Path dir;

    private static Project sample() {
        Design d = new Design(new Catalog());
        d.loadDefault();
        d.addFurniture("cama");
        Project p = d.snapshot();
        p.floor.material = SurfaceMaterial.MARMOL;
        p.floor.color = "#ffffff";
        p.walls.get(WallSide.NORTE).material = SurfaceMaterial.LADRILLO;
        p.walls.get(WallSide.NORTE).color = "#a4553a";
        return p;
    }

    @Test
    void jsonIdaYVueltaConservaTodo() throws Exception {
        Project p = sample();
        String json = ProjectJson.write(p);
        assertEquals(json, ProjectJson.write(ProjectJson.read(json)));
    }

    @Test
    void archivoAntiguoSinSuperficiesUsaLosAcabadosPorDefecto() throws Exception {
        String old = """
                {"app":"Amoblador3D","version":1,
                 "room":{"width":4,"length":3,"height":2.5,"wallThickness":0.15},
                 "openings":[],"lights":[],"furniture":[]}""";
        Project p = ProjectJson.read(old);
        assertEquals(Surface.DEFAULT_FLOOR, p.floor.material);
        assertEquals(Surface.DEFAULT_WALL, p.walls.get(WallSide.SUR).material);
    }

    @Test
    void superficiesDanadasSeCorrigenSinPerderElProyecto() throws Exception {
        String json = """
                {"app":"Amoblador3D","version":1,
                 "room":{"width":4,"length":3,"height":2.5,"wallThickness":0.15},
                 "surfaces":{"piso":{"material":"madera","color":"rojo"},
                             "pared-norte":{"material":"alfombra","color":"#112233"}}}""";
        Project p = ProjectJson.read(json);
        assertEquals(SurfaceMaterial.MADERA.defaultColor(), p.floor.color);
        assertEquals(Surface.DEFAULT_WALL, p.walls.get(WallSide.NORTE).material);
    }

    @Test
    void archivosNoValidosDanUnErrorClaro() {
        assertThrows(ProjectFormatException.class, () -> ProjectJson.read("no es json {"));
        assertThrows(ProjectFormatException.class, () -> ProjectJson.read("{\"app\":\"Otra\"}"));
        assertThrows(ProjectFormatException.class, () -> ProjectJson.read("""
                {"app":"Amoblador3D","room":{"width":4,"length":3,"height":2.5,"wallThickness":0.15},
                 "surfaces":{"piso":{"material":"lava","color":"#000000"}}}"""));
    }

    @Test
    void guardarYCargarEnSqlite() throws Exception {
        try (Database db = new Database(dir.resolve("a.db"))) {
            ProjectRepository repo = new ProjectRepository(db);
            Project p = sample();
            long id = repo.save(null, "Dormitorio", p, null);
            repo.save(id, "Dormitorio", p, null); // sobrescribir no duplica filas
            Project q = repo.load(id);
            assertEquals(ProjectJson.write(p), ProjectJson.write(q));
            assertEquals(1, repo.list().size());
            repo.delete(id);
            assertEquals(0, repo.list().size());
        }
    }

    @Test
    void unaBaseDeLaVersionAnteriorSeActualiza() throws Exception {
        Path file = dir.resolve("v1.db");
        // Base creada por la versión 1 (sin la tabla surface) con un proyecto.
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + file); Statement st = c.createStatement()) {
            st.execute("""
                    CREATE TABLE project (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE COLLATE NOCASE,
                      width REAL NOT NULL, length REAL NOT NULL, height REAL NOT NULL, wall_thickness REAL NOT NULL,
                      ambient REAL NOT NULL, created_at TEXT NOT NULL, updated_at TEXT NOT NULL, thumbnail BLOB)""");
            st.execute("CREATE TABLE opening (project_id INTEGER, position INTEGER, id TEXT, type TEXT, wall TEXT, wall_offset REAL, width REAL, height REAL, sill REAL)");
            st.execute("CREATE TABLE light (project_id INTEGER, position INTEGER, id TEXT, type TEXT, color TEXT, intensity REAL, cast_shadow INTEGER, x REAL, y REAL, z REAL, target_x REAL, target_z REAL, angle REAL, penumbra REAL, window_id TEXT, elevation REAL, deviation REAL)");
            st.execute("CREATE TABLE furniture (project_id INTEGER, position INTEGER, id TEXT, type TEXT, x REAL, z REAL, rotation REAL, width REAL, depth REAL, height REAL, color TEXT)");
            st.execute("CREATE TABLE catalog_model (id TEXT PRIMARY KEY, name TEXT, file TEXT, width REAL, depth REAL, height REAL, rotation REAL, created_at TEXT)");
            st.execute("INSERT INTO project VALUES (1, 'Viejo', 4, 3, 2.5, 0.15, 0.6, '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z', NULL)");
            st.execute("PRAGMA user_version = 1");
        }
        try (Database db = new Database(file)) {
            try (Statement st = db.connection().createStatement(); ResultSet rs = st.executeQuery("PRAGMA user_version")) {
                assertEquals(Database.SCHEMA_VERSION, rs.getInt(1));
            }
            Project p = new ProjectRepository(db).load(1);
            assertNotNull(p);
            assertEquals(Surface.DEFAULT_FLOOR, p.floor.material);
            // Y ya se puede guardar con superficies.
            p.floor.material = SurfaceMaterial.CERAMICO;
            new ProjectRepository(db).save(1L, "Viejo", p, null);
            assertEquals(SurfaceMaterial.CERAMICO, new ProjectRepository(db).load(1).floor.material);
        }
    }

    @Test
    void nombresRepetidosSeDetectanSinImportarMayusculas() throws Exception {
        try (Database db = new Database(dir.resolve("b.db"))) {
            ProjectRepository repo = new ProjectRepository(db);
            long id = repo.save(null, "Sala", new Project(), null);
            assertEquals(id, repo.findByName("SALA"));
            assertTrue(repo.findByName("Cocina") == null);
        }
    }

    @Test
    void elScriptSqliteDeEjemploLoAbreLaAplicacion() throws Exception {
        Path file = dir.resolve("script.db");
        String script = Files.readString(Path.of("database/amoblador3d_sqlite.sql"));
        String sql = String.join("\n", script.lines().filter(l -> !l.strip().startsWith("--")).toList());
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + file); Statement st = c.createStatement()) {
            for (String statement : sql.split(";")) if (!statement.isBlank()) st.execute(statement);
        }
        // La aplicación reconoce las tablas (no intenta crearlas de nuevo) y el proyecto es válido.
        try (Database db = new Database(file)) {
            Project p = new ProjectRepository(db).load(1);
            assertNotNull(p);
            assertEquals(SurfaceMaterial.LADRILLO, p.walls.get(WallSide.NORTE).material);
            assertEquals(4, p.furniture.size());
            assertTrue(p.furniture.get(0).locked, "la cama del ejemplo está bloqueada");
            assertEquals("window-1", p.lights.stream().filter(l -> l.type == LightType.SUN).findFirst().orElseThrow().windowId);
            Design design = new Design(new Catalog());
            assertTrue(design.load(p).isEmpty(), "el ejemplo no debería necesitar correcciones");
            assertEquals(70, new PriceRepository(db).load().get(PriceList.Key.material(SurfaceMaterial.PINTURA)), 1e-9);
        }
    }

    @Test
    void elBloqueoSeGuardaEnLaBaseYEnElArchivo() throws Exception {
        Project p = sample();
        p.furniture.get(0).locked = true;
        assertTrue(ProjectJson.read(ProjectJson.write(p)).furniture.get(0).locked);
        try (Database db = new Database(dir.resolve("bloqueo.db"))) {
            ProjectRepository repo = new ProjectRepository(db);
            long id = repo.save(null, "Con bloqueo", p, null);
            assertTrue(repo.load(id).furniture.get(0).locked);
        }
    }

    @Test
    void losPreciosDelUsuarioSeGuardanYSeRestablecen() throws Exception {
        try (Database db = new Database(dir.resolve("precios.db"))) {
            PriceRepository repo = new PriceRepository(db);
            PriceList.Key chair = PriceList.Key.furniture("silla");
            repo.save(chair, 150);
            repo.save(chair, 99.5); // actualiza, no duplica
            PriceList prices = repo.load();
            assertEquals(99.5, prices.get(chair), 1e-9);
            assertTrue(prices.isCustom(chair));
            assertEquals(PriceList.defaultPrice(PriceList.BASEBOARD), prices.get(PriceList.BASEBOARD), 1e-9);
            repo.resetAll();
            assertEquals(PriceList.defaultPrice(chair), repo.load().get(chair), 1e-9);
        }
    }
}
