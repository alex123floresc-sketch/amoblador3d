package pe.edu.unaj.amoblador3d.estimate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pe.edu.unaj.amoblador3d.catalog.Catalog;
import pe.edu.unaj.amoblador3d.design.Design;
import pe.edu.unaj.amoblador3d.model.Opening;
import pe.edu.unaj.amoblador3d.model.OpeningType;
import pe.edu.unaj.amoblador3d.model.Project;
import pe.edu.unaj.amoblador3d.model.RoomDims;
import pe.edu.unaj.amoblador3d.model.SurfaceMaterial;
import pe.edu.unaj.amoblador3d.model.WallSide;
import pe.edu.unaj.amoblador3d.report.ProjectReport;

class EstimateTest {
    /** Habitación de 4 x 3 x 2,5 m, con una ventana de 1,2 x 1 en el norte y una puerta de 0,9 x 2,1 en el sur. */
    private static Project room() {
        Project p = new Project();
        p.room = new RoomDims(4, 3, 2.5, 0.15);
        p.openings.add(new Opening("window-1", OpeningType.WINDOW, WallSide.NORTE, 1, 1.2, 1, 0.9));
        p.openings.add(new Opening("door-1", OpeningType.DOOR, WallSide.SUR, 1, 0.9, 2.1, 0));
        return p;
    }

    @Test
    void metradoDescuentaVentanasYPuertas() {
        Quantities q = Quantities.of(room());
        assertEquals(12, q.floorArea(), 1e-9);
        assertEquals(4 * 2.5 - 1.2, q.wallNet().get(WallSide.NORTE), 1e-9);
        assertEquals(4 * 2.5 - 0.9 * 2.1, q.wallNet().get(WallSide.SUR), 1e-9);
        assertEquals(3 * 2.5, q.wallNet().get(WallSide.ESTE), 1e-9);
        assertEquals(14, q.perimeter(), 1e-9);
        assertEquals(14 - 0.9, q.baseboard(), 1e-9);
        assertEquals(1.2 + 0.9 * 2.1, q.openingsArea(), 1e-9);
    }

    @Test
    void laPinturaSeCompraPorCuartosDeGalon() {
        PurchaseSpec paint = PurchaseSpec.of(SurfaceMaterial.PINTURA);
        // 35 m² con 2 manos = 70 m² de pintado = 2 galones exactos (sin redondear de más).
        assertEquals(2, paint.quantity(35), 1e-9);
        // 36 m² -> 2,06 galones -> 2,25.
        assertEquals(2.25, paint.quantity(36), 1e-9);
        assertEquals(0, paint.quantity(0), 1e-9);
    }

    @Test
    void losPisosLlevanDiezPorCientoDeDesperdicio() {
        assertEquals(13.2, PurchaseSpec.of(SurfaceMaterial.PORCELANATO).quantity(12), 1e-9);
        // 12 m² + 10 % = 13,2 m² / 5,3 = 2,49 -> 3 rollos.
        assertEquals(3, PurchaseSpec.of(SurfaceMaterial.PAPEL_TAPIZ).quantity(12), 1e-9);
    }

    @Test
    void paredesIgualesSeAgrupanYElPisoVaAparte() {
        Project p = room();
        p.walls.get(WallSide.ESTE).material = SurfaceMaterial.LADRILLO;
        p.walls.get(WallSide.ESTE).color = "#a4553a";
        Estimate e = Estimate.of(p, new Catalog(), new PriceList());
        List<Estimate.Line> finishes = e.lines().stream().filter(l -> l.section() == Estimate.Section.FINISHES).toList();
        // piso de madera + pintura (norte, sur, oeste) + ladrillo (este)
        assertEquals(3, finishes.size());
        Estimate.Line paint = finishes.stream().filter(l -> l.priceKey().equals(PriceList.Key.material(SurfaceMaterial.PINTURA)))
                .findFirst().orElseThrow();
        assertTrue(paint.detail().startsWith("Paredes norte, sur, oeste"), paint.detail());
        double area = (4 * 2.5 - 1.2) + (4 * 2.5 - 0.9 * 2.1) + 3 * 2.5;
        assertEquals(PurchaseSpec.of(SurfaceMaterial.PINTURA).quantity(area), paint.quantity(), 1e-9);
        assertEquals("gal", paint.unit());
    }

    @Test
    void elTotalUsaLosPreciosDelUsuario() {
        Design d = new Design(new Catalog());
        d.loadDefault();
        d.addFurniture("silla");
        d.addFurniture("silla");
        PriceList prices = new PriceList();
        prices.set(PriceList.Key.furniture("silla"), 100);
        Estimate e = Estimate.of(d.snapshot(), new Catalog(), prices);
        Estimate.Line chairs = e.lines().stream().filter(l -> l.section() == Estimate.Section.FURNITURE).findFirst().orElseThrow();
        assertEquals(2, chairs.quantity(), 1e-9);
        assertEquals(200, chairs.subtotal(), 1e-9);
        double sum = e.lines().stream().mapToDouble(Estimate.Line::subtotal).sum();
        assertEquals(Math.round(sum * 100) / 100.0, e.total(), 1e-9);
        assertThrows(IllegalArgumentException.class, () -> prices.set(PriceList.BASEBOARD, -1));
    }

    @Test
    void elInformePdfSeGenera(@TempDir Path dir) throws Exception {
        Design d = new Design(new Catalog());
        d.loadDefault();
        d.addFurniture("cama");
        Project p = d.snapshot();
        Path file = dir.resolve("informe.pdf");
        new ProjectReport("Dormitorio de prueba ñandú → ok", p, Estimate.of(p, new Catalog(), new PriceList()),
                null, null, d::label).write(file);
        byte[] bytes = Files.readAllBytes(file);
        assertTrue(bytes.length > 1000);
        assertEquals("%PDF", new String(bytes, 0, 4));
    }
}
