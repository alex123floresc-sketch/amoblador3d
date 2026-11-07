package pe.edu.unaj.amoblador3d.estimate;

import java.util.HashMap;
import java.util.Map;
import pe.edu.unaj.amoblador3d.model.FSHFSurfaceMaterial;

public final class FSHFPriceList {
    public enum Kind {
        MATERIAL("material"), FURNITURE("furniture"), EXTRA("extra");

        private final String id;

        Kind(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public static Kind fromId(String id) {
            for (Kind k : values()) if (k.id.equals(id)) return k;
            throw new IllegalArgumentException("Tipo de precio desconocido: " + id);
        }
    }

    public record Key(Kind kind, String item) {
        public static Key material(FSHFSurfaceMaterial m) {
            return new Key(Kind.MATERIAL, m.id());
        }

        public static Key furniture(String type) {
            return new Key(Kind.FURNITURE, type);
        }
    }

    public static final Key BASEBOARD = new Key(Kind.EXTRA, "zocalo");

    private static final Map<Key, Double> DEFAULTS = new HashMap<>();

    static {
        material(FSHFSurfaceMaterial.PINTURA, 65);
        material(FSHFSurfaceMaterial.SATINADO, 85);
        material(FSHFSurfaceMaterial.PAPEL_TAPIZ, 45);
        material(FSHFSurfaceMaterial.LADRILLO, 55);
        material(FSHFSurfaceMaterial.PIEDRA, 70);
        material(FSHFSurfaceMaterial.CONCRETO, 60);
        material(FSHFSurfaceMaterial.MADERA, 90);
        material(FSHFSurfaceMaterial.CERAMICO, 35);
        material(FSHFSurfaceMaterial.MARMOL, 180);
        material(FSHFSurfaceMaterial.PORCELANATO, 60);
        material(FSHFSurfaceMaterial.ALFOMBRA, 40);

        furniture("cama", 900);
        furniture("mesa", 450);
        furniture("silla", 120);
        furniture("closet", 1200);
        furniture("escritorio", 400);
        furniture("mesa-noche", 180);
        furniture("comoda", 600);
        furniture("sillon", 1500);
        furniture("rack-tv", 2500);
        furniture("librero", 350);
        furniture("cocina", 1800);
        furniture("refrigeradora", 2200);
        furniture("inodoro", 350);
        furniture("lavatorio", 250);
        furniture("escalera", 3500);
        furniture("escalera-l", 5200);
        furniture("escalera-caracol", 4800);
        furniture("baranda", 300);
        DEFAULTS.put(BASEBOARD, 12.0);
    }

    private static void material(FSHFSurfaceMaterial m, double price) {
        DEFAULTS.put(Key.material(m), price);
    }

    private static void furniture(String type, double price) {
        DEFAULTS.put(Key.furniture(type), price);
    }

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

    public Map<Key, Double> custom() {
        return Map.copyOf(custom);
    }

    public void resetAll() {
        custom.clear();
    }
}
