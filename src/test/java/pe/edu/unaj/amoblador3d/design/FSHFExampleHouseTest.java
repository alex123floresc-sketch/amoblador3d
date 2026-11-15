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
 * Verifica la casa de ejemplo (dos pisos: social abajo, dormitorios arriba) sin abrir el
 * 3D: los ambientes correctos, el reparto por piso y que ninguno se superponga dentro de
 * su piso. Además genera el archivo {@code src/main/resources/ejemplo/casa-ejemplo.amob3d}
 * que viaja con la aplicación.
 */
class FSHFExampleHouseTest {

    private static final String[] PISO1 = {"Sala, comedor y cocina", "Baño de visita", "Escalera", "Entrada"};
    private static final String[] PISO2 = {"Dormitorio principal", "Baño principal", "Pasillo",
            "Dormitorio 2", "Baño 2", "Dormitorio 3"};

    @Test
    void esUnaCasaDeDosPisosConElRepartoAcordado() {
        FSHFHome h = FSHFExampleHouse.build(new FSHFCatalog());
        assertEquals(PISO1.length + PISO2.length, h.spaces.size(), "número de ambientes");
        assertEquals(2, h.levels(), "la casa es de dos pisos");
        for (String n : PISO1) assertEquals(0, espacio(h, n).level, n + " va en el piso 1");
        for (String n : PISO2) assertEquals(1, espacio(h, n).level, n + " va en el piso 2");
    }

    @Test
    void ningunAmbienteSeSuperponeEnSuPiso() {
        FSHFHome h = FSHFExampleHouse.build(new FSHFCatalog());
        for (int i = 0; i < h.spaces.size(); i++) {
            for (int j = i + 1; j < h.spaces.size(); j++) {
                if (h.spaces.get(i).level != h.spaces.get(j).level) continue;  // pisos distintos se apilan
                double[] a = footprint(h.spaces.get(i));
                double[] b = footprint(h.spaces.get(j));
                boolean overlap = a[0] < b[2] - 1e-6 && b[0] < a[2] - 1e-6
                        && a[1] < b[3] - 1e-6 && b[1] < a[3] - 1e-6;
                assertFalse(overlap, h.spaces.get(i).name + " se superpone con " + h.spaces.get(j).name);
            }
        }
    }

    @Test
    void laEscaleraQuedaBajoElPasilloDelPisoDeArriba() {
        FSHFHome h = FSHFExampleHouse.build(new FSHFCatalog());
        var escalera = espacio(h, "Escalera");
        var pasillo = espacio(h, "Pasillo");
        assertEquals(escalera.x, pasillo.x, 1e-6, "la escalera y el pasillo comparten columna X");
    }

    private static FSHFHome.Space espacio(FSHFHome h, String nombre) {
        return h.spaces.stream().filter(s -> s.name.equals(nombre)).findFirst().orElseThrow();
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
