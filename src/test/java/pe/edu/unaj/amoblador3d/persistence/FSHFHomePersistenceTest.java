package pe.edu.unaj.amoblador3d.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalog;
import pe.edu.unaj.amoblador3d.design.FSHFDesign;
import pe.edu.unaj.amoblador3d.design.FSHFTemplates;
import pe.edu.unaj.amoblador3d.model.FSHFHome;
import pe.edu.unaj.amoblador3d.model.FSHFProject;
import pe.edu.unaj.amoblador3d.model.FSHFRoomDims;

/** Casas con varios ambientes: ubicación, archivos .amob3d, base de datos y proyectos antiguos. */
class FSHFHomePersistenceTest {
    @TempDir
    Path dir;

    private static FSHFProject room(String template) {
        return FSHFTemplates.all(new FSHFCatalog()).stream().filter(t -> t.name().equals(template)).findFirst().orElseThrow()
                .build().get();
    }

    private static FSHFHome house() {
        FSHFDesign d = new FSHFDesign(new FSHFCatalog());
        d.loadDefault();
        FSHFHome h = FSHFHome.of(d.snapshot(), "Sala");
        for (String t : new String[] {"Dormitorio", "Cocina"}) {
            FSHFProject p = room(t);
            double[] spot = h.freeSpot(p);
            h.spaces.add(new FSHFHome.Space(h.nextId(), h.uniqueName(t), spot[0], spot[1], p));
        }
        return h;
    }

    @Test
    void losAmbientesNuevosSePeganSinEncimarse() {
        FSHFHome h = house();
        assertEquals(3, h.spaces.size());
        for (int i = 0; i < h.spaces.size(); i++) {
            for (int j = i + 1; j < h.spaces.size(); j++) {
                assertFalse(FSHFHome.overlaps(h.spaces.get(i).outer(), h.spaces.get(j).outer()), i + " y " + j);
            }
        }
        // Muro con muro: el borde exterior de uno es el del siguiente.
        assertEquals(h.spaces.get(0).outer()[2], h.spaces.get(1).outer()[0], 1e-9);
        assertEquals("ambiente-4", h.nextId());
        assertEquals("Sala 2", h.uniqueName("Sala"));
        assertEquals("sala 2", h.uniqueName("sala")); // sin distinguir mayúsculas, respetando lo escrito
    }

    @Test
    void unaColisionSeDetecta() {
        FSHFHome h = house();
        FSHFHome.Space kitchen = h.spaces.get(2);
        assertNotNull(h.collision(kitchen, h.spaces.get(0).x + 0.5, h.spaces.get(0).z));
        assertNull(h.collision(kitchen, kitchen.x, kitchen.z + 20));
    }

    @Test
    void elArchivoGuardaTodaLaCasa() throws Exception {
        FSHFHome h = house();
        String json = FSHFHomeJson.write(h);
        FSHFHome back = FSHFHomeJson.read(json, "X");
        assertEquals(json, FSHFHomeJson.write(back));
        assertEquals("Dormitorio", back.spaces.get(1).name);
        // Una versión anterior lee el primer ambiente con normalidad.
        assertEquals(h.spaces.get(0).project.room, FSHFProjectJson.read(json).room);
    }

