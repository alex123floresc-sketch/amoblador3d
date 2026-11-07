package pe.edu.unaj.amoblador3d.model;

public final class FSHFBaseboard {
    public static final String ID = "zocalo";
    public static final double DEFAULT_HEIGHT = 0.08, MAX_HEIGHT = 0.2;
    public static final String DEFAULT_COLOR = "#f7f5f0";

    public double height = DEFAULT_HEIGHT;
    public String color = DEFAULT_COLOR;

    public FSHFBaseboard copy() {
        FSHFBaseboard b = new FSHFBaseboard();
        b.height = height;
        b.color = color;
        return b;
    }

    public boolean isDefault() {
        return height == DEFAULT_HEIGHT && DEFAULT_COLOR.equalsIgnoreCase(color);
    }
}
