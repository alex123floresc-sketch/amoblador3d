package pe.edu.unaj.amoblador3d.design;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalog;
import pe.edu.unaj.amoblador3d.model.FSHFHome;
import pe.edu.unaj.amoblador3d.persistence.FSHFHomeJson;

/**
 * Verifica la casa de ejemplo (un piso, según el plano) sin necesidad de abrir el 3D:
 * los ambientes correctos, todos en el mismo piso y sin superponerse. Además genera el
 * archivo {@code src/main/resources/ejemplo/casa-ejemplo.amob3d} que viaja con la app.
 */
class FSHFExampleHouseTest {

    private static final String[] ESPERADOS = {
            "Sala, comedor y cocina", "Baño principal", "Dormitorio principal",
            "Pasillo", "Entrada", "Dormitorio 2", "Baño 2", "Dormitorio 3"
    };

    @Test
    void tieneLosAmbientesDelPlanoEnUnSoloPiso() {
        FSHFHome h = FSHFExampleHouse.build(new FSHFCatalog());
        assertEquals(ESPERADOS.length, h.spaces.size(), "número de ambientes");
        for (int i = 0; i < ESPERADOS.length; i++) {
            assertEquals(ESPERADOS[i], h.spaces.get(i).name);
            assertEquals(0, h.spaces.get(i).level, ESPERADOS[i] + " debe estar en la planta baja");
        }
        assertEquals(1, h.levels(), "la casa es de un solo piso");
    }

    @Test
    void ningunAmbienteSeSuperpone() {
        FSHFHome h = FSHFExampleHouse.build(new FSHFCatalog());
        for (int i = 0; i < h.spaces.size(); i++) {
            for (int j = i + 1; j < h.spaces.size(); j++) {
                double[] a = footprint(h.spaces.get(i));
                double[] b = footprint(h.spaces.get(j));
                boolean overlap = a[0] < b[2] - 1e-6 && b[0] < a[2] - 1e-6
                        && a[1] < b[3] - 1e-6 && b[1] < a[3] - 1e-6;
                assertFalse(overlap, h.spaces.get(i).name + " se superpone con " + h.spaces.get(j).name);
            }
        }
    }

    /** Footprint interior del ambiente (sin contar los muros): [minX, minZ, maxX, maxZ]. */
    private static double[] footprint(FSHFHome.Space s) {
        return new double[] {s.x, s.z, s.x + s.project.room.width(), s.z + s.project.room.length()};
    }

    @Test
    void elArchivoSeEscribeYSeVuelveALeer() throws Exception {
        FSHFHome h = FSHFExampleHouse.build(new FSHFCatalog());
        String json = FSHFHomeJson.write(h);

        FSHFHome leido = FSHFHomeJson.read(json, "Casa de ejemplo");
        assertEquals(h.spaces.size(), leido.spaces.size());
        assertTrue(json.contains("Dormitorio principal") && json.contains("Pasillo"));

        // Genera el recurso que viaja con la aplicación.
        Path out = Path.of("src", "main", "resources", "ejemplo", "casa-ejemplo.amob3d");
        Files.createDirectories(out.getParent());
        Files.writeString(out, json);
        assertTrue(Files.size(out) > 0);
    }
}
