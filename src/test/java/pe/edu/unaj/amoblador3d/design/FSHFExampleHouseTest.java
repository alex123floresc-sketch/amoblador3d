package pe.edu.unaj.amoblador3d.design;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalog;
import pe.edu.unaj.amoblador3d.model.FSHFFurniture;
import pe.edu.unaj.amoblador3d.model.FSHFHome;
import pe.edu.unaj.amoblador3d.model.FSHFOpening;
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
            "Estudio", "Dormitorio 2", "Baño 2", "Dormitorio 3"};

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

    @Test
    void losAmbientesQuedanPegadosSinEncimarseSegunLaApp() {
        FSHFHome h = FSHFExampleHouse.build(new FSHFCatalog());
        for (FSHFHome.Space s : h.spaces) {
            FSHFHome.Space otro = h.collision(s, s.x, s.z);
            assertNull(otro, s.name + " se encima con " + (otro == null ? "" : otro.name));
        }
    }

    @Test
    void cadaPisoOcupaElMismoTerrenoDe9x9() {
        FSHFHome h = FSHFExampleHouse.build(new FSHFCatalog());
        for (int nivel = 0; nivel < 2; nivel++) {
            double area = 0;
            for (FSHFHome.Space s : h.spaces) {
                if (s.level != nivel) continue;
                double[] o = s.outer();
                area += (o[2] - o[0]) * (o[3] - o[1]);
                assertTrue(o[0] >= -1e-6 && o[1] >= -1e-6 && o[2] <= 9 + 1e-6 && o[3] <= 9 + 1e-6, s.name + " sale del terreno");
            }
            assertEquals(81.0, area, 1e-6, "el piso " + (nivel + 1) + " cubre todo el terreno sin huecos");
        }
    }

    @Test
    void losDormitoriosYLosBanosDelPiso2MidenLoMismo() {
        FSHFHome h = FSHFExampleHouse.build(new FSHFCatalog());
        for (String n : new String[] {"Dormitorio principal", "Estudio", "Dormitorio 2", "Dormitorio 3"}) {
            assertEquals(3.30, espacio(h, n).project.room.width(), 1e-6, n);
            assertEquals(3.30, espacio(h, n).project.room.length(), 1e-6, n);
        }
        for (String n : new String[] {"Baño principal", "Baño 2"}) {
            assertEquals(3.30, espacio(h, n).project.room.width(), 1e-6, n);
            assertEquals(1.50, espacio(h, n).project.room.length(), 1e-6, n);
        }
        assertEquals(1.50, espacio(h, "Pasillo").project.room.width(), 1e-6, "ancho del pasillo");
    }

    @Test
    void cadaPuertaInteriorComunicaConOtroAmbiente() {
        FSHFHome h = FSHFExampleHouse.build(new FSHFCatalog());
        long puertas = 0;
        for (FSHFHome.Space s : h.spaces) {
            for (FSHFOpening o : s.project.openings) {
                if (!o.type.isDoor() || o.type == pe.edu.unaj.amoblador3d.model.FSHFOpeningType.ENTRY) continue;
                puertas++;
                boolean comunica = h.links().stream().anyMatch(l -> l.from() == s && l.door() == o && !l.exterior());
                assertTrue(comunica, "la puerta " + o.id + " de " + s.name + " no da a ningún ambiente");
            }
        }
        assertEquals(9, puertas, "puertas interiores");
    }

    @Test
    void losMueblesCabenSinChocarNiTaparPuertas() {
        FSHFCatalog catalog = new FSHFCatalog();
        FSHFHome h = FSHFExampleHouse.build(catalog);
        for (FSHFHome.Space s : h.spaces) {
            FSHFDesign d = new FSHFDesign(catalog);
            d.load(s.project);
            for (FSHFFurniture f : d.furniture()) {
                assertNull(d.checkFurniture(f, f), s.name + ": " + f.type);
            }
        }
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
        assertEquals(2, leido.levels(), "al leer debe conservar los dos pisos");
        assertEquals(1, espacio(leido, "Dormitorio principal").level, "el dormitorio sigue en el piso 2");
        assertTrue(json.contains("Dormitorio principal") && json.contains("Pasillo"));

        // Genera el recurso que viaja con la aplicación.
        Path out = Path.of("src", "main", "resources", "ejemplo", "casa-ejemplo.amob3d");
        Files.createDirectories(out.getParent());
        Files.writeString(out, json);
        assertTrue(Files.size(out) > 0);
    }
}
