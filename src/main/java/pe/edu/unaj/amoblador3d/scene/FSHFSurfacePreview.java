package pe.edu.unaj.amoblador3d.scene;

import javafx.scene.image.Image;
import pe.edu.unaj.amoblador3d.model.FSHFSurfaceMaterial;

/**
 * Muestra pequeña del material de una superficie para la interfaz.
 */
public final class FSHFSurfacePreview {
    private FSHFSurfacePreview() {}

    public static Image of(FSHFSurfaceMaterial material, String color) {
        return FSHFSurfaceTextures.material(material, color).getDiffuseMap();
    }
}
