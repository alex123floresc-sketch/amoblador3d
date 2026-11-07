package pe.edu.unaj.amoblador3d.scene;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.paint.Color;
import pe.edu.unaj.amoblador3d.design.FSHFDesign;
import pe.edu.unaj.amoblador3d.model.FSHFBaseboard;
import pe.edu.unaj.amoblador3d.model.FSHFColumn;
import pe.edu.unaj.amoblador3d.model.FSHFOpening;
import pe.edu.unaj.amoblador3d.model.FSHFOpeningType;
import pe.edu.unaj.amoblador3d.model.FSHFRoomDims;
import pe.edu.unaj.amoblador3d.model.FSHFSurface;
import pe.edu.unaj.amoblador3d.model.FSHFWallSide;

/**
 * Base, piso, cuadrícula, las 4 paredes con sus huecos y zócalos, columnas y muros internos,
 * el techo (solo se muestra en el recorrido) y los marcos, vidrios y hojas de puerta. El piso,
 * cada pared, el techo y cada columna usan el material y el color elegidos en el proyecto.
 */
final class FSHFRoomView {
    private static final double BASEBOARD_DEPTH = 0.012;
    private static final double PLINTH_MARGIN = 0.3, PLINTH_DEPTH = 0.08;

    final Group group = new Group();
    private final Group grid = new Group();
    private final Map<FSHFWallSide, Group> walls = new EnumMap<>(FSHFWallSide.class);
    private Node ceiling;
    private FSHFRoomDims dims = FSHFRoomDims.DEFAULT;
    // Zócalo de la reconstrucción actual (alto 0 = sin zócalo)
    private double baseboardHeight;
    private PhongMaterial baseboardMaterial;
    private FSHFBaseboard baseboard;

    void rebuild(FSHFDesign design) {
        dims = design.room();
        group.getChildren().clear();
        grid.getChildren().clear();
        walls.clear();
        baseboard = design.baseboard();
        baseboardHeight = baseboard.height;
        baseboardMaterial = FSHFMaterials.shiny(Color.web(baseboard.color), 0.25, 20);
        createFloor(design.floor());
        createCeiling(design.ceiling());
        for (FSHFColumn c : design.columns()) createColumn(c);
        for (FSHFWallSide side : FSHFWallSide.values()) {
            FSHFWallFrame wall = FSHFWallFrame.of(side, dims);
            List<FSHFOpening> mine = new ArrayList<>();
            for (FSHFOpening o : design.openings()) if (o.wall == side) mine.add(o);
            Group g = new Group();
            FSHFSurface surface = design.wall(side);
            createWall(g, wall, mine, FSHFSurfaceTextures.material(surface.material, surface.color), FSHFSurfaceTextures.tileSize(surface.material));
            createBaseboard(g, wall, mine);
            for (FSHFOpening o : mine) g.getChildren().add(openingVisual(wall, o));
            FSHFSceneView.tag(g, surface);
            walls.put(side, g);
            group.getChildren().add(g);
        }
    }

    /**
     * El piso cubre también la planta de los muros (queda tapado por ellos), así al ocultar una
     * pared en 3D se ve el borde del piso. Debajo va una base clara que enmarca la habitación.
     */
    private void createFloor(FSHFSurface surface) {
        double w = dims.width(), l = dims.length(), t = dims.wallThickness();
        double m = t + PLINTH_MARGIN;
        Box plinth = new Box(w + 2 * m, PLINTH_DEPTH, l + 2 * m);
        plinth.setTranslateX(w / 2);
        plinth.setTranslateY(-0.02 - PLINTH_DEPTH / 2);
        plinth.setTranslateZ(l / 2);
        plinth.setMaterial(FSHFMaterials.PLINTH);
        plinth.setMouseTransparent(true);
        group.getChildren().add(plinth);

        PhongMaterial material = FSHFSurfaceTextures.material(surface.material, surface.color);
        Node floor = FSHFMeshes.box(-t, w + t, -0.02, 0, -t, l + t, FSHFSurfaceTextures.tileSize(surface.material), material);
        FSHFSceneView.tag(floor, surface);
        group.getChildren().add(floor);

        // Cuadrícula de 1 m para percibir la escala (solo en planta: en 3D la textura ya da la escala).
        for (int x = 1; x < w; x++) grid.getChildren().add(gridLine(x, l / 2, 0.012, l));
        for (int z = 1; z < l; z++) grid.getChildren().add(gridLine(w / 2, z, w, 0.012));
        group.getChildren().add(grid);
    }

