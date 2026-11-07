package pe.edu.unaj.amoblador3d.scene;

import javafx.scene.image.Image;
import pe.edu.unaj.amoblador3d.model.FSHFSurfaceMaterial;

public final class FSHFSurfacePreview {
    private FSHFSurfacePreview() {}

    public static Image of(FSHFSurfaceMaterial material, String color) {
        return FSHFSurfaceTextures.material(material, color).getDiffuseMap();
    }
}
