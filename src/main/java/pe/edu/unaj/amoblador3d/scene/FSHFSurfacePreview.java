package pe.edu.unaj.amoblador3d.scene;

import javafx.scene.image.Image;
import pe.edu.unaj.amoblador3d.model.FSHFSurfaceMaterial;

/** Miniaturas de los materiales de paredes y piso para la interfaz (la misma textura que en 3D). */
public final class FSHFSurfacePreview {
    private FSHFSurfacePreview() {}

    /** Imagen de una repetición de la textura ({@code FSHFSurfaceTextures.tileSize} metros de lado). */
    public static Image of(FSHFSurfaceMaterial material, String color) {
        return FSHFSurfaceTextures.material(material, color).getDiffuseMap();
    }
}
