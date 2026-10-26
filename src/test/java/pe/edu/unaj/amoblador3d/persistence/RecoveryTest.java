package pe.edu.unaj.amoblador3d.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pe.edu.unaj.amoblador3d.catalog.Catalog;
import pe.edu.unaj.amoblador3d.design.Design;
import pe.edu.unaj.amoblador3d.model.Project;

class RecoveryTest {
    @TempDir
    Path dir;

    private static Project sample() {
        Design d = new Design(new Catalog());
        d.loadDefault();
        d.addFurniture("cama");
        return d.snapshot();
    }

    @Test
    void seRecuperaLoQueSeCopio() throws Exception {
        Recovery r = new Recovery(dir.resolve("sub").resolve("recuperacion.json"));
        assertTrue(r.read().isEmpty());
        Project p = sample();
        r.write(p, 7L, "Mi cuarto");
        Recovery.Snapshot s = r.read().orElseThrow();
        assertEquals(7L, s.projectId());
        assertEquals("Mi cuarto", s.name());
        assertEquals(ProjectJson.write(p), ProjectJson.write(s.project()));
        // Se puede escribir encima varias veces (reemplaza la copia anterior).
        r.write(p, null, "Sin título");
        assertNull(r.read().orElseThrow().projectId());
        r.delete();
        assertTrue(r.read().isEmpty());
    }

    @Test
    void unaCopiaDanadaSeDescarta() throws Exception {
        Path file = dir.resolve("recuperacion.json");
        Files.writeString(file, "{\"name\":\"x\",\"project\":{\"app\":\"otra\"}}");
        assertTrue(new Recovery(file).read().isEmpty());
        assertFalse(Files.exists(file));
        Files.writeString(file, "no es json");
        assertTrue(new Recovery(file).read().isEmpty());
    }
}
