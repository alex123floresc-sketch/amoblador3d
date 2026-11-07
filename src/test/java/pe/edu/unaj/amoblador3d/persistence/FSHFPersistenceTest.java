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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalog;
import pe.edu.unaj.amoblador3d.design.FSHFDesign;
import pe.edu.unaj.amoblador3d.estimate.FSHFPriceList;
import pe.edu.unaj.amoblador3d.model.FSHFCameraView;
import pe.edu.unaj.amoblador3d.model.FSHFLightType;
import pe.edu.unaj.amoblador3d.model.FSHFProject;
import pe.edu.unaj.amoblador3d.model.FSHFSurface;
import pe.edu.unaj.amoblador3d.model.FSHFSurfaceMaterial;
import pe.edu.unaj.amoblador3d.model.FSHFWallSide;

class FSHFPersistenceTest {
    @TempDir
    Path dir;

    private static FSHFProject sample() {
        FSHFDesign d = new FSHFDesign(new FSHFCatalog());
        d.loadDefault();
        d.addFurniture("cama");
        FSHFProject p = d.snapshot();
        p.floor.material = FSHFSurfaceMaterial.MARMOL;
        p.floor.color = "#ffffff";
        p.walls.get(FSHFWallSide.NORTE).material = FSHFSurfaceMaterial.LADRILLO;
        p.walls.get(FSHFWallSide.NORTE).color = "#a4553a";
        return p;
    }

    @Test
    void jsonIdaYVueltaConservaTodo() throws Exception {
        FSHFProject p = sample();
        String json = FSHFProjectJson.write(p);
        assertEquals(json, FSHFProjectJson.write(FSHFProjectJson.read(json)));
    }

    @Test
    void archivoAntiguoSinSuperficiesUsaLosAcabadosPorDefecto() throws Exception {
        String old = """
                {"app":"Amoblador3D","version":1,
                 "room":{"width":4,"length":3,"height":2.5,"wallThickness":0.15},
                 "openings":[],"lights":[],"furniture":[]}""";
        FSHFProject p = FSHFProjectJson.read(old);
        assertEquals(FSHFSurface.DEFAULT_FLOOR, p.floor.material);
        assertEquals(FSHFSurface.DEFAULT_WALL, p.walls.get(FSHFWallSide.SUR).material);
    }

    @Test
    void superficiesDanadasSeCorrigenSinPerderElProyecto() throws Exception {
        String json = """
                {"app":"Amoblador3D","version":1,
                 "room":{"width":4,"length":3,"height":2.5,"wallThickness":0.15},
                 "surfaces":{"piso":{"material":"madera","color":"rojo"},
                             "pared-norte":{"material":"alfombra","color":"#112233"}}}""";
        FSHFProject p = FSHFProjectJson.read(json);
        assertEquals(FSHFSurfaceMaterial.MADERA.defaultColor(), p.floor.color);
        assertEquals(FSHFSurface.DEFAULT_WALL, p.walls.get(FSHFWallSide.NORTE).material);
    }

    @Test
    void archivosNoValidosDanUnErrorClaro() {
        assertThrows(FSHFProjectFormatException.class, () -> FSHFProjectJson.read("no es json {"));
        assertThrows(FSHFProjectFormatException.class, () -> FSHFProjectJson.read("{\"app\":\"Otra\"}"));
        assertThrows(FSHFProjectFormatException.class, () -> FSHFProjectJson.read("""
                {"app":"Amoblador3D","room":{"width":4,"length":3,"height":2.5,"wallThickness":0.15},
                 "surfaces":{"piso":{"material":"lava","color":"#000000"}}}"""));
    }

