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
import pe.edu.unaj.amoblador3d.design.Design;
import pe.edu.unaj.amoblador3d.model.Opening;
import pe.edu.unaj.amoblador3d.model.OpeningType;
import pe.edu.unaj.amoblador3d.model.RoomDims;
import pe.edu.unaj.amoblador3d.model.Surface;
import pe.edu.unaj.amoblador3d.model.WallSide;

/**
 * Base, piso, cuadrícula, las 4 paredes con sus huecos y zócalos, y los marcos, vidrios y
 * hojas de puerta. El piso y cada pared usan el material y el color elegidos en el proyecto.
 */
final class RoomView {
    private static final double BASEBOARD_HEIGHT = 0.08, BASEBOARD_DEPTH = 0.012;
    private static final double PLINTH_MARGIN = 0.3, PLINTH_DEPTH = 0.08;

    final Group group = new Group();
    private final Group grid = new Group();
    private final Map<WallSide, Group> walls = new EnumMap<>(WallSide.class);
    private RoomDims dims = RoomDims.DEFAULT;

    void rebuild(Design design) {
        dims = design.room();
        group.getChildren().clear();
        grid.getChildren().clear();
        walls.clear();
        createFloor(design.floor());
        for (WallSide side : WallSide.values()) {
            WallFrame wall = WallFrame.of(side, dims);
            List<Opening> mine = new ArrayList<>();
            for (Opening o : design.openings()) if (o.wall == side) mine.add(o);
            Group g = new Group();
            Surface surface = design.wall(side);
            createWall(g, wall, mine, SurfaceTextures.material(surface.material, surface.color), SurfaceTextures.tileSize(surface.material));
            createBaseboard(g, wall, mine);
            for (Opening o : mine) g.getChildren().add(openingVisual(wall, o));
            SceneView.tag(g, surface);
            walls.put(side, g);
            group.getChildren().add(g);
        }
    }

    /**
     * El piso cubre también la planta de los muros (queda tapado por ellos), así al ocultar una
     * pared en 3D se ve el borde del piso. Debajo va una base clara que enmarca la habitación.
     */
    private void createFloor(Surface surface) {
        double w = dims.width(), l = dims.length(), t = dims.wallThickness();
        double m = t + PLINTH_MARGIN;
        Box plinth = new Box(w + 2 * m, PLINTH_DEPTH, l + 2 * m);
        plinth.setTranslateX(w / 2);
        plinth.setTranslateY(-0.02 - PLINTH_DEPTH / 2);
        plinth.setTranslateZ(l / 2);
        plinth.setMaterial(Materials.PLINTH);
        plinth.setMouseTransparent(true);
        group.getChildren().add(plinth);

        PhongMaterial material = SurfaceTextures.material(surface.material, surface.color);
        Node floor = Meshes.box(-t, w + t, -0.02, 0, -t, l + t, SurfaceTextures.tileSize(surface.material), material);
        SceneView.tag(floor, surface);
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
        b.setMaterial(Materials.of("#9c7a55"));
        b.setMouseTransparent(true);
        return b;
    }

    /**
     * El muro se arma con cajas: tramos completos entre aberturas y, en cada abertura,
     * una caja debajo (si es ventana) y otra encima, dejando el hueco libre.
     */
    private void createWall(Group g, WallFrame wall, List<Opening> openings, PhongMaterial material, double tile) {
        double h = dims.height(), t = dims.wallThickness();
        List<Opening> sorted = new ArrayList<>(openings);
        sorted.sort(Comparator.comparingDouble(o -> o.offset));
        double cursor = wall.start();
        for (Opening o : sorted) {
            if (o.offset > cursor) g.getChildren().add(wall.texturedBox(cursor, o.offset, 0, h, 0, t, tile, material));
            double a0 = o.offset, a1 = o.offset + o.width, top = o.sill + o.height;
            if (o.sill > 1e-6) g.getChildren().add(wall.texturedBox(a0, a1, 0, o.sill, 0, t, tile, material));
            if (h - top > 1e-6) g.getChildren().add(wall.texturedBox(a0, a1, top, h, 0, t, tile, material));
            cursor = Math.max(cursor, a1);
        }
        if (wall.end() > cursor) g.getChildren().add(wall.texturedBox(cursor, wall.end(), 0, h, 0, t, tile, material));
    }

