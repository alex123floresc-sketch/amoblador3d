package pe.edu.unaj.amoblador3d.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.unaj.amoblador3d.catalog.Catalog;
import pe.edu.unaj.amoblador3d.design.Design;
import pe.edu.unaj.amoblador3d.model.Furniture;

/** Asas de la vista 3D: estirar un lado deja quieto el opuesto, el giro se pega cada 15°. */
class FurnitureHandlesTest {
    private Design design;
    private ViewCamera cam;
    private Furniture table;

    @BeforeEach
    void setUp() {
        design = new Design(new Catalog());
        design.loadDefault();
        table = design.addFurniture("mesa").value();
        cam = new ViewCamera();
        cam.setViewport(1200, 800);
        cam.frame(0, 0, 0, 4, 2.5, 3.5);
    }

    private Point2D screen(double x, double z) {
        Point2D p = cam.project(new Point3D(x, 0, z));
        assertNotNull(p);
        return p;
    }

    @Test
    void lasAsasSeEncuentranDondeSeDibujan() {
        for (FurnitureHandles.Handle h : FurnitureHandles.of(table, false)) {
            Point2D p = cam.project(h.at());
            assertEquals(h.kind(), FurnitureHandles.hit(cam, table, p.getX(), p.getY(), false));
        }
    }

    @Test
    void estirarElLadoDerechoDejaQuietoElIzquierdo() {
        double left = table.x - table.width / 2, z = table.z;
        FurnitureHandles h = new FurnitureHandles();
        Point2D start = screen(table.x + table.width / 2, z);
        h.begin(FurnitureHandles.Kind.RIGHT, table, start.getX(), start.getY());
        Point2D to = screen(table.x + table.width / 2 + 0.3, z);
        assertNotNull(h.drag(design, cam, to.getX(), to.getY(), false));
        assertTrue(h.end());
        assertEquals(1.5, table.width, 0.02);
        assertEquals(left, table.x - table.width / 2, 0.011);
    }

    @Test
    void elGiroSePegaCada15Grados() {
        FurnitureHandles h = new FurnitureHandles();
        h.begin(FurnitureHandles.Kind.ROTATE, table, 0, 0);
        // Ratón a la derecha y un poco adelante del centro: unos 70° -> se pega a 75°.
        double a = Math.toRadians(70);
        Point2D to = screen(table.x + Math.sin(a), table.z + Math.cos(a));
        assertNotNull(h.drag(design, cam, to.getX(), to.getY(), false));
        assertEquals(75, table.rotation, 1e-9);
        h.end();
    }

    @Test
    void unaMedidaQueChocaNoSeAplica() {
        double before = table.width;
        FurnitureHandles h = new FurnitureHandles();
        h.begin(FurnitureHandles.Kind.RIGHT, table, 0, 0);
        // Hasta la pared este y más allá: el lado no puede salirse de la habitación.
        Point2D to = screen(5.5, table.z);
        h.drag(design, cam, to.getX(), to.getY(), false);
        assertTrue(table.x + table.width / 2 <= design.room().width() + 1e-9);
        assertFalse(table.width < before);
        h.end();
    }
}
