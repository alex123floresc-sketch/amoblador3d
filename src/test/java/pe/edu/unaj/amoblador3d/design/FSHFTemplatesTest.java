package pe.edu.unaj.amoblador3d.design;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import pe.edu.unaj.amoblador3d.catalog.FSHFBuiltinCatalog;
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalog;
import pe.edu.unaj.amoblador3d.model.FSHFFurniture;
import pe.edu.unaj.amoblador3d.model.FSHFProject;
import pe.edu.unaj.amoblador3d.persistence.FSHFProjectJson;

class FSHFTemplatesTest {
    @Test
    void todasLasPlantillasCumplenLasReglas() throws Exception {
        FSHFCatalog catalog = new FSHFCatalog();
        catalog.setModels(FSHFBuiltinCatalog.load(new ArrayList<>())); // con la planta (modelo 3D)
        assertTrue(catalog.contains("planta"));
        for (FSHFTemplates.Template t : FSHFTemplates.all(catalog)) {
            FSHFProject p = t.build().get();
            // Pasa por el mismo camino que un archivo (validación incluida).
            FSHFProject read = FSHFProjectJson.read(FSHFProjectJson.write(p));
            FSHFDesign d = new FSHFDesign(catalog);
            List<String> warnings = d.load(read);
            assertTrue(warnings.isEmpty(), t.name() + ": " + warnings);
            assertEquals(p.openings.size(), d.openings().size(), t.name() + ": se descartó una abertura");
            for (FSHFFurniture f : d.furniture()) {
                assertTrue(catalog.contains(f.type), t.name() + ": tipo " + f.type);
                assertNull(d.checkFurniture(f, f), t.name() + ": " + d.label(f) + " -> " + d.checkFurniture(f, f));
            }
            assertFalse(d.lights().isEmpty(), t.name());
        }
    }

    @Test
    void cadaVezSeCreaUnProyectoNuevo() {
        FSHFTemplates.Template bedroom = FSHFTemplates.all(new FSHFCatalog()).get(0);
        FSHFProject a = bedroom.build().get(), b = bedroom.build().get();
        a.furniture.get(0).x = 0;
        assertTrue(b.furniture.get(0).x > 0, "cambiar un proyecto no cambia la plantilla");
    }
}