    @Test
    void elGiroElPisoYLosTiposDePuertaSeGuardan() throws Exception {
        FSHFHome h = house();
        FSHFHome.Space kitchen = h.spaces.get(2);
        kitchen.rotation = 90;
        kitchen.level = 1;
        kitchen.project.openings.add(new pe.edu.unaj.amoblador3d.model.FSHFOpening("garage-1",
                pe.edu.unaj.amoblador3d.model.FSHFOpeningType.GARAGE, pe.edu.unaj.amoblador3d.model.FSHFWallSide.NORTE, 0.2, 2.6, 2.2, 0));
        FSHFHome back = FSHFHomeJson.read(FSHFHomeJson.write(h), "X");
        assertEquals(90, back.spaces.get(2).rotation);
        assertEquals(1, back.spaces.get(2).level);
        assertTrue(back.spaces.get(2).project.openings.stream().anyMatch(o -> o.type == pe.edu.unaj.amoblador3d.model.FSHFOpeningType.GARAGE));
        try (FSHFDatabase db = new FSHFDatabase(dir.resolve("pisos.db"))) {
            FSHFProjectRepository repo = new FSHFProjectRepository(db);
            long id = repo.saveHome(null, "Dos pisos", h, null);
            assertEquals(FSHFHomeJson.write(h), FSHFHomeJson.write(repo.loadHome(id)));
            // El primer ambiente va en las tablas: su puerta de otro tipo pasa el CHECK de la tabla opening.
            h.spaces.get(0).project.openings.get(0).type = pe.edu.unaj.amoblador3d.model.FSHFOpeningType.ENTRY;
            repo.saveHome(id, "Dos pisos", h, null);
            assertEquals(pe.edu.unaj.amoblador3d.model.FSHFOpeningType.ENTRY, repo.loadHome(id).spaces.get(0).project.openings.get(0).type);
        }
        // Valores fuera de rango en un archivo se corrigen al abrirlo.
        String bad = FSHFHomeJson.write(h).replace("\"rotation\": 90", "\"rotation\": 100").replace("\"level\": 1", "\"level\": 9");
        FSHFHome fixed = FSHFHomeJson.read(bad, "X");
        assertEquals(90, fixed.spaces.get(2).rotation);
        assertEquals(FSHFHome.MAX_LEVELS - 1, fixed.spaces.get(2).level);
    }

    @Test
    void unArchivoDeUnaSolaHabitacionEsUnaCasaDeUnAmbiente() throws Exception {
        FSHFProject p = room("Baño");
        FSHFHome h = FSHFHomeJson.read(FSHFProjectJson.write(p), "Mi baño");
        assertEquals(1, h.spaces.size());
        assertEquals("Mi baño", h.spaces.get(0).name);
        assertEquals(p.furniture.size(), h.spaces.get(0).project.furniture.size());
    }

    @Test
    void idsYNombresRepetidosSeCorrigen() throws Exception {
        FSHFHome h = house();
        h.spaces.get(1).id = h.spaces.get(0).id;
        h.spaces.get(2).name = "Sala";
        FSHFHome back = FSHFHomeJson.read(FSHFHomeJson.write(h), "X");
        assertEquals(3, back.spaces.stream().map(s -> s.id).distinct().count());
        assertEquals(3, back.spaces.stream().map(s -> s.name.toLowerCase()).distinct().count());
    }

    @Test
    void laBaseGuardaYLeeTodaLaCasa() throws Exception {
        try (FSHFDatabase db = new FSHFDatabase(dir.resolve("casa.db"))) {
            FSHFProjectRepository repo = new FSHFProjectRepository(db);
            FSHFHome h = house();
            long id = repo.saveHome(null, "Mi casa", h, null);
            FSHFHome back = repo.loadHome(id);
            assertEquals(FSHFHomeJson.write(h), FSHFHomeJson.write(back));
            // Al guardar de nuevo con menos ambientes, no quedan restos.
            h.spaces.remove(2);
            h.spaces.get(1).project.room = new FSHFRoomDims(3, 3, 2.5, 0.15);
            repo.saveHome(id, "Mi casa", h, null);
            back = repo.loadHome(id);
            assertEquals(2, back.spaces.size());
            assertEquals(3, back.spaces.get(1).project.room.width(), 1e-9);
            // Un proyecto guardado como una sola habitación se abre como casa de un ambiente.
            long old = repo.save(null, "Antiguo", room("Oficina"), null);
            assertEquals(1, repo.loadHome(old).spaces.size());
            repo.delete(id);
            assertNull(repo.loadHome(id));
            assertTrue(repo.list().stream().noneMatch(s -> s.id() == id));
        }
    }
}
