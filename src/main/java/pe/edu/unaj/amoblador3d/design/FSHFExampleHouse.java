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

import static pe.edu.unaj.amoblador3d.model.FSHFWallSide.ESTE;
import static pe.edu.unaj.amoblador3d.model.FSHFWallSide.NORTE;
import static pe.edu.unaj.amoblador3d.model.FSHFWallSide.OESTE;
import static pe.edu.unaj.amoblador3d.model.FSHFWallSide.SUR;

/**
 * Construye la "Casa de ejemplo" de dos pisos sobre una retícula a escala de
 * 9.00 × 9.00 m (medidas exteriores, con muros).
 *
 * <pre>
 *   X:  0 ──── 3.60 ── 5.40 ──── 9.00        Z piso 1: 0 ── 5.00 ── 9.00
 *                                            Z piso 2: 0 ── 3.60 ── 5.40 ── 9.00
 *
 *   Piso 1                                   Piso 2
 *   ┌──────────────────────────────┐         ┌─────────┬─────┬─────────┐
 *   │                              │         │ Dorm.   │     │ Dorm. 2 │
 *   │   Sala · comedor · cocina    │         │ princ.  │  P  │         │
 *   │                              │         ├─────────┤  a  ├─────────┤
 *   ├─────────┬─────┬──────────────┤         │ Baño P. │  s  │ Baño 2  │
 *   │ Baño de │ Esc.│   Entrada    │         ├─────────┤  i  ├─────────┤
 *   │ visita  │     │              │         │ Estudio │  l  │ Dorm. 3 │
 *   └─────────┴─────┴──────────────┘         └─────────┴─────┴─────────┘
 * </pre>
 *
 * Cada ambiente ocupa una celda de la retícula: su interior mide la celda menos dos
 * medios muros y sus muros exteriores tocan los del vecino, que es como la aplicación
 * reconoce ambientes pegados (sin encimarse, con puertas que comunican y la fachada
 * solo en los muros que dan afuera). Interiores: dormitorios y estudio de
 * 3.30 × 3.30 m, baños de 3.30 × 1.50 m, pasillo de 1.50 × 8.70 m, zona social de
 * 8.70 × 4.70 m y, abajo, baño de visita y entrada de 3.30 × 3.70 m con la escalera
 * de 1.50 × 3.70 m entre ambos. El archivo {@code /ejemplo/casa-ejemplo.amob3d} se genera desde aquí.
 */
public final class FSHFExampleHouse {
    private FSHFExampleHouse() {}

    private static final double T = 0.15;   // grosor de muro
    private static final double H = 2.5;    // altura libre de cada piso

    /** Líneas de la retícula (caras exteriores de los muros). */
    private static final double X0 = 0.0, X1 = 3.6, X2 = 5.4, X3 = 9.0;
    private static final double Z0 = 0.0, Z1 = 3.6, Z2 = 5.4, Z3 = 9.0;
    /** En el piso 1 la fila de abajo es más profunda para que quepa la escalera. */
    private static final double ZS = 5.0;

    private static final String PISO = "#e9ebee";
    private static final String PISO_BANO = "#dde6ea";
    private static final String MURO = "#f2f1ec";

