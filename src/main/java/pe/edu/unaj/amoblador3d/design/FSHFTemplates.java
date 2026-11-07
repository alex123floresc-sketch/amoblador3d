package pe.edu.unaj.amoblador3d.design;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalog;
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalogEntry;
import pe.edu.unaj.amoblador3d.model.FSHFFurniture;
import pe.edu.unaj.amoblador3d.model.FSHFLight;
import pe.edu.unaj.amoblador3d.model.FSHFLightType;
import pe.edu.unaj.amoblador3d.model.FSHFOpening;
import pe.edu.unaj.amoblador3d.model.FSHFOpeningType;
import pe.edu.unaj.amoblador3d.model.FSHFProject;
import pe.edu.unaj.amoblador3d.model.FSHFRoomDims;
import pe.edu.unaj.amoblador3d.model.FSHFSurfaceMaterial;
import pe.edu.unaj.amoblador3d.model.FSHFWallSide;

public final class FSHFTemplates {
    public record Template(String name, String description, Supplier<FSHFProject> build) {}

    private FSHFTemplates() {}

    public static List<Template> all(FSHFCatalog catalog) {
        return List.of(
                new Template("Dormitorio", "3.5 × 3.2 m · cama de 2 plazas, mesas de noche, closet y cómoda",
                        () -> bedroom(catalog)),
                new Template("Sala", "5 × 4 m · sofá, mueble de TV, mesa de centro, librero y planta",
                        () -> living(catalog)),
                new Template("Comedor", "4 × 3.5 m · mesa para 6 y aparador", () -> dining(catalog)),
                new Template("Cocina", "3.2 × 2.6 m · muebles en L, refrigeradora y mesa para 2",
                        () -> kitchen(catalog)),
                new Template("Baño", "2.4 × 1.8 m · inodoro y lavatorio, cerámico en piso y paredes",
                        () -> bathroom(catalog)),
                new Template("Oficina", "3 × 3 m · escritorio, silla, librero y planta", () -> office(catalog)),
                new Template("Habitación vacía", "4 × 3.5 m · solo una ventana, una puerta y luces", FSHFTemplates::empty));
    }

    private static final class Builder {
        final FSHFProject p = new FSHFProject();
        private final FSHFCatalog catalog;
        private final Map<String, Integer> counters = new HashMap<>();

        Builder(FSHFCatalog catalog, double width, double length, double height) {
            this.catalog = catalog;
            p.room = new FSHFRoomDims(width, length, height, 0.15);
        }

        private String id(String prefix) {
            return prefix + "-" + counters.merge(prefix, 1, Integer::sum);
        }

        Builder window(FSHFWallSide wall, double offset, double width, double height, double sill) {
            p.openings.add(new FSHFOpening(id("window"), FSHFOpeningType.WINDOW, wall, offset, width, height, sill));
            return this;
        }

        Builder door(FSHFWallSide wall, double offset, double width) {
            p.openings.add(new FSHFOpening(id("door"), FSHFOpeningType.DOOR, wall, offset, width, 2.1, 0));
            return this;
        }

        FSHFFurniture add(String type, double x, double z, double rotation) {
            FSHFCatalogEntry e = catalog.get(type);
            FSHFCatalogEntry.Size s = e.defaults();
            FSHFFurniture f = new FSHFFurniture(id(type), type, x, z, rotation, s.width(), s.depth(), s.height(), e.defaultColor());
            p.furniture.add(f);
            return f;
        }

        FSHFFurniture add(String type, double x, double z, double rotation, double width, double depth) {
            FSHFFurniture f = add(type, x, z, rotation);
            f.width = width;
            f.depth = depth;
            return f;
        }

        Builder floor(FSHFSurfaceMaterial m, String color) {
            p.floor.material = m;
            p.floor.color = color;
            return this;
        }

        Builder walls(FSHFSurfaceMaterial m, String color) {
            for (FSHFWallSide side : FSHFWallSide.values()) {
                p.walls.get(side).material = m;
                p.walls.get(side).color = color;
            }
            return this;
        }

        FSHFProject lights() {
            FSHFLight bulb = new FSHFLight(id("point"), FSHFLightType.POINT);
            bulb.x = p.room.width() / 2;
            bulb.y = p.room.height() - 0.25;
            bulb.z = p.room.length() / 2;
            p.lights.add(bulb);
            FSHFOpening window = p.openings.stream().filter(o -> o.type == FSHFOpeningType.WINDOW).findFirst().orElse(null);
            if (window != null) {
                FSHFLight sun = new FSHFLight(id("sun"), FSHFLightType.SUN);
                sun.windowId = window.id;
                sun.elevation = 35;
                sun.deviation = 20;
                p.lights.add(sun);
            }
            return p;
        }
    }

    private static FSHFProject bedroom(FSHFCatalog c) {
        Builder b = new Builder(c, 3.5, 3.2, 2.5)
                .window(FSHFWallSide.NORTE, 1.15, 1.2, 1.0, 0.9)
                .door(FSHFWallSide.SUR, 2.4, 0.9)
                .floor(FSHFSurfaceMaterial.MADERA, "#c9a274")
                .walls(FSHFSurfaceMaterial.PINTURA, "#e3d5bf");
        b.add("cama", 1.4, 0.95, 0);
        b.add("mesa-noche", 0.475, 0.2, 0);
        b.add("mesa-noche", 2.325, 0.2, 0);
        b.add("closet", 3.2, 0.8, 270);
        b.add("comoda", 0.225, 2.2, 90);
        return b.lights();
    }

