package pe.edu.unaj.amoblador3d.model;

public final class FSHFFurniture {
    public String id;
    public String type;
    public double x, z, rotation;
    public double width, depth, height;
    public String color;
    public String color2;
    public FSHFFinish finish;
    public boolean locked;

    public FSHFFurniture(String id, String type, double x, double z, double rotation,
                     double width, double depth, double height, String color) {
        this.id = id;
        this.type = type;
        this.x = x;
        this.z = z;
        this.rotation = rotation;
        this.width = width;
        this.depth = depth;
        this.height = height;
        this.color = color;
    }

    public FSHFFurniture copy() {
        FSHFFurniture f = new FSHFFurniture(id, type, x, z, rotation, width, depth, height, color);
        f.color2 = color2;
        f.finish = finish;
        f.locked = locked;
        return f;
    }

    public String appearance() {
        return type + "|" + width + "|" + depth + "|" + height + "|" + color + "|" + color2 + "|" + finish;
    }
}
