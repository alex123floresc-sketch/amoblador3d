package pe.edu.unaj.amoblador3d.scene;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.geometry.Bounds;
import javafx.scene.Group;
import org.junit.jupiter.api.Test;
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalog;
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalogEntry;
import pe.edu.unaj.amoblador3d.model.FSHFFurniture;

class FSHFParametricBuilderTest {
    private static final double TOL = 1e-6;

    @Test
    void todosLosMueblesCabenEnSusMedidas() {
        FSHFCatalog catalog = new FSHFCatalog();
        for (FSHFCatalogEntry e : catalog.list(FSHFCatalogEntry.Kind.PARAMETRIC)) {
            double[][] sizes = {
                {e.defaults().width(), e.defaults().depth(), e.defaults().height()},
                {e.widthRange().min(), e.depthRange().min(), e.heightRange().min()},
                {e.widthRange().max(), e.depthRange().max(), e.heightRange().max()},
                {e.widthRange().min(), e.depthRange().max(), e.heightRange().min()},
                {e.widthRange().max(), e.depthRange().min(), e.heightRange().max()},
            };
            for (double[] s : sizes) {
                FSHFFurniture f = new FSHFFurniture("x", e.id(), 0, 0, 0, s[0], s[1], s[2], e.defaultColor());
                Group g = FSHFParametricBuilder.build(f);
                assertNotNull(g, e.id() + " no tiene constructor en ParametricBuilder");
                Bounds b = g.getBoundsInLocal();
                String what = e.id() + " " + s[0] + " x " + s[1] + " x " + s[2] + ": " + b;
                assertTrue(b.getMinX() >= -s[0] / 2 - TOL && b.getMaxX() <= s[0] / 2 + TOL, what);
                assertTrue(b.getMinY() >= -TOL && b.getMaxY() <= s[2] + TOL, what);
                assertTrue(b.getMinZ() >= -s[1] / 2 - TOL && b.getMaxZ() <= s[1] / 2 + TOL, what);

                assertTrue(b.getWidth() > s[0] * 0.9 && b.getDepth() > s[1] * 0.85 && b.getHeight() > s[2] * 0.95, what);
            }
        }
    }
}
