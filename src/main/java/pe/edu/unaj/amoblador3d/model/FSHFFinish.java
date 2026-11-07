package pe.edu.unaj.amoblador3d.model;

public enum FSHFFinish {
    MATE("mate", "Mate", 0.04, 8),
    SATINADO("satinado", "Satinado", 0.22, 22),
    BRILLANTE("brillante", "Brillante", 0.7, 48),
    METALICO("metalico", "Metálico", 0.95, 30);

    private final String id;
    private final String label;
    private final double specular;
    private final double power;

    FSHFFinish(String id, String label, double specular, double power) {
        this.id = id;
        this.label = label;
        this.specular = specular;
        this.power = power;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public double specular() {
        return specular;
    }

    public double power() {
        return power;
    }

    public static FSHFFinish fromId(String id) {
        for (FSHFFinish f : values()) if (f.id.equals(id)) return f;
        return null;
    }
}
