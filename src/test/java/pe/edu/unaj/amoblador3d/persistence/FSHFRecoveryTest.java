package pe.edu.unaj.amoblador3d.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalog;
import pe.edu.unaj.amoblador3d.design.FSHFDesign;
import pe.edu.unaj.amoblador3d.model.FSHFHome;
import pe.edu.unaj.amoblador3d.model.FSHFProject;

class FSHFRecoveryTest {
    @TempDir
    Path dir;

    private static FSHFProject sample() {
        FSHFDesign d = new FSHFDesign(new FSHFCatalog());
        d.loadDefault();
        d.addFurniture("cama");
        return d.snapshot();
    }

    @Test
    void seRecuperaLoQueSeCopio() throws Exception {
        FSHFRecovery r = new FSHFRecovery(dir.resolve("sub").resolve("recuperacion.json"));
        assertTrue(r.read().isEmpty());
        FSHFHome p = FSHFHome.of(sample(), "Ambiente 1");
        r.write(p, 7L, "Mi cuarto");
        FSHFRecovery.Snapshot s = r.read().orElseThrow();
        assertEquals(7L, s.projectId());
        assertEquals("Mi cuarto", s.name());
        assertEquals(FSHFHomeJson.write(p), FSHFHomeJson.write(s.home()));
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
        assertTrue(new FSHFRecovery(file).read().isEmpty());
        assertFalse(Files.exists(file));
        Files.writeString(file, "no es json");
        assertTrue(new FSHFRecovery(file).read().isEmpty());
    }
}
