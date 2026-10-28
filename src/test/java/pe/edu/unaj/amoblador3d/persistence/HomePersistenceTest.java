package pe.edu.unaj.amoblador3d.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pe.edu.unaj.amoblador3d.catalog.Catalog;
import pe.edu.unaj.amoblador3d.design.Design;
import pe.edu.unaj.amoblador3d.design.Templates;
import pe.edu.unaj.amoblador3d.model.Home;
import pe.edu.unaj.amoblador3d.model.Project;
import pe.edu.unaj.amoblador3d.model.RoomDims;

/** Casas con varios ambientes: ubicación, archivos .amob3d, base de datos y proyectos antiguos. */
class HomePersistenceTest {
    @TempDir
    Path dir;

    private static Project room(String template) {
        return Templates.all(new Catalog()).stream().filter(t -> t.name().equals(template)).findFirst().orElseThrow()
                .build().get();
    }

    private static Home house() {
        Design d = new Design(new Catalog());
        d.loadDefault();
        Home h = Home.of(d.snapshot(), "Sala");
        for (String t : new String[] {"Dormitorio", "Cocina"}) {
            Project p = room(t);
            double[] spot = h.freeSpot(p);
            h.spaces.add(new Home.Space(h.nextId(), h.uniqueName(t), spot[0], spot[1], p));
        }
        return h;
    }

    @Test
    void losAmbientesNuevosSePeganSinEncimarse() {
        Home h = house();
        assertEquals(3, h.spaces.size());
        for (int i = 0; i < h.spaces.size(); i++) {
            for (int j = i + 1; j < h.spaces.size(); j++) {
                assertFalse(Home.overlaps(h.spaces.get(i).outer(), h.spaces.get(j).outer()), i + " y " + j);
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
        Home h = house();
        Home.Space kitchen = h.spaces.get(2);
        assertNotNull(h.collision(kitchen, h.spaces.get(0).x + 0.5, h.spaces.get(0).z));
        assertNull(h.collision(kitchen, kitchen.x, kitchen.z + 20));
    }

    @Test
    void elArchivoGuardaTodaLaCasa() throws Exception {
        Home h = house();
        String json = HomeJson.write(h);
        Home back = HomeJson.read(json, "X");
        assertEquals(json, HomeJson.write(back));
        assertEquals("Dormitorio", back.spaces.get(1).name);
        // Una versión anterior lee el primer ambiente con normalidad.
        assertEquals(h.spaces.get(0).project.room, ProjectJson.read(json).room);
    }

    @Test
    void unArchivoDeUnaSolaHabitacionEsUnaCasaDeUnAmbiente() throws Exception {
        Project p = room("Baño");
        Home h = HomeJson.read(ProjectJson.write(p), "Mi baño");
        assertEquals(1, h.spaces.size());
        assertEquals("Mi baño", h.spaces.get(0).name);
        assertEquals(p.furniture.size(), h.spaces.get(0).project.furniture.size());
    }

    @Test
    void idsYNombresRepetidosSeCorrigen() throws Exception {
        Home h = house();
        h.spaces.get(1).id = h.spaces.get(0).id;
        h.spaces.get(2).name = "Sala";
        Home back = HomeJson.read(HomeJson.write(h), "X");
        assertEquals(3, back.spaces.stream().map(s -> s.id).distinct().count());
        assertEquals(3, back.spaces.stream().map(s -> s.name.toLowerCase()).distinct().count());
    }

    @Test
    void laBaseGuardaYLeeTodaLaCasa() throws Exception {
        try (Database db = new Database(dir.resolve("casa.db"))) {
            ProjectRepository repo = new ProjectRepository(db);
            Home h = house();
            long id = repo.saveHome(null, "Mi casa", h, null);
            Home back = repo.loadHome(id);
            assertEquals(HomeJson.write(h), HomeJson.write(back));
            // Al guardar de nuevo con menos ambientes, no quedan restos.
            h.spaces.remove(2);
            h.spaces.get(1).project.room = new RoomDims(3, 3, 2.5, 0.15);
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
