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

    // Rejilla a escala del plano (terreno 10.0 × 12.7 m), tilado completo:
    // columnas X en 0 · 3.2 · 5.6 · 10.0 ; filas Z en 0 · 4.7 · 7.6 · 9.3 · 12.7.
    // Cada celda se separa T/2 de la siguiente, así los muros compartidos quedan
    // alineados y no sobran espacios fuera del pasillo.
    private static final double M = T / 2;  // media junta para el muro compartido

    public static FSHFHome build(FSHFCatalog catalog) {
        // ===== Piso 1 (nivel 0): zona social =====
        FSHFProject social = cell(catalog, 0.0, 9.0, 0.0, 5.5, PISO, MURO, "social");
        FSHFHome h = FSHFHome.of(social, "Sala, comedor y cocina");
        h.spaces.get(0).x = round(M); h.spaces.get(0).z = round(M);
        place(h, "Baño de visita", cell(catalog, 0.0, 3.5, 5.5, 9.5, PISO_BANO, MURO, "banoV"), 0.0, 5.5, 0);
        place(h, "Escalera",       cell(catalog, 3.5, 5.5, 5.5, 9.5, PISO, MURO, "escalera"), 3.5, 5.5, 0);
        place(h, "Entrada",        cell(catalog, 5.5, 9.0, 5.5, 9.5, PISO, MURO, "entrada"), 5.5, 5.5, 0);

        // ===== Piso 2 (nivel 1): dormitorios =====
        place(h, "Dormitorio principal", cell(catalog, 0.0, 3.5, 0.0, 5.5, PISO, MURO, "dormP"), 0.0, 0.0, 1);
        place(h, "Baño principal",       cell(catalog, 0.0, 3.5, 5.5, 9.5, PISO_BANO, MURO, "banoP"), 0.0, 5.5, 1);
        place(h, "Pasillo",              cell(catalog, 3.5, 5.5, 0.0, 9.5, PISO, MURO, "pasillo"), 3.5, 0.0, 1);
        place(h, "Dormitorio 2",         cell(catalog, 5.5, 9.0, 0.0, 3.5, PISO, MURO, "dorm2"), 5.5, 0.0, 1);
        place(h, "Baño 2",               cell(catalog, 5.5, 9.0, 3.5, 5.7, PISO_BANO, MURO, "bano2"), 5.5, 3.5, 1);
        place(h, "Dormitorio 3",         cell(catalog, 5.5, 9.0, 5.7, 9.5, PISO, MURO, "dorm3"), 5.5, 5.7, 1);
        return h;
    }

    /** Crea el ambiente de una celda [x0,x1]×[z0,z1], dejando T/2 para los muros. */
    private static FSHFProject cell(FSHFCatalog catalog, double x0, double x1, double z0, double z1,
                                    String floorColor, String wallColor, String kind) {
        return room(catalog, x1 - x0 - T, z1 - z0 - T, floorColor, wallColor, kind);
    }

    private static void place(FSHFHome h, String name, FSHFProject p, double cellX0, double cellZ0, int level) {
        h.spaces.add(new FSHFHome.Space(h.nextId(), name, round(cellX0 + M), round(cellZ0 + M), p, 0, level));
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
                add(catalog, p, "rack-tv", 1.7, 0.35, 0);
                add(catalog, p, "tv", 1.7, 0.15, 0);
                add(catalog, p, "sillon", 1.7, l - 0.7, 180);
                add(catalog, p, "mesa-centro", 1.7, l / 2, 0);
                add(catalog, p, "lampara-pie", 0.4, l - 0.4, 0);
                add(catalog, p, "mesa", w / 2, l / 2, 0, 1.6, 0.9);     // comedor
                add(catalog, p, "cocina", w - 0.4, 1.2, 90);
                add(catalog, p, "refrigeradora", w - 0.5, l - 0.7, 180);
                add(catalog, p, "planta", 0.45, 0.45, 0);
            }
            case "dormP" -> { add(catalog, p, "cama", w / 2, 1.3, 0); add(catalog, p, "ropero", w - 0.35, l - 1.0, 270); add(catalog, p, "comoda", 0.35, l - 0.6, 90); add(catalog, p, "lampara-pie", 0.35, 0.4, 0); }
            case "dorm2" -> { add(catalog, p, "cama", w - 1.2, 1.2, 0); add(catalog, p, "escritorio", 0.35, 0.6, 90); add(catalog, p, "silla-oficina", 0.8, 0.9, 0); }
            case "dorm3" -> { add(catalog, p, "cama", w / 2, 1.2, 0); add(catalog, p, "closet", 0.35, l - 1.0, 90); add(catalog, p, "mesa-noche", w - 0.4, 0.4, 0); }
            case "banoP" -> { add(catalog, p, "inodoro", w - 0.4, 0.5, 0); add(catalog, p, "lavatorio", 0.5, 0.5, 0); add(catalog, p, "tina", w / 2, l - 0.5, 0); add(catalog, p, "espejo-bano", 0.5, 0.1, 0); }
            case "bano2" -> { add(catalog, p, "inodoro", w - 0.4, 0.5, 0); add(catalog, p, "lavatorio", 0.5, 0.5, 0); add(catalog, p, "ducha", w / 2, l - 0.6, 0); }
            case "banoV" -> { add(catalog, p, "inodoro", w - 0.4, 0.5, 0); add(catalog, p, "lavatorio", 0.5, 0.5, 0); add(catalog, p, "espejo-bano", 0.5, 0.1, 0); }
            case "escalera" -> add(catalog, p, "escalera", w / 2, l / 2, 0);
            case "entrada" -> { add(catalog, p, "planta", 0.45, 0.45, 0); add(catalog, p, "cuadro", w / 2, 0.08, 0); }
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
