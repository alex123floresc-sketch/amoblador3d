package pe.edu.unaj.amoblador3d.design;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import pe.edu.unaj.amoblador3d.catalog.Catalog;
import pe.edu.unaj.amoblador3d.catalog.CatalogEntry;
import pe.edu.unaj.amoblador3d.model.Furniture;
import pe.edu.unaj.amoblador3d.model.Light;
import pe.edu.unaj.amoblador3d.model.LightType;
import pe.edu.unaj.amoblador3d.model.Opening;
import pe.edu.unaj.amoblador3d.model.OpeningType;
import pe.edu.unaj.amoblador3d.model.Project;
import pe.edu.unaj.amoblador3d.model.RoomDims;
import pe.edu.unaj.amoblador3d.model.SurfaceMaterial;
import pe.edu.unaj.amoblador3d.model.WallSide;

/**
 * Habitaciones listas para empezar: medidas típicas, puerta, ventanas, luces, acabados y
 * muebles ya ubicados. Todas cumplen las reglas de {@link Design} (lo revisa una prueba).
 *
 * <p>Recordatorio de giros: con 0° el frente del mueble mira al sur (+Z), con 90° al este,
 * con 180° al norte y con 270° al oeste.
 */
public final class Templates {
    /** @param build crea el proyecto (cada vez uno nuevo) */
    public record Template(String name, String description, Supplier<Project> build) {}

    private Templates() {}