    public static FSHFHome build(FSHFCatalog c) {
        // ===== Piso 1 (nivel 0): zona social =====
        FSHFProject social = cell(X0, X3, Z0, ZS, PISO);
        window(social, NORTE, 0.85, 1.6);
        window(social, NORTE, 6.25, 1.6);
        window(social, ESTE, 3.4, 1.2);
        // sala (lado oeste)
        add(c, social, "rack-tv", 0.25, 2.35, 90);
        add(c, social, "mesa-centro", 1.6, 2.35, 90);
        add(c, social, "sillon", 2.9, 2.35, 270);
        add(c, social, "lampara-pie", 0.3, 4.35, 0);
        add(c, social, "planta", 0.3, 0.3, 0);
        // comedor (centro)
        add(c, social, "mesa", 4.6, 2.35, 0, 1.6, 0.9);
        add(c, social, "silla", 4.2, 1.6, 0);
        add(c, social, "silla", 5.0, 1.6, 0);
        add(c, social, "silla", 4.2, 3.1, 180);
        add(c, social, "silla", 5.0, 3.1, 180);
        // cocina (lado este)
        add(c, social, "cocina", 8.4, 1.4, 90);
        add(c, social, "refrigeradora", 8.35, 2.7, 90);
        add(c, social, "isla-cocina", 6.8, 2.35, 90);

        FSHFHome h = FSHFHome.of(social, "Sala, comedor y cocina");
        h.spaces.get(0).x = X0 + T;
        h.spaces.get(0).z = Z0 + T;

        FSHFProject banoV = cell(X0, X1, ZS, Z3, PISO_BANO);
        door(banoV, NORTE, 2.3, 0.8, FSHFOpeningType.DOOR);
        window(banoV, OESTE, 1.35, 0.6, 0.6, 1.5);
        add(c, banoV, "inodoro", 0.6, 3.34, 180);
        add(c, banoV, "lavatorio", 1.5, 3.47, 180);
        add(c, banoV, "planta", 0.35, 0.35, 0);
        place(h, "Baño de visita", banoV, X0, ZS, 0);

        FSHFProject escalera = cell(X1, X2, ZS, Z3, PISO);
        door(escalera, NORTE, 0.3, 0.8, FSHFOpeningType.PASSAGE);
        add(c, escalera, "escalera", 0.75, 2.25, 0, 0.9, 2.8);
        place(h, "Escalera", escalera, X1, ZS, 0);

        FSHFProject entrada = cell(X2, X3, ZS, Z3, PISO);
        door(entrada, NORTE, 1.2, 0.9, FSHFOpeningType.PASSAGE);
        door(entrada, SUR, 1.15, 1.0, FSHFOpeningType.ENTRY);
        add(c, entrada, "aparador", 0.25, 1.85, 90);
        add(c, entrada, "cuadro", 3.28, 1.85, 270);
        add(c, entrada, "planta", 2.95, 3.35, 0);
        place(h, "Entrada", entrada, X2, ZS, 0);

        // ===== Piso 2 (nivel 1): dormitorios a ambos lados del pasillo =====
        FSHFProject dormP = cell(X0, X1, Z0, Z1, PISO);
        door(dormP, ESTE, 2.2, 0.9, FSHFOpeningType.DOOR);
        window(dormP, OESTE, 1.05, 1.2);
        add(c, dormP, "cama", 1.4, 1.0, 0);
        add(c, dormP, "mesa-noche", 0.4, 0.25, 0);
        add(c, dormP, "mesa-noche", 2.4, 0.25, 0);
        add(c, dormP, "ropero", 1.2, 3.0, 180);
        place(h, "Dormitorio principal", dormP, X0, Z0, 1);

        FSHFProject banoP = cell(X0, X1, Z1, Z2, PISO_BANO);
        door(banoP, ESTE, 0.3, 0.8, FSHFOpeningType.DOOR);
        window(banoP, OESTE, 0.45, 0.6, 0.6, 1.5);
        add(c, banoP, "tina", 0.85, 1.12, 0);
        add(c, banoP, "lavatorio", 1.0, 0.23, 0);
        add(c, banoP, "inodoro", 2.0, 0.36, 0);
        place(h, "Baño principal", banoP, X0, Z1, 1);

        FSHFProject estudio = cell(X0, X1, Z2, Z3, PISO);
        door(estudio, ESTE, 0.2, 0.9, FSHFOpeningType.DOOR);
        window(estudio, OESTE, 1.05, 1.2);
        add(c, estudio, "escritorio", 0.31, 1.65, 270);
        add(c, estudio, "silla-oficina", 1.0, 1.65, 90);
        add(c, estudio, "estante", 1.6, 0.16, 0);
        add(c, estudio, "sillon-1", 2.6, 2.7, 0);
        add(c, estudio, "planta", 0.35, 2.95, 0);
        place(h, "Estudio", estudio, X0, Z2, 1);

        FSHFProject pasillo = cell(X1, X2, Z0, Z3, PISO);
        window(pasillo, NORTE, 0.3, 0.9);
        place(h, "Pasillo", pasillo, X1, Z0, 1);

        FSHFProject dorm2 = cell(X2, X3, Z0, Z1, PISO);
        door(dorm2, OESTE, 2.2, 0.9, FSHFOpeningType.DOOR);
        window(dorm2, ESTE, 1.05, 1.2);
        add(c, dorm2, "cama", 1.9, 1.0, 0);
        add(c, dorm2, "mesa-noche", 0.9, 0.25, 0);
        add(c, dorm2, "mesa-noche", 2.9, 0.25, 0);
        add(c, dorm2, "escritorio", 2.2, 3.0, 180);
        add(c, dorm2, "silla-oficina", 2.2, 2.3, 180);
        place(h, "Dormitorio 2", dorm2, X2, Z0, 1);

        FSHFProject bano2 = cell(X2, X3, Z1, Z2, PISO_BANO);
        door(bano2, OESTE, 0.3, 0.8, FSHFOpeningType.DOOR);
        window(bano2, ESTE, 0.45, 0.6, 0.6, 1.5);
        add(c, bano2, "ducha", 2.8, 1.0, 0);
        add(c, bano2, "lavatorio", 2.3, 0.23, 0);
        add(c, bano2, "inodoro", 1.3, 0.36, 0);
        place(h, "Baño 2", bano2, X2, Z1, 1);

        FSHFProject dorm3 = cell(X2, X3, Z2, Z3, PISO);
        door(dorm3, OESTE, 0.2, 0.9, FSHFOpeningType.DOOR);
        window(dorm3, ESTE, 1.05, 1.2);
        add(c, dorm3, "cama", 1.9, 2.3, 180);
        add(c, dorm3, "mesa-noche", 0.9, 3.05, 180);
        add(c, dorm3, "mesa-noche", 2.9, 3.05, 180);
        add(c, dorm3, "closet", 2.1, 0.31, 0);
        place(h, "Dormitorio 3", dorm3, X2, Z2, 1);
        return h;
    }