    private static Box gridLine(double x, double z, double sx, double sz) {
        Box b = new Box(sx, 0.002, sz);
        b.setTranslateX(x);
        b.setTranslateY(0.001);
        b.setTranslateZ(z);
        b.setMaterial(FSHFMaterials.of("#9c7a55"));
        b.setMouseTransparent(true);
        return b;
    }

    /**
     * El muro se arma con cajas: tramos completos entre aberturas y, en cada abertura,
     * una caja debajo (si es ventana) y otra encima, dejando el hueco libre.
     */
    private void createWall(Group g, FSHFWallFrame wall, List<FSHFOpening> openings, PhongMaterial material, double tile) {
        double h = dims.height(), t = dims.wallThickness();
        List<FSHFOpening> sorted = new ArrayList<>(openings);
        sorted.sort(Comparator.comparingDouble(o -> o.offset));
        double cursor = wall.start();
        for (FSHFOpening o : sorted) {
            if (o.offset > cursor) g.getChildren().add(wall.texturedBox(cursor, o.offset, 0, h, 0, t, tile, material));
            double a0 = o.offset, a1 = o.offset + o.width, top = o.sill + o.height;
            if (o.sill > 1e-6) g.getChildren().add(wall.texturedBox(a0, a1, 0, o.sill, 0, t, tile, material));
            if (h - top > 1e-6) g.getChildren().add(wall.texturedBox(a0, a1, top, h, 0, t, tile, material));
            cursor = Math.max(cursor, a1);
        }
        if (wall.end() > cursor) g.getChildren().add(wall.texturedBox(cursor, wall.end(), 0, h, 0, t, tile, material));
    }

    /** Zócalo a lo largo de la cara interior, cortado en las puertas. */
    private void createBaseboard(Group g, FSHFWallFrame wall, List<FSHFOpening> openings) {
        if (baseboardHeight <= 0) return;
        List<FSHFOpening> doors = new ArrayList<>();
        for (FSHFOpening o : openings) if (o.type.isDoor()) doors.add(o);
        doors.sort(Comparator.comparingDouble(o -> o.offset));
        double cursor = 0, end = dims.wallLength(wall.side());
        for (FSHFOpening d : doors) {
            if (d.offset > cursor) g.getChildren().add(baseboard(wall, cursor, d.offset));
            cursor = Math.max(cursor, d.offset + d.width);
        }
        if (end > cursor) g.getChildren().add(baseboard(wall, cursor, end));
    }

    private Node baseboard(FSHFWallFrame wall, double a0, double a1) {
        Box b = wall.box(a0, a1, 0, baseboardHeight, -BASEBOARD_DEPTH, 0, baseboardMaterial);
        FSHFSceneView.tag(b, baseboard);
        return b;
    }

    /** Cielo raso: una losa delgada sobre la habitación. Solo se ve en el recorrido. */
    private void createCeiling(FSHFSurface surface) {
        double w = dims.width(), l = dims.length(), h = dims.height(), t = dims.wallThickness();
        ceiling = FSHFMeshes.box(-t, w + t, h, h + 0.02, -t, l + t, FSHFSurfaceTextures.tileSize(surface.material),
                FSHFSurfaceTextures.material(surface.material, surface.color));
        FSHFSceneView.tag(ceiling, surface);
        ceiling.setVisible(false);
        group.getChildren().add(ceiling);
    }