    /** Zócalo a lo largo de la cara interior, cortado en las puertas. */
    private void createBaseboard(Group g, WallFrame wall, List<Opening> openings) {
        List<Opening> doors = new ArrayList<>();
        for (Opening o : openings) if (o.type == OpeningType.DOOR) doors.add(o);
        doors.sort(Comparator.comparingDouble(o -> o.offset));
        double cursor = 0, end = dims.wallLength(wall.side());
        for (Opening d : doors) {
            if (d.offset > cursor) g.getChildren().add(baseboard(wall, cursor, d.offset));
            cursor = Math.max(cursor, d.offset + d.width);
        }
        if (end > cursor) g.getChildren().add(baseboard(wall, cursor, end));
    }

    private static Box baseboard(WallFrame wall, double a0, double a1) {
        return wall.box(a0, a1, 0, BASEBOARD_HEIGHT, -BASEBOARD_DEPTH, 0, Materials.BASEBOARD);
    }

    /** Marco y vidrio (ventana) o marco, hoja y manijas (puerta). */
    private Group openingVisual(WallFrame wall, Opening o) {
        Group g = new Group();
        double t = dims.wallThickness(), x0 = o.offset, w = o.width, h = o.height, sill = o.sill;
        double x1 = x0 + w, cz = t / 2, fw = 0.05; // fw = ancho de los perfiles del marco
        List<Box> parts = new ArrayList<>();
        if (o.type == OpeningType.WINDOW) {
            double top = sill + h, fd0 = cz - 0.035, fd1 = cz + 0.035;
            parts.add(wall.box(x0, x1, top - fw, top, fd0, fd1, Materials.FRAME)); // dintel
            parts.add(wall.box(x0, x1, sill, sill + fw, fd0, fd1, Materials.FRAME)); // base
            parts.add(wall.box(x0, x0 + fw, sill + fw, top - fw, fd0, fd1, Materials.FRAME)); // lateral izq.
            parts.add(wall.box(x1 - fw, x1, sill + fw, top - fw, fd0, fd1, Materials.FRAME)); // lateral der.
            if (w > 1) parts.add(wall.box(x0 + w / 2 - 0.02, x0 + w / 2 + 0.02, sill + fw, top - fw, fd0, fd1, Materials.FRAME));
            parts.add(wall.box(x0 + fw, x1 - fw, sill + fw, top - fw, cz - 0.005, cz + 0.005, Materials.GLASS)); // vidrio
            parts.add(wall.box(x0 - 0.05, x1 + 0.05, sill - 0.03, sill, -0.10, 0.02, Materials.FRAME)); // repisa interior
        } else {
            parts.add(wall.box(x0, x0 + fw, 0, h, -0.01, t + 0.01, Materials.FRAME)); // jamba izq.
            parts.add(wall.box(x1 - fw, x1, 0, h, -0.01, t + 0.01, Materials.FRAME)); // jamba der.
            parts.add(wall.box(x0, x1, h - fw, h, -0.01, t + 0.01, Materials.FRAME)); // cabezal
            parts.add(wall.box(x0 + fw, x1 - fw, 0.01, h - fw, 0.01, 0.05, Materials.DOOR)); // hoja
            double hx0 = x1 - fw - 0.16, hx1 = x1 - fw - 0.04;
            parts.add(wall.box(hx0, hx1, 0.99, 1.01, -0.015, 0.005, Materials.METAL)); // manija interior
            parts.add(wall.box(hx0, hx1, 0.99, 1.01, 0.055, 0.075, Materials.METAL)); // manija exterior
            parts.add(wall.box(x0, x1, 0, 0.01, 0, t, Materials.FRAME)); // umbral (cubre el piso dentro del muro)
        }
        g.getChildren().addAll(parts);
        SceneView.tag(g, o);
        return g;
    }

    /**
     * En 3D, oculta las paredes que están entre la cámara y el interior (efecto
     * "casa de muñecas") para poder ver dentro. En planta se ven todas.
     */
    void updateVisibility(Point3D eye, boolean plan, WallSide selected) {
        grid.setVisible(plan);
        for (Map.Entry<WallSide, Group> e : walls.entrySet()) {
            WallFrame wall = WallFrame.of(e.getKey(), dims);
            Point3D normal = wall.interiorNormal();
            // Un punto de la cara interior: el centro de la habitación proyectado sobre la pared.
            Point3D inside = wall.point(e.getKey().alongX() ? dims.width() / 2 : dims.length() / 2, 0, 0);
            // La pared seleccionada siempre se ve (si no, al elegirla desde el panel no se vería nada).
            boolean show = plan || e.getKey() == selected || eye.subtract(inside).dotProduct(normal) > 0;
            e.getValue().setVisible(show);
        }
    }
}