    @Test
    void guardarYCargarEnSqlite() throws Exception {
        try (FSHFDatabase db = new FSHFDatabase(dir.resolve("a.db"))) {
            FSHFProjectRepository repo = new FSHFProjectRepository(db);
            FSHFProject p = sample();
            long id = repo.save(null, "Dormitorio", p, null);
            repo.save(id, "Dormitorio", p, null); // sobrescribir no duplica filas
            FSHFProject q = repo.load(id);
            assertEquals(FSHFProjectJson.write(p), FSHFProjectJson.write(q));
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
        try (FSHFDatabase db = new FSHFDatabase(file)) {
            try (Statement st = db.connection().createStatement(); ResultSet rs = st.executeQuery("PRAGMA user_version")) {
                assertEquals(FSHFDatabase.SCHEMA_VERSION, rs.getInt(1));
            }
            FSHFProject p = new FSHFProjectRepository(db).load(1);
            assertNotNull(p);
            assertEquals(FSHFSurface.DEFAULT_FLOOR, p.floor.material);
            // Y ya se puede guardar con superficies.
            p.floor.material = FSHFSurfaceMaterial.CERAMICO;
            new FSHFProjectRepository(db).save(1L, "Viejo", p, null);
            assertEquals(FSHFSurfaceMaterial.CERAMICO, new FSHFProjectRepository(db).load(1).floor.material);
        }
    }

    @Test
    void nombresRepetidosSeDetectanSinImportarMayusculas() throws Exception {
        try (FSHFDatabase db = new FSHFDatabase(dir.resolve("b.db"))) {
            FSHFProjectRepository repo = new FSHFProjectRepository(db);
            long id = repo.save(null, "Sala", new FSHFProject(), null);
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
        try (FSHFDatabase db = new FSHFDatabase(file)) {
            FSHFProject p = new FSHFProjectRepository(db).load(1);
            assertNotNull(p);
            assertEquals(FSHFSurfaceMaterial.LADRILLO, p.walls.get(FSHFWallSide.NORTE).material);
            assertEquals(4, p.furniture.size());
            assertTrue(p.furniture.get(0).locked, "la cama del ejemplo está bloqueada");
            assertEquals(List.of("Desde la puerta"), p.views.stream().map(FSHFCameraView::name).toList());
            assertEquals("window-1", p.lights.stream().filter(l -> l.type == FSHFLightType.SUN).findFirst().orElseThrow().windowId);
            FSHFDesign design = new FSHFDesign(new FSHFCatalog());
            assertTrue(design.load(p).isEmpty(), "el ejemplo no debería necesitar correcciones");
            assertEquals(70, new FSHFPriceRepository(db).load().get(FSHFPriceList.Key.material(FSHFSurfaceMaterial.PINTURA)), 1e-9);
        }
    }

    @Test
    void elBloqueoSeGuardaEnLaBaseYEnElArchivo() throws Exception {
        FSHFProject p = sample();
        p.furniture.get(0).locked = true;
        assertTrue(FSHFProjectJson.read(FSHFProjectJson.write(p)).furniture.get(0).locked);
        try (FSHFDatabase db = new FSHFDatabase(dir.resolve("bloqueo.db"))) {
            FSHFProjectRepository repo = new FSHFProjectRepository(db);
            long id = repo.save(null, "Con bloqueo", p, null);
            assertTrue(repo.load(id).furniture.get(0).locked);
        }
    }

    @Test
    void lasVistasGuardadasSeConservan() throws Exception {
        FSHFDesign d = new FSHFDesign(new FSHFCatalog());
        d.loadDefault();
        d.saveView(new FSHFCameraView("Desde la puerta", FSHFCameraView.Kind.WALK, 1.5, 1.6, 3.2, 0.3, -0.1, 0));
        d.saveView(new FSHFCameraView("Planta", FSHFCameraView.Kind.PLAN, 2, 0, 1.75, 0, 0, 8));
        d.saveView(new FSHFCameraView("  desde la PUERTA ", FSHFCameraView.Kind.ORBIT, 2, 1, 1.75, 0.5, 0.6, 9)); // reemplaza
        assertEquals(2, d.views().size());
        assertEquals(FSHFCameraView.Kind.ORBIT, d.views().get(0).kind());
        assertEquals("desde la PUERTA", d.views().get(0).name());

        FSHFProject p = d.snapshot();
        assertEquals(p.views, FSHFProjectJson.read(FSHFProjectJson.write(p)).views);
        try (FSHFDatabase db = new FSHFDatabase(dir.resolve("vistas.db"))) {
            FSHFProjectRepository repo = new FSHFProjectRepository(db);
            long id = repo.save(null, "Con vistas", p, null);
            assertEquals(p.views, repo.load(id).views);
            p.views.clear();
            repo.save(id, "Con vistas", p, null); // al guardar de nuevo se reemplazan
            assertTrue(repo.load(id).views.isEmpty());
        }
        d.removeView("Planta");
        assertEquals(1, d.views().size());
    }

    @Test
    void unaVistaDanadaSeDescartaSinPerderElProyecto() throws Exception {
        String json = """
                {"app":"Amoblador3D","version":1,
                 "room":{"width":4,"length":3,"height":2.5,"wallThickness":0.15},
                 "views":[{"name":"Buena","mode":"plan","x":2,"y":0,"z":1.5,"yaw":0,"pitch":0,"distance":8},
                          {"name":"","mode":"3d"},{"name":"Rara","mode":"lateral"},
                          {"name":"buena","mode":"walk","x":1,"y":1.6,"z":1,"yaw":0,"pitch":0,"distance":0}]}""";
        FSHFProject p = FSHFProjectJson.read(json);
        assertEquals(1, p.views.size());
        assertEquals("Buena", p.views.get(0).name());
    }

    @Test
    void losPreciosDelUsuarioSeGuardanYSeRestablecen() throws Exception {
        try (FSHFDatabase db = new FSHFDatabase(dir.resolve("precios.db"))) {
            FSHFPriceRepository repo = new FSHFPriceRepository(db);
            FSHFPriceList.Key chair = FSHFPriceList.Key.furniture("silla");
            repo.save(chair, 150);
            repo.save(chair, 99.5); // actualiza, no duplica
            FSHFPriceList prices = repo.load();
            assertEquals(99.5, prices.get(chair), 1e-9);
            assertTrue(prices.isCustom(chair));
            assertEquals(FSHFPriceList.defaultPrice(FSHFPriceList.BASEBOARD), prices.get(FSHFPriceList.BASEBOARD), 1e-9);
            repo.resetAll();
            assertEquals(FSHFPriceList.defaultPrice(chair), repo.load().get(chair), 1e-9);
        }
    }
}
