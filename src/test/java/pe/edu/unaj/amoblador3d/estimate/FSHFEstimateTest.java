package pe.edu.unaj.amoblador3d.estimate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalog;
import pe.edu.unaj.amoblador3d.design.FSHFDesign;
import pe.edu.unaj.amoblador3d.model.FSHFOpening;
import pe.edu.unaj.amoblador3d.model.FSHFOpeningType;
import pe.edu.unaj.amoblador3d.model.FSHFProject;
import pe.edu.unaj.amoblador3d.model.FSHFRoomDims;
import pe.edu.unaj.amoblador3d.model.FSHFSurfaceMaterial;
import pe.edu.unaj.amoblador3d.model.FSHFWallSide;
import pe.edu.unaj.amoblador3d.report.FSHFProjectReport;

class FSHFEstimateTest {
    private static FSHFProject room() {
        FSHFProject p = new FSHFProject();
        p.room = new FSHFRoomDims(4, 3, 2.5, 0.15);
        p.openings.add(new FSHFOpening("window-1", FSHFOpeningType.WINDOW, FSHFWallSide.NORTE, 1, 1.2, 1, 0.9));
        p.openings.add(new FSHFOpening("door-1", FSHFOpeningType.DOOR, FSHFWallSide.SUR, 1, 0.9, 2.1, 0));
        return p;
    }

    @Test
    void metradoDescuentaVentanasYPuertas() {
        FSHFQuantities q = FSHFQuantities.of(room());
        assertEquals(12, q.floorArea(), 1e-9);
        assertEquals(4 * 2.5 - 1.2, q.wallNet().get(FSHFWallSide.NORTE), 1e-9);
        assertEquals(4 * 2.5 - 0.9 * 2.1, q.wallNet().get(FSHFWallSide.SUR), 1e-9);
        assertEquals(3 * 2.5, q.wallNet().get(FSHFWallSide.ESTE), 1e-9);
        assertEquals(14, q.perimeter(), 1e-9);
        assertEquals(14 - 0.9, q.baseboard(), 1e-9);
        assertEquals(1.2 + 0.9 * 2.1, q.openingsArea(), 1e-9);
    }

    @Test
    void laPinturaSeCompraPorCuartosDeGalon() {
        FSHFPurchaseSpec paint = FSHFPurchaseSpec.of(FSHFSurfaceMaterial.PINTURA);

        assertEquals(2, paint.quantity(35), 1e-9);

        assertEquals(2.25, paint.quantity(36), 1e-9);
        assertEquals(0, paint.quantity(0), 1e-9);
    }

    @Test
    void losPisosLlevanDiezPorCientoDeDesperdicio() {
        assertEquals(13.2, FSHFPurchaseSpec.of(FSHFSurfaceMaterial.PORCELANATO).quantity(12), 1e-9);

        assertEquals(3, FSHFPurchaseSpec.of(FSHFSurfaceMaterial.PAPEL_TAPIZ).quantity(12), 1e-9);
    }

    @Test
    void paredesIgualesSeAgrupanYElPisoVaAparte() {
        FSHFProject p = room();
        p.walls.get(FSHFWallSide.ESTE).material = FSHFSurfaceMaterial.LADRILLO;
        p.walls.get(FSHFWallSide.ESTE).color = "#a4553a";
        FSHFEstimate e = FSHFEstimate.of(p, new FSHFCatalog(), new FSHFPriceList());
        List<FSHFEstimate.Line> finishes = e.lines().stream().filter(l -> l.section() == FSHFEstimate.Section.FINISHES).toList();

        assertEquals(4, finishes.size());
        FSHFEstimate.Line paint = finishes.stream().filter(l -> l.priceKey().equals(FSHFPriceList.Key.material(FSHFSurfaceMaterial.PINTURA)))
                .findFirst().orElseThrow();
        assertTrue(paint.detail().startsWith("Paredes norte, sur, oeste"), paint.detail());
        double area = (4 * 2.5 - 1.2) + (4 * 2.5 - 0.9 * 2.1) + 3 * 2.5;
        assertEquals(FSHFPurchaseSpec.of(FSHFSurfaceMaterial.PINTURA).quantity(area), paint.quantity(), 1e-9);
        assertEquals("gal", paint.unit());
    }

    @Test
    void elTotalUsaLosPreciosDelUsuario() {
        FSHFDesign d = new FSHFDesign(new FSHFCatalog());
        d.loadDefault();
        d.addFurniture("silla");
        d.addFurniture("silla");
        FSHFPriceList prices = new FSHFPriceList();
        prices.set(FSHFPriceList.Key.furniture("silla"), 100);
        FSHFEstimate e = FSHFEstimate.of(d.snapshot(), new FSHFCatalog(), prices);
        FSHFEstimate.Line chairs = e.lines().stream().filter(l -> l.section() == FSHFEstimate.Section.FURNITURE).findFirst().orElseThrow();
        assertEquals(2, chairs.quantity(), 1e-9);
        assertEquals(200, chairs.subtotal(), 1e-9);
        double sum = e.lines().stream().mapToDouble(FSHFEstimate.Line::subtotal).sum();
        assertEquals(Math.round(sum * 100) / 100.0, e.total(), 1e-9);
        assertThrows(IllegalArgumentException.class, () -> prices.set(FSHFPriceList.BASEBOARD, -1));
    }

    @Test
    void elInformePdfSeGenera(@TempDir Path dir) throws Exception {
        FSHFDesign d = new FSHFDesign(new FSHFCatalog());
        d.loadDefault();
        d.addFurniture("cama");
        FSHFProject p = d.snapshot();
        Path file = dir.resolve("informe.pdf");
        new FSHFProjectReport("Dormitorio de prueba ñandú → ok", p, FSHFEstimate.of(p, new FSHFCatalog(), new FSHFPriceList()),
                null, null, d::label).write(file);
        byte[] bytes = Files.readAllBytes(file);
        assertTrue(bytes.length > 1000);
        assertEquals("%PDF", new String(bytes, 0, 4));
    }
}
