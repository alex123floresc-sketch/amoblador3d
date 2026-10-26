package pe.edu.unaj.amoblador3d.scene;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.geometry.Bounds;
import javafx.scene.Group;
import org.junit.jupiter.api.Test;
import pe.edu.unaj.amoblador3d.catalog.Catalog;
import pe.edu.unaj.amoblador3d.catalog.CatalogEntry;
import pe.edu.unaj.amoblador3d.model.Furniture;

/**
 * Regla de todos los muebles paramétricos: ocupan exactamente x ∈ [-ancho/2, ancho/2],
 * y ∈ [0, alto], z ∈ [-prof/2, prof/2] (si se salen, las colisiones no coinciden con lo que se ve).
 */
class ParametricBuilderTest {
    private static final double TOL = 1e-6;

    @Test
    void todosLosMueblesCabenEnSusMedidas() {
        Catalog catalog = new Catalog();
        for (CatalogEntry e : catalog.list(CatalogEntry.Kind.PARAMETRIC)) {
            double[][] sizes = {
                {e.defaults().width(), e.defaults().depth(), e.defaults().height()},
                {e.widthRange().min(), e.depthRange().min(), e.heightRange().min()},
                {e.widthRange().max(), e.depthRange().max(), e.heightRange().max()},
                {e.widthRange().min(), e.depthRange().max(), e.heightRange().min()},
                {e.widthRange().max(), e.depthRange().min(), e.heightRange().max()},
            };
            for (double[] s : sizes) {
                Furniture f = new Furniture("x", e.id(), 0, 0, 0, s[0], s[1], s[2], e.defaultColor());
                Group g = ParametricBuilder.build(f);
                assertNotNull(g, e.id() + " no tiene constructor en ParametricBuilder");
                Bounds b = g.getBoundsInLocal();
                String what = e.id() + " " + s[0] + " x " + s[1] + " x " + s[2] + ": " + b;
                assertTrue(b.getMinX() >= -s[0] / 2 - TOL && b.getMaxX() <= s[0] / 2 + TOL, what);
                assertTrue(b.getMinY() >= -TOL && b.getMaxY() <= s[2] + TOL, what);
                assertTrue(b.getMinZ() >= -s[1] / 2 - TOL && b.getMaxZ() <= s[1] / 2 + TOL, what);
                // Y que lo ocupe casi todo (si no, se vería más chico que su caja de colisión).
                assertTrue(b.getWidth() > s[0] * 0.9 && b.getDepth() > s[1] * 0.85 && b.getHeight() > s[2] * 0.95, what);
            }
        }
    }
}
