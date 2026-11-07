package pe.edu.unaj.amoblador3d.model;

public final class FSHFLight {
    public String id;
    public FSHFLightType type;
    public String color;
    public double intensity;
    public boolean castShadow = true;

    public double x, y, z;
    public double targetX, targetZ, angle, penumbra;
    public String windowId;
    public double elevation, deviation;

    public FSHFLight(String id, FSHFLightType type) {
        this.id = id;
        this.type = type;
        this.color = type.defaultColor;
        this.intensity = type.defaultIntensity;
    }

    public FSHFLight copy() {
        FSHFLight l = new FSHFLight(id, type);
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
