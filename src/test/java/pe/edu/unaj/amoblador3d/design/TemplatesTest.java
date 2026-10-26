package pe.edu.unaj.amoblador3d.design;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import pe.edu.unaj.amoblador3d.catalog.BuiltinCatalog;
import pe.edu.unaj.amoblador3d.catalog.Catalog;
import pe.edu.unaj.amoblador3d.model.Furniture;
import pe.edu.unaj.amoblador3d.model.Project;
import pe.edu.unaj.amoblador3d.persistence.ProjectJson;

class TemplatesTest {
    @Test
    void todasLasPlantillasCumplenLasReglas() throws Exception {
        Catalog catalog = new Catalog();
        catalog.setModels(BuiltinCatalog.load(new ArrayList<>())); // con la planta (modelo 3D)
        assertTrue(catalog.contains("planta"));
        for (Templates.Template t : Templates.all(catalog)) {
            Project p = t.build().get();
            // Pasa por el mismo camino que un archivo (validación incluida).
            Project read = ProjectJson.read(ProjectJson.write(p));
            Design d = new Design(catalog);
            List<String> warnings = d.load(read);
            assertTrue(warnings.isEmpty(), t.name() + ": " + warnings);
            assertEquals(p.openings.size(), d.openings().size(), t.name() + ": se descartó una abertura");
            for (Furniture f : d.furniture()) {
                assertTrue(catalog.contains(f.type), t.name() + ": tipo " + f.type);
                assertNull(d.checkFurniture(f, f), t.name() + ": " + d.label(f) + " -> " + d.checkFurniture(f, f));
            }
            assertFalse(d.lights().isEmpty(), t.name());
        }
    }

    @Test
    void cadaVezSeCreaUnProyectoNuevo() {
        Templates.Template bedroom = Templates.all(new Catalog()).get(0);
        Project a = bedroom.build().get(), b = bedroom.build().get();
        a.furniture.get(0).x = 0;
        assertTrue(b.furniture.get(0).x > 0, "cambiar un proyecto no cambia la plantilla");
    }
}