    /** Columna, muro interno o recorte: un bloque del piso al techo con su acabado y su zócalo. */
    private void createColumn(FSHFColumn c) {
        double h = dims.height();
        Group g = new Group();
        g.getChildren().add(FSHFMeshes.box(c.minX(), c.maxX(), 0, h, c.minZ(), c.maxZ(), FSHFSurfaceTextures.tileSize(c.material),
                FSHFSurfaceTextures.material(c.material, c.color)));
        if (baseboardHeight > 0) {
            double d = BASEBOARD_DEPTH, bh = baseboardHeight, e = 0.005;
            // Solo en las caras que no están pegadas a una pared.
            if (c.minZ() > e) g.getChildren().add(baseboardBox(c.minX() - d, c.maxX() + d, c.minZ() - d, c.minZ(), bh));
            if (c.maxZ() < dims.length() - e) g.getChildren().add(baseboardBox(c.minX() - d, c.maxX() + d, c.maxZ(), c.maxZ() + d, bh));
            if (c.minX() > e) g.getChildren().add(baseboardBox(c.minX() - d, c.minX(), c.minZ(), c.maxZ(), bh));
            if (c.maxX() < dims.width() - e) g.getChildren().add(baseboardBox(c.maxX(), c.maxX() + d, c.minZ(), c.maxZ(), bh));
        }
        FSHFSceneView.tag(g, c);
        group.getChildren().add(g);
    }

    private Box baseboardBox(double x0, double x1, double z0, double z1, double h) {
        Box b = new Box(x1 - x0, h, z1 - z0);
        b.setTranslateX((x0 + x1) / 2);
        b.setTranslateY(h / 2);
        b.setTranslateZ((z0 + z1) / 2);
        b.setMaterial(baseboardMaterial);
        FSHFSceneView.tag(b, baseboard);
        return b;
    }

    /** Marco y vidrio (ventana) o marco, hoja y manijas (puerta). */
    private Group openingVisual(FSHFWallFrame wall, FSHFOpening o) {
        Group g = new Group();
        double t = dims.wallThickness(), x0 = o.offset, w = o.width, h = o.height, sill = o.sill;
        double x1 = x0 + w, cz = t / 2, fw = 0.05; // fw = ancho de los perfiles del marco
        List<Box> parts = new ArrayList<>();
        if (o.type == FSHFOpeningType.WINDOW) {
            double top = sill + h, fd0 = cz - 0.035, fd1 = cz + 0.035;
            parts.add(wall.box(x0, x1, top - fw, top, fd0, fd1, FSHFMaterials.FRAME)); // dintel
            parts.add(wall.box(x0, x1, sill, sill + fw, fd0, fd1, FSHFMaterials.FRAME)); // base
            parts.add(wall.box(x0, x0 + fw, sill + fw, top - fw, fd0, fd1, FSHFMaterials.FRAME)); // lateral izq.
            parts.add(wall.box(x1 - fw, x1, sill + fw, top - fw, fd0, fd1, FSHFMaterials.FRAME)); // lateral der.
            if (w > 1) parts.add(wall.box(x0 + w / 2 - 0.02, x0 + w / 2 + 0.02, sill + fw, top - fw, fd0, fd1, FSHFMaterials.FRAME));
            parts.add(wall.box(x0 + fw, x1 - fw, sill + fw, top - fw, cz - 0.005, cz + 0.005, FSHFMaterials.GLASS)); // vidrio
            parts.add(wall.box(x0 - 0.05, x1 + 0.05, sill - 0.03, sill, -0.10, 0.02, FSHFMaterials.FRAME)); // repisa interior
        } else {
            parts.add(wall.box(x0, x0 + fw, 0, h, -0.01, t + 0.01, FSHFMaterials.FRAME)); // jamba izq.
            parts.add(wall.box(x1 - fw, x1, 0, h, -0.01, t + 0.01, FSHFMaterials.FRAME)); // jamba der.
            parts.add(wall.box(x0, x1, h - fw, h, -0.01, t + 0.01, FSHFMaterials.FRAME)); // cabezal
            parts.add(wall.box(x0, x1, 0, 0.01, 0, t, FSHFMaterials.FRAME)); // umbral (cubre el piso dentro del muro)
            double l0 = x0 + fw, l1 = x1 - fw, top = h - fw; // hueco libre entre el marco
            switch (o.type) {
                case DOUBLE -> {
                    double mid = (l0 + l1) / 2;
                    parts.add(wall.box(l0, mid - 0.003, 0.01, top, 0.01, 0.05, FSHFMaterials.DOOR)); // hoja izq.
                    parts.add(wall.box(mid + 0.003, l1, 0.01, top, 0.01, 0.05, FSHFMaterials.DOOR)); // hoja der.
                    for (double side : new double[] {-1, 1}) handles(parts, wall, mid + side * 0.1);
                }
                case SLIDING -> {
                    // Dos paños de vidrio con marco, uno delante del otro, que se cruzan en el centro.
                    double mid = (l0 + l1) / 2, ov = 0.04;
                    slidingPanel(parts, wall, l0, mid + ov, top, cz - 0.03);
                    slidingPanel(parts, wall, mid - ov, l1, top, cz + 0.03);
                }
                case GARAGE -> {
                    // Portón seccional: placas horizontales de metal.
                    int n = Math.max(3, (int) Math.round(top / 0.5));
                    double ph = (top - 0.01) / n;
                    for (int i = 0; i < n; i++) {
                        parts.add(wall.box(l0, l1, 0.01 + i * ph + 0.005, 0.01 + (i + 1) * ph - 0.005, cz - 0.02, cz + 0.02,
                                FSHFMaterials.METAL));
                    }
                }
                case PASSAGE -> { } // solo el marco: se pasa sin puerta
                default -> {
                    PhongMaterial leaf = o.type == FSHFOpeningType.ENTRY ? FSHFMaterials.ENTRY_DOOR : FSHFMaterials.DOOR;
                    parts.add(wall.box(l0, l1, 0.01, top, 0.01, 0.05, leaf)); // hoja
                    handles(parts, wall, l1 - 0.1);
                    if (o.type == FSHFOpeningType.ENTRY) {
                        // Mirilla de vidrio angosta a lo alto de la puerta principal.
                        double gx = l0 + (l1 - l0) * 0.25;
                        parts.add(wall.box(gx - 0.05, gx + 0.05, 0.9, top - 0.25, 0.005, 0.055, FSHFMaterials.GLASS));
                    }
                }
            }
        }
        g.getChildren().addAll(parts);
        FSHFSceneView.tag(g, o);
        return g;
    }