    /** Ambiente que llena la celda [x0,x1]×[z0,z1] medida por las caras exteriores de los muros. */
    private static FSHFProject cell(double x0, double x1, double z0, double z1, String floorColor) {
        double w = round(x1 - x0 - 2 * T), l = round(z1 - z0 - 2 * T);
        FSHFProject p = new FSHFProject();
        p.room = new FSHFRoomDims(w, l, H, T);
        p.floor.material = FSHFSurfaceMaterial.PORCELANATO;
        p.floor.color = floorColor;
        for (FSHFWallSide side : FSHFWallSide.values()) {
            p.walls.get(side).material = FSHFSurfaceMaterial.PINTURA;
            p.walls.get(side).color = MURO;
        }
        FSHFLight bulb = new FSHFLight("point-1", FSHFLightType.POINT);
        bulb.x = w / 2; bulb.y = H - 0.25; bulb.z = l / 2;
        p.lights.add(bulb);
        return p;
    }

    private static void place(FSHFHome h, String name, FSHFProject p, double cellX0, double cellZ0, int level) {
        h.spaces.add(new FSHFHome.Space(h.nextId(), name, round(cellX0 + T), round(cellZ0 + T), p, 0, level));
    }

    private static void door(FSHFProject p, FSHFWallSide wall, double offset, double width, FSHFOpeningType type) {
        double height = type == FSHFOpeningType.ENTRY ? 2.2 : 2.1;
        p.openings.add(new FSHFOpening("door-" + (p.openings.size() + 1), type, wall, offset, width, height, 0));
    }

    private static void window(FSHFProject p, FSHFWallSide wall, double offset, double width) {
        window(p, wall, offset, width, 1.1, 0.9);
    }

    private static void window(FSHFProject p, FSHFWallSide wall, double offset, double width, double height, double sill) {
        p.openings.add(new FSHFOpening("win-" + (p.openings.size() + 1), FSHFOpeningType.WINDOW, wall, offset, width, height, sill));
    }

    private static void add(FSHFCatalog catalog, FSHFProject p, String type, double x, double z, double rot) {
        if (catalog == null || !catalog.contains(type)) return;
        var s = catalog.get(type).defaults();
        add(catalog, p, type, x, z, rot, s.width(), s.depth());
    }

    private static void add(FSHFCatalog catalog, FSHFProject p, String type, double x, double z, double rot,
                            double width, double depth) {
        if (catalog == null || !catalog.contains(type)) return;
        var e = catalog.get(type);
        p.furniture.add(new FSHFFurniture(type + "-" + (p.furniture.size() + 1), type,
                round(x), round(z), rot, width, depth, e.defaults().height(), e.defaultColor()));
    }

    private static double round(double v) { return Math.round(v * 100.0) / 100.0; }
}
