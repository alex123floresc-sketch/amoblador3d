package pe.edu.unaj.amoblador3d.design;

import pe.edu.unaj.amoblador3d.catalog.FSHFCatalog;
import pe.edu.unaj.amoblador3d.model.FSHFFurniture;
import pe.edu.unaj.amoblador3d.model.FSHFHome;
import pe.edu.unaj.amoblador3d.model.FSHFLight;
import pe.edu.unaj.amoblador3d.model.FSHFLightType;
import pe.edu.unaj.amoblador3d.model.FSHFOpening;
import pe.edu.unaj.amoblador3d.model.FSHFOpeningType;
import pe.edu.unaj.amoblador3d.model.FSHFProject;
import pe.edu.unaj.amoblador3d.model.FSHFRoomDims;
import pe.edu.unaj.amoblador3d.model.FSHFSurfaceMaterial;
import pe.edu.unaj.amoblador3d.model.FSHFWallSide;

/**
 * Construye la "Casa de ejemplo" de un solo piso según el plano de referencia
 * (cochera, comedor, cocina, dos estudios, sala, baño, pasillo y dos dormitorios).
 * Se usa para generar el archivo {@code /ejemplo/casa-ejemplo.amob3d} que viaja con la
 * aplicación; así el ejemplo se puede abrir, editar y mejorar como cualquier proyecto.
 */
public final class FSHFExampleHouse {
    private FSHFExampleHouse() {}

    private static final double T = 0.15;      // grosor de muro
    private static final double H = 2.5;       // altura de los ambientes

    public static FSHFHome build(FSHFCatalog catalog) {
        // Cada ambiente: nombre, posición (x,z) de su esquina, ancho(X) y largo(Z).
        FSHFProject cochera  = room(catalog, 6.0, 3.0, "#8d8f93", "#d9d9d9", "cochera");
        FSHFHome h = FSHFHome.of(cochera, "Cochera");
        h.spaces.get(0).x = 0;   h.spaces.get(0).z = 0;

        place(h, "Comedor",      6.3,  0.0, room(catalog, 3.5, 3.0, "#c9c3b9", "#f2efe9", "comedor"));
        place(h, "Cocina",      10.0,  0.0, room(catalog, 3.5, 2.8, "#d7d2c7", "#eceae3", "cocina"));
        place(h, "Estudio 1",   13.7,  0.0, room(catalog, 2.5, 2.0, "#c9a274", "#e3d5bf", "estudio"));
        place(h, "Sala",         0.0,  3.3, room(catalog, 5.0, 4.5, "#c9c3b9", "#f2efe9", "sala"));
        place(h, "Estudio 2",    5.3,  3.3, room(catalog, 2.5, 2.0, "#c9a274", "#e3d5bf", "estudio"));
        place(h, "Baño",         8.0,  3.3, room(catalog, 2.5, 1.8, "#d9e2e6", "#eef3f5", "bano"));
        place(h, "Pasillo",     10.7,  3.3, room(catalog, 1.5, 4.5, "#c9c3b9", "#f2efe9", "pasillo"));
        place(h, "Dormitorio 1",13.7,  2.3, room(catalog, 4.0, 3.5, "#c9a274", "#e3d5bf", "dormitorio"));
        place(h, "Dormitorio 2",13.7,  6.0, room(catalog, 4.0, 3.5, "#c9a274", "#e3d5bf", "dormitorio"));
        return h;
    }

    private static void place(FSHFHome h, String name, double x, double z, FSHFProject p) {
        h.spaces.add(new FSHFHome.Space(h.nextId(), name, round(x), round(z), p, 0, 0));
    }

    /** Crea un ambiente con sus medidas, acabados, una puerta, una ventana, luz y algún mueble. */
    private static FSHFProject room(FSHFCatalog catalog, double w, double l, String floorColor,
                                    String wallColor, String kind) {
        FSHFProject p = new FSHFProject();
        p.room = new FSHFRoomDims(w, l, H, T);
        p.floor.material = FSHFSurfaceMaterial.PORCELANATO;
        p.floor.color = floorColor;
        for (FSHFWallSide side : FSHFWallSide.values()) {
            p.walls.get(side).material = FSHFSurfaceMaterial.PINTURA;
            p.walls.get(side).color = wallColor;
        }
        // Puerta al sur y una ventana al norte (si caben).
        p.openings.add(new FSHFOpening("door-1", FSHFOpeningType.DOOR, FSHFWallSide.SUR,
                clamp(w / 2 - 0.45, 0.2, w - 1.1), 0.9, 2.1, 0));
        if (w >= 1.6) {
            p.openings.add(new FSHFOpening("win-1", FSHFOpeningType.WINDOW, FSHFWallSide.NORTE,
                    clamp(w / 2 - 0.6, 0.2, w - 1.4), 1.2, 1.0, 0.9));
        }
        // Luz de techo al centro.
        FSHFLight bulb = new FSHFLight("point-1", FSHFLightType.POINT);
        bulb.x = w / 2; bulb.y = H - 0.25; bulb.z = l / 2;
        p.lights.add(bulb);

        furnish(catalog, p, kind, w, l);
        return p;
    }

    private static void furnish(FSHFCatalog catalog, FSHFProject p, String kind, double w, double l) {
        switch (kind) {
            case "dormitorio" -> { add(catalog, p, "cama", w / 2, 1.0, 0); add(catalog, p, "closet", w - 0.3, l - 0.8, 270); }
            case "sala" -> { add(catalog, p, "sillon", w / 2, l - 0.5, 180); add(catalog, p, "rack-tv", w / 2, 0.25, 0); add(catalog, p, "mesa", w / 2, l / 2, 0); }
            case "comedor" -> add(catalog, p, "mesa", w / 2, l / 2, 0);
            case "estudio" -> add(catalog, p, "librero", 0.2, l / 2, 90);
            case "cochera" -> {}
            default -> { }
        }
    }

    private static void add(FSHFCatalog catalog, FSHFProject p, String type, double x, double z, double rot) {
        if (catalog == null || !catalog.contains(type)) return;
        var e = catalog.get(type);
        var s = e.defaults();
        p.furniture.add(new FSHFFurniture(type + "-" + (p.furniture.size() + 1), type,
                round(x), round(z), rot, s.width(), s.depth(), s.height(), e.defaultColor()));
    }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    private static double round(double v) { return Math.round(v * 100.0) / 100.0; }
}
