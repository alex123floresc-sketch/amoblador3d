package pe.edu.unaj.amoblador3d.estimate;

import java.util.HashMap;
import java.util.Map;
import pe.edu.unaj.amoblador3d.model.SurfaceMaterial;

/**
 * Precios unitarios en soles (S/). Empieza con precios referenciales de Lima y el usuario
 * puede cambiarlos desde el presupuesto; los cambios se guardan en la base de datos
 * (ver {@code persistence.PriceRepository}) y sirven para todos los proyectos.
 */
public final class PriceList {
    /** Qué se cotiza: un acabado, un mueble del catálogo u otro rubro (el zócalo). */
    public enum Kind {
        MATERIAL("material"), FURNITURE("furniture"), EXTRA("extra");

        private final String id;

        Kind(String id) {
            this.id = id;
        }

        /** Texto que se guarda en la base de datos. */
        public String id() {
            return id;
        }

        public static Kind fromId(String id) {
            for (Kind k : values()) if (k.id.equals(id)) return k;
            throw new IllegalArgumentException("Tipo de precio desconocido: " + id);
        }
    }

    /** @param item id del material, del mueble ("cama") o del rubro ("zocalo") */
    public record Key(Kind kind, String item) {
        public static Key material(SurfaceMaterial m) {
            return new Key(Kind.MATERIAL, m.id());
        }

        public static Key furniture(String type) {
            return new Key(Kind.FURNITURE, type);
        }
    }

    public static final Key BASEBOARD = new Key(Kind.EXTRA, "zocalo");

    private static final Map<Key, Double> DEFAULTS = new HashMap<>();

    static {
        // Acabados: por galón, rollo o m² (ver PurchaseSpec).
        material(SurfaceMaterial.PINTURA, 65);
        material(SurfaceMaterial.SATINADO, 85);
        material(SurfaceMaterial.PAPEL_TAPIZ, 45);
        material(SurfaceMaterial.LADRILLO, 55);
        material(SurfaceMaterial.PIEDRA, 70);
        material(SurfaceMaterial.CONCRETO, 60);
        material(SurfaceMaterial.MADERA, 90);
        material(SurfaceMaterial.CERAMICO, 35);
        material(SurfaceMaterial.MARMOL, 180);
        material(SurfaceMaterial.PORCELANATO, 60);
        material(SurfaceMaterial.ALFOMBRA, 40);
        // Muebles paramétricos, por unidad. Los modelos 3D empiezan en 0: el usuario pone su precio.
        furniture("cama", 900);
        furniture("mesa", 450);
        furniture("silla", 120);
        furniture("closet", 1200);
        furniture("escritorio", 400);
        DEFAULTS.put(BASEBOARD, 12.0); // por metro lineal
    }

    private static void material(SurfaceMaterial m, double price) {
        DEFAULTS.put(Key.material(m), price);
    }

    private static void furniture(String type, double price) {
        DEFAULTS.put(Key.furniture(type), price);
    }

    /** Precio que se usa si el usuario no lo cambió. */
    public static double defaultPrice(Key key) {
        return DEFAULTS.getOrDefault(key, 0.0);
    }

    private final Map<Key, Double> custom = new HashMap<>();

    public double get(Key key) {
        Double p = custom.get(key);
        return p != null ? p : defaultPrice(key);
    }

    public void set(Key key, double price) {
        if (!Double.isFinite(price) || price < 0) throw new IllegalArgumentException("El precio no puede ser negativo.");
        custom.put(key, price);
    }

    public boolean isCustom(Key key) {
        return custom.containsKey(key);
    }

    /** Precios cambiados por el usuario. */
    public Map<Key, Double> custom() {
        return Map.copyOf(custom);
    }

    public void resetAll() {
        custom.clear();
    }
}
