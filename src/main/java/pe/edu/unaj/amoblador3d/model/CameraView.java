package pe.edu.unaj.amoblador3d.model;

/**
 * Vista de cámara guardada en el proyecto ("Vista desde la cama", "Planta completa"...).
 * Según el tipo, los números significan:
 * <ul>
 *   <li>ORBIT (vista 3D): x, y, z = punto que se mira; yaw y pitch = giro e inclinación (radianes);
 *       distance = distancia de la cámara a ese punto.</li>
 *   <li>PLAN (planta): x, z = centro; distance = metros visibles de alto.</li>
 *   <li>WALK (recorrido): x, y, z = posición de los ojos; yaw y pitch = hacia dónde se mira.</li>
 * </ul>
 */
public record CameraView(String name, Kind kind, double x, double y, double z, double yaw, double pitch, double distance) {
    public static final int MAX_NAME = 60;

    public enum Kind {
        ORBIT("3d"), PLAN("plan"), WALK("walk");

        private final String id;

        Kind(String id) {
            this.id = id;
        }

        /** Texto que se guarda en los archivos y en la base de datos. */
        public String id() {
            return id;
        }

        public static Kind fromId(String id) {
            for (Kind k : values()) if (k.id.equals(id)) return k;
            throw new IllegalArgumentException("Tipo de vista desconocido: " + id);
        }
    }

    /** true si todos los números son finitos y el nombre no está vacío. */
    public boolean isValid() {
        return name != null && !name.isBlank() && name.length() <= MAX_NAME && kind != null
                && Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
                && Double.isFinite(yaw) && Double.isFinite(pitch) && Double.isFinite(distance);
    }
}
