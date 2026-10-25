package pe.edu.unaj.amoblador3d.scene;

import javafx.scene.image.Image;
import pe.edu.unaj.amoblador3d.model.SurfaceMaterial;

/** Miniaturas de los materiales de paredes y piso para la interfaz (la misma textura que en 3D). */
public final class SurfacePreview {
    private SurfacePreview() {}

    /** Imagen de una repetición de la textura ({@code SurfaceTextures.tileSize} metros de lado). */
    public static Image of(SurfaceMaterial material, String color) {
        return SurfaceTextures.material(material, color).getDiffuseMap();
    }
}
