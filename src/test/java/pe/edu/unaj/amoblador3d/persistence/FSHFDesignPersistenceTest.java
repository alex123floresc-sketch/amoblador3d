package pe.edu.unaj.amoblador3d.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalog;
import pe.edu.unaj.amoblador3d.catalog.FSHFCustomDesign;
import pe.edu.unaj.amoblador3d.design.FSHFDesign;
import pe.edu.unaj.amoblador3d.model.FSHFFinish;
import pe.edu.unaj.amoblador3d.model.FSHFFurniture;
import pe.edu.unaj.amoblador3d.model.FSHFProject;

class FSHFDesignPersistenceTest {
    @TempDir
    Path dir;

    private static FSHFCustomDesign design(String id) {
        FSHFCustomDesign d = new FSHFCustomDesign();
        d.id = id;
        d.name = "Mesa " + id;
        FSHFCustomDesign.Piece p = new FSHFCustomDesign.Piece();
        p.width = 1;
        p.height = 0.75;
        p.depth = 0.6;
        d.pieces.add(p);
        return d;
    }

    private static FSHFProject withCustomizedBed() {
        FSHFDesign d = new FSHFDesign(new FSHFCatalog());
        d.loadDefault();
        FSHFFurniture bed = d.addFurniture("cama").value();
        FSHFProject p = d.snapshot();
        FSHFFurniture f = p.furniture.get(0);
        assertEquals(bed.id, f.id);
        f.color2 = "#8c2f39";
        f.finish = FSHFFinish.BRILLANTE;
        return p;
    }

    @Test
    void colorSecundarioYAcabadoSeGuardanEnJson() throws Exception {
        FSHFProject p = withCustomizedBed();
        FSHFProject back = FSHFProjectJson.read(FSHFProjectJson.write(p));
        assertEquals("#8c2f39", back.furniture.get(0).color2);
        assertEquals(FSHFFinish.BRILLANTE, back.furniture.get(0).finish);

        String damaged = FSHFProjectJson.write(p).replace("#8c2f39", "rojo");
        assertNull(FSHFProjectJson.read(damaged).furniture.get(0).color2);
    }

    @Test
    void colorSecundarioYAcabadoSeGuardanEnLaBase() throws Exception {
        try (FSHFDatabase db = new FSHFDatabase(dir.resolve("a.db"))) {
            FSHFProjectRepository repo = new FSHFProjectRepository(db);
            long id = repo.save(null, "Dormitorio", withCustomizedBed(), null);
            FSHFFurniture f = repo.load(id).furniture.get(0);
            assertEquals("#8c2f39", f.color2);
            assertEquals(FSHFFinish.BRILLANTE, f.finish);
        }
    }

    @Test
    void losDisenosSeGuardanActualizanYBorran() throws Exception {
        try (FSHFDatabase db = new FSHFDatabase(dir.resolve("b.db"))) {
            FSHFDesignRepository repo = new FSHFDesignRepository(db);
            repo.save(design("a"));
            repo.save(design("b"));
            FSHFCustomDesign changed = design("a");
            changed.name = "Mesa grande";
            repo.save(changed);
            List<String> warnings = new ArrayList<>();
            List<FSHFCustomDesign> all = repo.list(warnings);
            assertTrue(warnings.isEmpty());
            assertEquals(2, all.size());
            assertEquals("Mesa grande", all.get(0).name);
            repo.delete("a");
            assertEquals(List.of("b"), repo.list(warnings).stream().map(d -> d.id).toList());
        }
    }

    @Test
    void alExportarSeIncluyenLosDisenos() throws Exception {
        FSHFProject p = new FSHFProject();
        String text = FSHFProjectJson.write(p, List.of(design("x")));
        List<FSHFCustomDesign> back = FSHFProjectJson.readDesigns(text);
        assertEquals(1, back.size());
        assertEquals("Mesa x", back.get(0).name);

        FSHFProjectJson.read(text);
        assertTrue(FSHFProjectJson.readDesigns(FSHFProjectJson.write(p)).isEmpty());
    }
}