    public static List<Template> all(Catalog catalog) {
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
                new Template("Habitación vacía", "4 × 3.5 m · solo una ventana, una puerta y luces", Templates::empty));
    }

    // ---------------------------------------------------------------- Ayudantes

    /** Arma un proyecto paso a paso (los ids se numeran por tipo: "cama-1", "silla-2"...). */
    private static final class Builder {
        final Project p = new Project();
        private final Catalog catalog;
        private final Map<String, Integer> counters = new HashMap<>();

        Builder(Catalog catalog, double width, double length, double height) {
            this.catalog = catalog;
            p.room = new RoomDims(width, length, height, 0.15);
        }

        private String id(String prefix) {
            return prefix + "-" + counters.merge(prefix, 1, Integer::sum);
        }

        Builder window(WallSide wall, double offset, double width, double height, double sill) {
            p.openings.add(new Opening(id("window"), OpeningType.WINDOW, wall, offset, width, height, sill));
            return this;
        }

        Builder door(WallSide wall, double offset, double width) {
            p.openings.add(new Opening(id("door"), OpeningType.DOOR, wall, offset, width, 2.1, 0));
            return this;
        }

        /** Mueble con las medidas por defecto del catálogo. */
        Furniture add(String type, double x, double z, double rotation) {
            CatalogEntry e = catalog.get(type);
            CatalogEntry.Size s = e.defaults();
            Furniture f = new Furniture(id(type), type, x, z, rotation, s.width(), s.depth(), s.height(), e.defaultColor());
            p.furniture.add(f);
            return f;
        }

        /** Mueble con otras medidas (ancho y profundidad; el alto es el del catálogo). */
        Furniture add(String type, double x, double z, double rotation, double width, double depth) {
            Furniture f = add(type, x, z, rotation);
            f.width = width;
            f.depth = depth;
            return f;
        }

        Builder floor(SurfaceMaterial m, String color) {
            p.floor.material = m;
            p.floor.color = color;
            return this;
        }

        Builder walls(SurfaceMaterial m, String color) {
            for (WallSide side : WallSide.values()) {
                p.walls.get(side).material = m;
                p.walls.get(side).color = color;
            }
            return this;
        }

        /** Una bombilla en el centro del techo y el sol por la primera ventana. */
        Project lights() {
            Light bulb = new Light(id("point"), LightType.POINT);
            bulb.x = p.room.width() / 2;
            bulb.y = p.room.height() - 0.25;
            bulb.z = p.room.length() / 2;
            p.lights.add(bulb);
            Opening window = p.openings.stream().filter(o -> o.type == OpeningType.WINDOW).findFirst().orElse(null);
            if (window != null) {
                Light sun = new Light(id("sun"), LightType.SUN);
                sun.windowId = window.id;
                sun.elevation = 35;
                sun.deviation = 20;
                p.lights.add(sun);
            }
            return p;
        }
    }

    // ---------------------------------------------------------------- Plantillas

    private static Project bedroom(Catalog c) {
        Builder b = new Builder(c, 3.5, 3.2, 2.5)
                .window(WallSide.NORTE, 1.15, 1.2, 1.0, 0.9)
                .door(WallSide.SUR, 2.4, 0.9)
                .floor(SurfaceMaterial.MADERA, "#c9a274")
                .walls(SurfaceMaterial.PINTURA, "#e3d5bf");
        b.add("cama", 1.4, 0.95, 0); // cabecera contra la pared norte
        b.add("mesa-noche", 0.475, 0.2, 0);
        b.add("mesa-noche", 2.325, 0.2, 0);
        b.add("closet", 3.2, 0.8, 270); // contra la pared este, abre hacia el oeste
        b.add("comoda", 0.225, 2.2, 90); // contra la pared oeste
        return b.lights();
    }

    private static Project living(Catalog c) {
        Builder b = new Builder(c, 5, 4, 2.6)
                .window(WallSide.ESTE, 1.1, 1.8, 1.2, 0.8)
                .door(WallSide.SUR, 0.3, 0.9)
                .floor(SurfaceMaterial.PORCELANATO, "#c9c3b9")
                .walls(SurfaceMaterial.PINTURA, "#f2efe9");
        b.p.walls.get(WallSide.NORTE).material = SurfaceMaterial.LADRILLO; // pared del televisor
        b.p.walls.get(WallSide.NORTE).color = "#a4553a";
        b.add("rack-tv", 2.8, 0.225, 0);
        b.add("sillon", 2.8, 3.55, 180); // frente al televisor
        b.add("mesa", 2.8, 2.1, 0, 1.0, 0.5).height = 0.45; // mesa de centro
        b.add("librero", 0.15, 1.6, 90);
        if (c.contains("planta")) b.add("planta", 4.6, 3.6, 0);
        return b.lights();
    }

    private static Project dining(Catalog c) {
        Builder b = new Builder(c, 4, 3.5, 2.5)
                .window(WallSide.NORTE, 1.4, 1.2, 1.0, 0.9)
                .door(WallSide.SUR, 0.3, 0.9)
                .floor(SurfaceMaterial.MADERA, "#b07d4f")
                .walls(SurfaceMaterial.PINTURA, "#d9d7d1");
        b.add("mesa", 2.2, 1.6, 0, 1.6, 0.9);
        b.add("silla", 1.8, 0.9, 0); // lado norte, miran a la mesa (sur)
        b.add("silla", 2.6, 0.9, 0);
        b.add("silla", 1.8, 2.3, 180); // lado sur, miran al norte
        b.add("silla", 2.6, 2.3, 180);
        b.add("silla", 1.15, 1.6, 90); // cabeceras
        b.add("silla", 3.25, 1.6, 270);
        b.add("comoda", 3.775, 3.0, 270); // aparador contra la pared este
        return b.lights();
    }

    private static Project kitchen(Catalog c) {
        Builder b = new Builder(c, 3.2, 2.6, 2.5)
                .window(WallSide.NORTE, 0.9, 1.0, 0.8, 1.15)
                .door(WallSide.SUR, 2.1, 0.85)
                .floor(SurfaceMaterial.CERAMICO, "#e4e0d8")
                .walls(SurfaceMaterial.SATINADO, "#e9eef2");
        b.add("cocina", 1.2, 0.3, 0, 2.4, 0.6); // contra la pared norte
        b.add("refrigeradora", 2.75, 0.35, 0);
        b.add("cocina", 0.3, 1.2, 90, 1.2, 0.6); // tramo de la L, contra la pared oeste
        b.add("mesa", 1.5, 1.6, 0, 0.9, 0.7);
        b.add("silla", 1.5, 1.0, 0);
        b.add("silla", 1.5, 2.2, 180);
        return b.lights();
    }

    private static Project bathroom(Catalog c) {
        Builder b = new Builder(c, 2.4, 1.8, 2.4)
                .window(WallSide.NORTE, 0.9, 0.6, 0.5, 1.6)
                .door(WallSide.SUR, 1.4, 0.8)
                .floor(SurfaceMaterial.CERAMICO, "#ecebe7")
                .walls(SurfaceMaterial.CERAMICO, "#e4e0d8");
        b.add("inodoro", 0.4, 0.35, 0);
        b.add("lavatorio", 1.2, 0.225, 0);
        return b.lights();
    }

    private static Project office(Catalog c) {
        Builder b = new Builder(c, 3, 3, 2.5)
                .window(WallSide.NORTE, 0.4, 1.2, 1.0, 1.0)
                .door(WallSide.SUR, 1.9, 0.9)
                .floor(SurfaceMaterial.MADERA, "#7a5235")
                .walls(SurfaceMaterial.PINTURA, "#fafafa");
        b.add("escritorio", 1.0, 0.35, 0, 1.4, 0.7); // contra la pared norte
        b.add("silla", 1.0, 0.95, 180); // mira al escritorio
        b.add("librero", 0.175, 1.9, 90, 1.0, 0.35);
        if (c.contains("planta")) b.add("planta", 2.6, 0.4, 0);
        return b.lights();
    }

    private static Project empty() {
        Project p = new Project();
        p.openings.add(new Opening("window-1", OpeningType.WINDOW, WallSide.NORTE, 1.4, 1.2, 1.0, 0.9));
        p.openings.add(new Opening("door-1", OpeningType.DOOR, WallSide.SUR, 1.55, 0.9, 2.1, 0));
        Light bulb = new Light("point-1", LightType.POINT);
        bulb.x = 2;
        bulb.y = 2.25;
        bulb.z = 1.75;
        Light sun = new Light("sun-1", LightType.SUN);
        sun.windowId = "window-1";
        sun.elevation = 35;
        sun.deviation = 20;
        p.lights.add(bulb);
        p.lights.add(sun);
        return p;
    }
}