    private static FSHFProject living(FSHFCatalog c) {
        Builder b = new Builder(c, 5, 4, 2.6)
                .window(FSHFWallSide.ESTE, 1.1, 1.8, 1.2, 0.8)
                .door(FSHFWallSide.SUR, 0.3, 0.9)
                .floor(FSHFSurfaceMaterial.PORCELANATO, "#c9c3b9")
                .walls(FSHFSurfaceMaterial.PINTURA, "#f2efe9");
        b.p.walls.get(FSHFWallSide.NORTE).material = FSHFSurfaceMaterial.LADRILLO;
        b.p.walls.get(FSHFWallSide.NORTE).color = "#a4553a";
        b.add("rack-tv", 2.8, 0.225, 0);
        b.add("sillon", 2.8, 3.55, 180);
        b.add("mesa", 2.8, 2.1, 0, 1.0, 0.5).height = 0.45;
        b.add("librero", 0.15, 1.6, 90);
        if (c.contains("planta")) b.add("planta", 4.6, 3.6, 0);
        return b.lights();
    }

    private static FSHFProject dining(FSHFCatalog c) {
        Builder b = new Builder(c, 4, 3.5, 2.5)
                .window(FSHFWallSide.NORTE, 1.4, 1.2, 1.0, 0.9)
                .door(FSHFWallSide.SUR, 0.3, 0.9)
                .floor(FSHFSurfaceMaterial.MADERA, "#b07d4f")
                .walls(FSHFSurfaceMaterial.PINTURA, "#d9d7d1");
        b.add("mesa", 2.2, 1.6, 0, 1.6, 0.9);
        b.add("silla", 1.8, 0.9, 0);
        b.add("silla", 2.6, 0.9, 0);
        b.add("silla", 1.8, 2.3, 180);
        b.add("silla", 2.6, 2.3, 180);
        b.add("silla", 1.15, 1.6, 90);
        b.add("silla", 3.25, 1.6, 270);
        b.add("comoda", 3.775, 3.0, 270);
        return b.lights();
    }

    private static FSHFProject kitchen(FSHFCatalog c) {
        Builder b = new Builder(c, 3.2, 2.6, 2.5)
                .window(FSHFWallSide.NORTE, 0.9, 1.0, 0.8, 1.15)
                .door(FSHFWallSide.SUR, 2.1, 0.85)
                .floor(FSHFSurfaceMaterial.CERAMICO, "#e4e0d8")
                .walls(FSHFSurfaceMaterial.SATINADO, "#e9eef2");
        b.add("cocina", 1.2, 0.3, 0, 2.4, 0.6);
        b.add("refrigeradora", 2.75, 0.35, 0);
        b.add("cocina", 0.3, 1.2, 90, 1.2, 0.6);
        b.add("mesa", 1.5, 1.6, 0, 0.9, 0.7);
        b.add("silla", 1.5, 1.0, 0);
        b.add("silla", 1.5, 2.2, 180);
        return b.lights();
    }

    private static FSHFProject bathroom(FSHFCatalog c) {
        Builder b = new Builder(c, 2.4, 1.8, 2.4)
                .window(FSHFWallSide.NORTE, 0.9, 0.6, 0.5, 1.6)
                .door(FSHFWallSide.SUR, 1.4, 0.8)
                .floor(FSHFSurfaceMaterial.CERAMICO, "#ecebe7")
                .walls(FSHFSurfaceMaterial.CERAMICO, "#e4e0d8");
        b.add("inodoro", 0.4, 0.35, 0);
        b.add("lavatorio", 1.2, 0.225, 0);
        return b.lights();
    }

    private static FSHFProject office(FSHFCatalog c) {
        Builder b = new Builder(c, 3, 3, 2.5)
                .window(FSHFWallSide.NORTE, 0.4, 1.2, 1.0, 1.0)
                .door(FSHFWallSide.SUR, 1.9, 0.9)
                .floor(FSHFSurfaceMaterial.MADERA, "#7a5235")
                .walls(FSHFSurfaceMaterial.PINTURA, "#fafafa");
        b.add("escritorio", 1.0, 0.35, 0, 1.4, 0.7);
        b.add("silla", 1.0, 0.95, 180);
        b.add("librero", 0.175, 1.9, 90, 1.0, 0.35);
        if (c.contains("planta")) b.add("planta", 2.6, 0.4, 0);
        return b.lights();
    }

    private static FSHFProject empty() {
        FSHFProject p = new FSHFProject();
        p.openings.add(new FSHFOpening("window-1", FSHFOpeningType.WINDOW, FSHFWallSide.NORTE, 1.4, 1.2, 1.0, 0.9));
        p.openings.add(new FSHFOpening("door-1", FSHFOpeningType.DOOR, FSHFWallSide.SUR, 1.55, 0.9, 2.1, 0));
        FSHFLight bulb = new FSHFLight("point-1", FSHFLightType.POINT);
        bulb.x = 2;
        bulb.y = 2.25;
        bulb.z = 1.75;
        FSHFLight sun = new FSHFLight("sun-1", FSHFLightType.SUN);
        sun.windowId = "window-1";
        sun.elevation = 35;
        sun.deviation = 20;
        p.lights.add(bulb);
        p.lights.add(sun);
        return p;
    }
}
