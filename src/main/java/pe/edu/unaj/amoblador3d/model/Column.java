package pe.edu.unaj.amoblador3d.model;

/**
 * Bloque macizo del piso al techo dentro de la habitación: una columna, un muro interno o el
 * recorte de una esquina (que convierte la habitación en una "L"). Es un rectángulo alineado
 * a los ejes; los muebles no lo atraviesan y no puede tapar ventanas ni puertas.
 * <ul>
 *   <li>x, z: centro en planta; width: medida en X; depth: medida en Z.</li>
 *   <li>corner: solo en los recortes, la esquina que ocupan (el recorte siempre toca dos paredes).</li>
 *   <li>material y color: su acabado (los de pared).</li>
 * </ul>
 */
public final class Column {
    public enum Kind {
        COLUMN("column", "Columna", 0.3, 0.3),
        WALL("wall", "Muro interno", 1.5, 0.12),
        NOTCH("notch", "Recorte en L", 1.2, 1.0);

        private final String id;
        private final String label;
        final double defaultWidth, defaultDepth;

        Kind(String id, String label, double width, double depth) {
            this.id = id;
            this.label = label;
            this.defaultWidth = width;
            this.defaultDepth = depth;
        }

        /** Texto que se guarda en los archivos y en la base de datos. */
        public String id() {
            return id;
        }

        public String label() {
            return label;
        }

        public double defaultWidth() {
            return defaultWidth;
        }

        public double defaultDepth() {
            return defaultDepth;
        }

        public static Kind fromId(String id) {
            for (Kind k : values()) if (k.id.equals(id)) return k;
            throw new IllegalArgumentException("Tipo de columna desconocido: " + id);
        }
    }

    /** Esquinas de la habitación (para los recortes en L). */
    public enum Corner {
        NOROESTE("noroeste", "Noroeste", false, false),
        NORESTE("noreste", "Noreste", true, false),
        SUROESTE("suroeste", "Suroeste", false, true),
        SURESTE("sureste", "Sureste", true, true);

        private final String id;
        private final String label;
        /** true si la esquina está en el lado este (x = ancho) / sur (z = largo). */
        public final boolean east, south;

        Corner(String id, String label, boolean east, boolean south) {
            this.id = id;
            this.label = label;
            this.east = east;
            this.south = south;
        }

        public String id() {
            return id;
        }

        public String label() {
            return label;
        }

        public static Corner fromId(String id) {
            for (Corner c : values()) if (c.id.equals(id)) return c;
            throw new IllegalArgumentException("Esquina desconocida: " + id);
        }
    }

    public String id;
    public Kind kind;
    public Corner corner;
    public double x, z, width, depth;
    public SurfaceMaterial material = Surface.DEFAULT_WALL;
    public String color = Surface.DEFAULT_WALL.defaultColor();

    public Column(String id, Kind kind, double x, double z, double width, double depth) {
        this.id = id;
        this.kind = kind;
        this.x = x;
        this.z = z;
        this.width = width;
        this.depth = depth;
    }

    public Column copy() {
        Column c = new Column(id, kind, x, z, width, depth);
        c.corner = corner;
        c.material = material;
        c.color = color;
        return c;
    }

    public Footprint footprint() {
        return new Footprint(x, z, width / 2, depth / 2, 0);
    }

    public double minX() {
        return x - width / 2;
    }

    public double maxX() {
        return x + width / 2;
    }

    public double minZ() {
        return z - depth / 2;
    }

    public double maxZ() {
        return z + depth / 2;
    }

    /** Lleva un recorte a su esquina (el centro depende de la esquina y las medidas). */
    public void placeInCorner(RoomDims r) {
        x = corner.east ? r.width() - width / 2 : width / 2;
        z = corner.south ? r.length() - depth / 2 : depth / 2;
    }
}
