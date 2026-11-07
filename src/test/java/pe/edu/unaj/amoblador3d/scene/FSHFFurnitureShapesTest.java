package pe.edu.unaj.amoblador3d.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.geometry.Bounds;
import javafx.scene.Group;
import org.junit.jupiter.api.Test;
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalog;
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalogEntry;
import pe.edu.unaj.amoblador3d.catalog.FSHFCustomDesign;
import pe.edu.unaj.amoblador3d.model.FSHFFurniture;

class FSHFFurnitureShapesTest {
    private static final double TOL = 2e-3;

    @Test
    void cadaMuebleParametricoSePuedeAbrirEnElDisenador() {
        for (FSHFCatalogEntry e : new FSHFCatalog().list(FSHFCatalogEntry.Kind.PARAMETRIC)) {
            FSHFCatalogEntry.Size s = e.defaults();
            FSHFFurniture f = new FSHFFurniture("x", e.id(), 0, 0, 0, s.width(), s.depth(), s.height(), e.defaultColor());
            FSHFCustomDesign d = FSHFFurnitureShapes.fromParametric(f, e.name());
            if (FSHFCatalog.isStair(e.id())) {
                assertNull(d, e.id() + ": las escaleras no se abren en el Diseñador");
                continue;
            }
            assertNotNull(d, e.id());
            assertNull(d.validate(), e.id() + ": " + d.validate());
            assertTrue(d.uses(FSHFCustomDesign.Paint.MAIN), e.id() + " debería tener piezas del color principal");
            FSHFCustomDesign.Bounds b = d.bounds();
            // La copia mide lo mismo que el mueble original.
            assertEquals(s.width(), b.width(), 0.03, e.id());
            assertEquals(s.depth(), b.depth(), 0.03, e.id());
            assertEquals(s.height(), b.height(), 0.03, e.id());
        }
    }

    @Test
    void unDisenoColocadoMideExactamenteLoDelMueble() {
        FSHFCustomDesign d = FSHFFurnitureShapes.fromParametric(new FSHFFurniture("x", "mesa", 0, 0, 0, 1.2, 0.8, 0.75, "#a0764f"), "Mesa");
        d.id = "diseno-mesa";
        FSHFFurniture placed = new FSHFFurniture("m", d.id, 0, 0, 0, 2.0, 1.0, 0.9, null);
        Group g = FSHFFurnitureShapes.fit(d, placed);
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
        assertNull(FSHFFurnitureShapes.fromParametric(new FSHFFurniture("x", "sofa-3d", 0, 0, 0, 1, 1, 1, null), "Sofá"));
    }
}
