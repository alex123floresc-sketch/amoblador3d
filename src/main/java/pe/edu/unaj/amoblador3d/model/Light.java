package pe.edu.unaj.amoblador3d.model;

/**
 * Luz agregada por el usuario. Según el tipo se usan unos campos u otros:
 * <ul>
 *   <li>POINT: x, y, z.</li>
 *   <li>SPOT: x, y, z, targetX, targetZ (punto del piso al que apunta), angle (apertura en grados), penumbra (0..1).</li>
 *   <li>SUN: windowId (ventana por la que entra), elevation (altura del sol) y deviation (desvío lateral), en grados.</li>
 * </ul>
 */
public final class Light {
    public String id;
    public LightType type;
    public String color;
    public double intensity;
    /** Se conserva por compatibilidad con los archivos de la versión anterior (JavaFX no dibuja sombras). */
    public boolean castShadow = true;

    public double x, y, z;
    public double targetX, targetZ, angle, penumbra;
    public String windowId;
    public double elevation, deviation;

    public Light(String id, LightType type) {
        this.id = id;
        this.type = type;
        this.color = type.defaultColor;
        this.intensity = type.defaultIntensity;
    }

    public Light copy() {
        Light l = new Light(id, type);
        l.color = color;
        l.intensity = intensity;
        l.castShadow = castShadow;
        l.x = x;
        l.y = y;
        l.z = z;
        l.targetX = targetX;
        l.targetZ = targetZ;
        l.angle = angle;
        l.penumbra = penumbra;
        l.windowId = windowId;
        l.elevation = elevation;
        l.deviation = deviation;
        return l;
    }
}