    /** Manijas a 1 m de alto, por dentro y por fuera de una hoja, centradas en {@code x}. */
    private static void handles(List<Box> parts, FSHFWallFrame wall, double x) {
        parts.add(wall.box(x - 0.06, x + 0.06, 0.99, 1.01, -0.015, 0.005, FSHFMaterials.METAL)); // interior
        parts.add(wall.box(x - 0.06, x + 0.06, 0.99, 1.01, 0.055, 0.075, FSHFMaterials.METAL)); // exterior
    }

    /** Paño de puerta corrediza: vidrio con marco, a la profundidad {@code z} dentro del muro. */
    private static void slidingPanel(List<Box> parts, FSHFWallFrame wall, double a0, double a1, double top, double z) {
        double f = 0.04, d0 = z - 0.015, d1 = z + 0.015;
        parts.add(wall.box(a0, a1, 0.01, 0.01 + f, d0, d1, FSHFMaterials.FRAME));
        parts.add(wall.box(a0, a1, top - f, top, d0, d1, FSHFMaterials.FRAME));
        parts.add(wall.box(a0, a0 + f, 0.01, top, d0, d1, FSHFMaterials.FRAME));
        parts.add(wall.box(a1 - f, a1, 0.01, top, d0, d1, FSHFMaterials.FRAME));
        parts.add(wall.box(a0 + f, a1 - f, 0.01 + f, top - f, z - 0.004, z + 0.004, FSHFMaterials.GLASS));
    }

    /**
     * En 3D, oculta las paredes que están entre la cámara y el interior (efecto
     * "casa de muñecas") para poder ver dentro. En planta se ven todas.
     */
    void updateVisibility(Point3D eye, boolean plan, boolean walk, FSHFWallSide selected) {
        grid.setVisible(plan);
        if (ceiling != null) ceiling.setVisible(walk);
        for (Map.Entry<FSHFWallSide, Group> e : walls.entrySet()) {
            FSHFWallFrame wall = FSHFWallFrame.of(e.getKey(), dims);
            Point3D normal = wall.interiorNormal();
            // Un punto de la cara interior: el centro de la habitación proyectado sobre la pared.
            Point3D inside = wall.point(e.getKey().alongX() ? dims.width() / 2 : dims.length() / 2, 0, 0);
            // La pared seleccionada siempre se ve (si no, al elegirla desde el panel no se vería nada).
            boolean show = plan || e.getKey() == selected || eye.subtract(inside).dotProduct(normal) > 0;
            e.getValue().setVisible(show);
        }
    }
}
