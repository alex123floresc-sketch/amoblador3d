package pe.edu.unaj.amoblador3d.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.geometry.Bounds;
import javafx.scene.Group;
import org.junit.jupiter.api.Test;
import pe.edu.unaj.amoblador3d.catalog.Catalog;
import pe.edu.unaj.amoblador3d.catalog.CatalogEntry;
import pe.edu.unaj.amoblador3d.catalog.CustomDesign;
import pe.edu.unaj.amoblador3d.model.Furniture;

class FurnitureShapesTest {
    private static final double TOL = 2e-3;

    @Test
    void cadaMuebleParametricoSePuedeAbrirEnElDisenador() {
        for (CatalogEntry e : new Catalog().list(CatalogEntry.Kind.PARAMETRIC)) {
            CatalogEntry.Size s = e.defaults();
            Furniture f = new Furniture("x", e.id(), 0, 0, 0, s.width(), s.depth(), s.height(), e.defaultColor());
            CustomDesign d = FurnitureShapes.fromParametric(f, e.name());
            assertNotNull(d, e.id());
            assertNull(d.validate(), e.id() + ": " + d.validate());
            assertTrue(d.uses(CustomDesign.Paint.MAIN), e.id() + " debería tener piezas del color principal");
            CustomDesign.Bounds b = d.bounds();
            // La copia mide lo mismo que el mueble original.
            assertEquals(s.width(), b.width(), 0.03, e.id());
            assertEquals(s.depth(), b.depth(), 0.03, e.id());
            assertEquals(s.height(), b.height(), 0.03, e.id());
        }
    }

    @Test
    void unDisenoColocadoMideExactamenteLoDelMueble() {
        CustomDesign d = FurnitureShapes.fromParametric(new Furniture("x", "mesa", 0, 0, 0, 1.2, 0.8, 0.75, "#a0764f"), "Mesa");
        d.id = "diseno-mesa";
        Furniture placed = new Furniture("m", d.id, 0, 0, 0, 2.0, 1.0, 0.9, null);
        Group g = FurnitureShapes.fit(d, placed);
        Bounds b = g.getBoundsInParent();
        assertEquals(-1.0, b.getMinX(), TOL);
        assertEquals(1.0, b.getMaxX(), TOL);
        assertEquals(0, b.getMinY(), TOL);
        assertEquals(0.9, b.getMaxY(), TOL);
        assertEquals(-0.5, b.getMinZ(), TOL);
        assertEquals(0.5, b.getMaxZ(), TOL);
    }

    @Test
    void unMuebleQueNoEsParametricoNoSeConvierte() {
        assertNull(FurnitureShapes.fromParametric(new Furniture("x", "sofa-3d", 0, 0, 0, 1, 1, 1, null), "Sofá"));
    }
}
