package pe.edu.unaj.amoblador3d.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pe.edu.unaj.amoblador3d.catalog.Catalog;
import pe.edu.unaj.amoblador3d.catalog.CustomDesign;
import pe.edu.unaj.amoblador3d.design.Design;
import pe.edu.unaj.amoblador3d.model.Finish;
import pe.edu.unaj.amoblador3d.model.Furniture;
import pe.edu.unaj.amoblador3d.model.Project;

/** Colores secundarios, acabados y muebles del Diseñador: SQLite y archivos .amob3d. */
class DesignPersistenceTest {
    @TempDir
    Path dir;

    private static CustomDesign design(String id) {
        CustomDesign d = new CustomDesign();
        d.id = id;
        d.name = "Mesa " + id;
        CustomDesign.Piece p = new CustomDesign.Piece();
        p.width = 1;
        p.height = 0.75;
        p.depth = 0.6;
        d.pieces.add(p);
        return d;
    }

    private static Project withCustomizedBed() {
        Design d = new Design(new Catalog());
        d.loadDefault();
        Furniture bed = d.addFurniture("cama").value();
        Project p = d.snapshot();
        Furniture f = p.furniture.get(0);
        assertEquals(bed.id, f.id);
        f.color2 = "#8c2f39";
        f.finish = Finish.BRILLANTE;
        return p;
    }

    @Test
    void colorSecundarioYAcabadoSeGuardanEnJson() throws Exception {
        Project p = withCustomizedBed();
        Project back = ProjectJson.read(ProjectJson.write(p));
        assertEquals("#8c2f39", back.furniture.get(0).color2);
        assertEquals(Finish.BRILLANTE, back.furniture.get(0).finish);
        // Un color dañado se descarta (vuelve a automático) en lugar de impedir abrir el archivo.
        String damaged = ProjectJson.write(p).replace("#8c2f39", "rojo");
        assertNull(ProjectJson.read(damaged).furniture.get(0).color2);
    }

    @Test
    void colorSecundarioYAcabadoSeGuardanEnLaBase() throws Exception {
        try (Database db = new Database(dir.resolve("a.db"))) {
            ProjectRepository repo = new ProjectRepository(db);
            long id = repo.save(null, "Dormitorio", withCustomizedBed(), null);
            Furniture f = repo.load(id).furniture.get(0);
            assertEquals("#8c2f39", f.color2);
            assertEquals(Finish.BRILLANTE, f.finish);
        }
    }

    @Test
    void losDisenosSeGuardanActualizanYBorran() throws Exception {
        try (Database db = new Database(dir.resolve("b.db"))) {
            DesignRepository repo = new DesignRepository(db);
            repo.save(design("a"));
            repo.save(design("b"));
            CustomDesign changed = design("a");
            changed.name = "Mesa grande";
            repo.save(changed);
            List<String> warnings = new ArrayList<>();
            List<CustomDesign> all = repo.list(warnings);
            assertTrue(warnings.isEmpty());
            assertEquals(2, all.size());
            assertEquals("Mesa grande", all.get(0).name);
            repo.delete("a");
            assertEquals(List.of("b"), repo.list(warnings).stream().map(d -> d.id).toList());
        }
    }

    @Test
    void alExportarSeIncluyenLosDisenos() throws Exception {
        Project p = new Project();
        String text = ProjectJson.write(p, List.of(design("x")));
        List<CustomDesign> back = ProjectJson.readDesigns(text);
        assertEquals(1, back.size());
        assertEquals("Mesa x", back.get(0).name);
        // El proyecto se sigue leyendo normalmente (las versiones anteriores ignoran "designs").
        ProjectJson.read(text);
        assertTrue(ProjectJson.readDesigns(ProjectJson.write(p)).isEmpty());
    }
}
