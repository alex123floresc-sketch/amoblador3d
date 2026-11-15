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
 * Construye la "Casa de ejemplo" de un solo piso según el plano de referencia:
 * área social abierta (sala, comedor y cocina) arriba; suite con baño y dormitorio
 * principal abajo a la izquierda; pasillo y entrada al centro; y a la derecha el
 * dormitorio 2, el baño 2 y el dormitorio 3. Se usa para generar el archivo
 * {@code /ejemplo/casa-ejemplo.amob3d} que viaja con la aplicación, así el ejemplo se
 * puede abrir, editar y mejorar como cualquier proyecto.
 */
public final class FSHFExampleHouse {
    private FSHFExampleHouse() {}

    private static final double T = 0.15;   // grosor de muro
    private static final double H = 2.5;    // altura

    // Acabados tipo mármol claro del plano
    private static final String PISO = "#e9ebee";      // piso porcelanato/mármol
    private static final String PISO_BANO = "#dde6ea"; // piso de baño
    private static final String MURO = "#f2f1ec";      // pared hueso

    // Rejilla simétrica: columna izquierda y derecha del MISMO ancho, con el
    // pasillo recto y centrado entre ellas.
    private static final double COL = 4.60;   // ancho de cada columna lateral
    private static final double HALL = 1.80;  // ancho del pasillo (uniforme)
    private static final double RX = COL + HALL;  // inicio de la columna derecha = 6.40

    public static FSHFHome build(FSHFCatalog catalog) {
        // Área social abierta (sala + comedor + cocina), ocupa todo el ancho arriba.
        FSHFProject social = room(catalog, 2 * COL + HALL, 4.5, PISO, MURO, "social");
        FSHFHome h = FSHFHome.of(social, "Sala, comedor y cocina");
        h.spaces.get(0).x = 0; h.spaces.get(0).z = 0;

        // Columna izquierda (suite): baño y dormitorio principal, ancho COL.
        place(h, "Baño principal",      0.00, 4.65, room(catalog, COL, 2.50, PISO_BANO, MURO, "banoP"));
        place(h, "Dormitorio principal",0.00, 7.30, room(catalog, COL, 6.05, PISO, MURO, "dormP"));

        // Pasillo recto y centrado, con la entrada al final.
        place(h, "Pasillo",             COL, 4.65, room(catalog, HALL, 6.30, PISO, MURO, "pasillo"));
        place(h, "Entrada",             COL, 11.10, room(catalog, HALL, 2.25, PISO, MURO, "entrada"));

        // Columna derecha (ancho COL): dormitorio 2 y 3 IGUALES, con el baño 2 en medio.
        place(h, "Dormitorio 2",        RX, 4.65, room(catalog, COL, 3.30, PISO, MURO, "dorm2"));
        place(h, "Baño 2",              RX, 8.10, room(catalog, COL, 1.80, PISO_BANO, MURO, "bano2"));
        place(h, "Dormitorio 3",        RX, 10.05, room(catalog, COL, 3.30, PISO, MURO, "dorm3"));
        return h;
    }

    private static void place(FSHFHome h, String name, double x, double z, FSHFProject p) {
        h.spaces.add(new FSHFHome.Space(h.nextId(), name, round(x), round(z), p, 0, 0));
    }

    /** Crea un ambiente con medidas, acabados, una puerta, una ventana, luz y muebles. */
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
        p.openings.add(new FSHFOpening("door-1", FSHFOpeningType.DOOR, FSHFWallSide.SUR,
                clamp(w / 2 - 0.45, 0.2, w - 1.1), 0.9, 2.1, 0));
        if (w >= 1.8 && !kind.startsWith("bano")) {
            p.openings.add(new FSHFOpening("win-1", FSHFOpeningType.WINDOW, FSHFWallSide.NORTE,
                    clamp(w / 2 - 0.6, 0.2, w - 1.4), 1.2, 1.1, 0.9));
        }
        FSHFLight bulb = new FSHFLight("point-1", FSHFLightType.POINT);
        bulb.x = w / 2; bulb.y = H - 0.25; bulb.z = l / 2;
        p.lights.add(bulb);

        furnish(catalog, p, kind, w, l);
        return p;
    }

    private static void furnish(FSHFCatalog catalog, FSHFProject p, String kind, double w, double l) {
        switch (kind) {
            case "social" -> {                       // izq: sala · centro: comedor · der: cocina
                add(catalog, p, "rack-tv", 1.8, 0.3, 0);
                add(catalog, p, "sillon", 1.8, l - 0.6, 180);
                add(catalog, p, "mesa", 1.8, l / 2, 0, 1.0, 0.5);
                add(catalog, p, "mesa", w / 2, l / 2, 0, 1.6, 0.9);     // comedor
                add(catalog, p, "cocina", w - 0.4, 1.2, 90);
                add(catalog, p, "refrigeradora", w - 0.5, l - 0.7, 180);
                add(catalog, p, "planta", 0.4, l - 0.4, 0);
            }
            case "dormP" -> { add(catalog, p, "cama", w / 2, 1.2, 0); add(catalog, p, "closet", w - 0.3, l - 1.0, 270); add(catalog, p, "comoda", 0.3, l - 0.6, 90); }
            case "dorm2" -> { add(catalog, p, "cama", w - 1.3, 1.1, 0); add(catalog, p, "escritorio", 0.3, 0.6, 90); }
            case "dorm3" -> { add(catalog, p, "cama", w / 2, 1.2, 0); add(catalog, p, "closet", 0.3, l - 1.0, 90); }
            case "banoP", "bano2" -> { add(catalog, p, "inodoro", w - 0.4, 0.5, 0); add(catalog, p, "lavatorio", 0.5, 0.5, 0); }
            case "entrada" -> add(catalog, p, "planta", 0.4, 0.4, 0);
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

    private static void add(FSHFCatalog catalog, FSHFProject p, String type, double x, double z, double rot,
                            double width, double depth) {
        if (catalog == null || !catalog.contains(type)) return;
        var e = catalog.get(type);
        var s = e.defaults();
        p.furniture.add(new FSHFFurniture(type + "-" + (p.furniture.size() + 1), type,
                round(x), round(z), rot, width, depth, s.height(), e.defaultColor()));
    }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    private static double round(double v) { return Math.round(v * 100.0) / 100.0; }
}
