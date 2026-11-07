package pe.edu.unaj.amoblador3d.model;

public record FSHFCameraView(String name, Kind kind, double x, double y, double z, double yaw, double pitch, double distance) {
    public static final int MAX_NAME = 60;

    public enum Kind {
        ORBIT("3d"), PLAN("plan"), WALK("walk");

        private final String id;

        Kind(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public static Kind fromId(String id) {
            for (Kind k : values()) if (k.id.equals(id)) return k;
            throw new IllegalArgumentException("Tipo de vista desconocido: " + id);
        }
    }

    public boolean isValid() {
        return name != null && !name.isBlank() && name.length() <= MAX_NAME && kind != null
                && Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
                && Double.isFinite(yaw) && Double.isFinite(pitch) && Double.isFinite(distance);
    }
}
