package pe.edu.unaj.amoblador3d.design;

import static pe.edu.unaj.amoblador3d.design.Design.MARGIN;

import java.util.ArrayList;
import java.util.List;
import pe.edu.unaj.amoblador3d.catalog.CatalogEntry;
import pe.edu.unaj.amoblador3d.design.Field.Option;
import pe.edu.unaj.amoblador3d.model.Furniture;
import pe.edu.unaj.amoblador3d.model.Light;
import pe.edu.unaj.amoblador3d.model.LightType;
import pe.edu.unaj.amoblador3d.model.Opening;
import pe.edu.unaj.amoblador3d.model.OpeningType;
import pe.edu.unaj.amoblador3d.model.RoomDims;
import pe.edu.unaj.amoblador3d.model.Surface;
import pe.edu.unaj.amoblador3d.model.SurfaceMaterial;
import pe.edu.unaj.amoblador3d.model.WallSide;
import pe.edu.unaj.amoblador3d.util.Units;

/**
 * Campos del panel de propiedades para cada tipo de entidad.
 *
 * <p>Los límites de cada campo son los mismos que usan las reglas de {@link Design}
 * (antes el panel permitía, por ejemplo, un alféizar de 0 m que luego se rechazaba).
 */
public final class Editors {
    /** Callback para el botón "Duplicar" de los muebles. */
    public interface DuplicateHandler {
        String duplicate(Furniture f);
    }

    private Editors() {}

    public static Editable of(Design design, Object entity, DuplicateHandler onDuplicate) {
        if (entity instanceof Opening o) return new OpeningEditor(design, o);
        if (entity instanceof Light l) return new LightEditor(design, l);
        if (entity instanceof Furniture f) return new FurnitureEditor(design, f, onDuplicate);
        if (entity instanceof Surface s) return new SurfaceEditor(design, s);
        throw new IllegalArgumentException("Entidad desconocida: " + entity);
    }

    /** Revisa que un número esté dentro de los límites del campo. */
    private static String checkField(List<Field> fields, String key, Object value) {
        for (Field f : fields) {
            if (!f.key().equals(key)) continue;
            if (f.type() == Field.Type.NUMBER) {
                if (!(value instanceof Double d)) return f.label() + " debe ser un número.";
                return Units.checkRange(f.label(), d, f.min(), f.max());
            }
            return null;
        }
        return "Campo desconocido: " + key;
    }

    // ---------------------------------------------------------------- Aberturas

    private record OpeningEditor(Design design, Opening o) implements Editable {
        @Override
        public Object entity() {
            return o;
        }

        @Override
        public String title() {
            return design.label(o);
        }

        @Override
        public List<Field> fields() {
            double len = design.wallLength(o.wall);
            double h = design.room().height();
            List<Option> walls = new ArrayList<>();
            for (WallSide w : WallSide.values()) walls.add(new Option(w.id(), w.label()));
            List<Field> fields = new ArrayList<>();
            fields.add(Field.select("wall", "Pared", walls));
            fields.add(Field.meters("offset", "Posición desde esquina", MARGIN, len - o.width - MARGIN));
            fields.add(Field.meters("width", "Ancho", 0.3, len - o.offset - MARGIN));
            if (o.type == OpeningType.DOOR) {
                fields.add(Field.meters("height", "Alto", 1.5, h - MARGIN));
            } else {
                fields.add(Field.meters("height", "Alto", 0.3, h - o.sill - MARGIN));
                fields.add(Field.meters("sill", "Altura alféizar", MARGIN, h - o.height - MARGIN));
            }
            return fields;
        }

        @Override
        public Object get(String key) {
            return switch (key) {
                case "wall" -> o.wall.id();
                case "offset" -> o.offset;
                case "width" -> o.width;
                case "height" -> o.height;
                case "sill" -> o.sill;
                default -> null;
            };
        }

        @Override
        public String set(String key, Object value) {
            Opening c = o.copy();
            if (key.equals("wall")) {
                c.wall = WallSide.fromId((String) value);
                return design.changeOpening(o, c);
            }
            String error = checkField(fields(), key, value);
            if (error != null) return error;
            double v = (Double) value;
            switch (key) {
                case "offset" -> c.offset = v;
                case "width" -> c.width = v;
                case "height" -> c.height = v;
                case "sill" -> c.sill = v;
                default -> { return "Campo desconocido: " + key; }
            }
            return design.changeOpening(o, c);
        }
    }

    // ---------------------------------------------------------------- Luces

    private record LightEditor(Design design, Light l) implements Editable {
        @Override
        public Object entity() {
            return l;
        }

        @Override
        public String title() {
            return design.label(l);
        }

        @Override
        public List<Field> fields() {
            RoomDims r = design.room();
            List<Field> fields = new ArrayList<>();
            fields.add(Field.color("color", "Color"));
            fields.add(Field.number("intensity", "Intensidad", 0, l.type.maxIntensity(), 0.5, l.type.unit()));
            if (l.type != LightType.SUN) {
                fields.add(Field.meters("x", "Posición X", 0.05, r.width() - 0.05));
                fields.add(Field.meters("y", "Altura (Y)", 0.05, r.height() - 0.05));
                fields.add(Field.meters("z", "Posición Z", 0.05, r.length() - 0.05));
            }
            if (l.type == LightType.SPOT) {
                fields.add(Field.meters("targetX", "Apunta a X", 0, r.width()));
                fields.add(Field.meters("targetZ", "Apunta a Z", 0, r.length()));
                fields.add(Field.number("angle", "Apertura", 5, 80, 1, "°"));
                fields.add(Field.number("penumbra", "Suavidad del borde", 0, 1, 0.05, ""));
            }
            if (l.type == LightType.SUN) {
                List<Option> windows = new ArrayList<>();
                for (Opening w : design.windows()) windows.add(new Option(w.id, design.label(w)));
                fields.add(Field.select("windowId", "Entra por", windows));
                fields.add(Field.number("elevation", "Altura del sol", 5, 85, 1, "°"));
                fields.add(Field.number("deviation", "Desvío lateral", -60, 60, 1, "°"));
            }
            return fields;
        }

        @Override
        public Object get(String key) {
            return switch (key) {
                case "color" -> l.color;
                case "intensity" -> l.intensity;
                case "x" -> l.x;
                case "y" -> l.y;
                case "z" -> l.z;
                case "targetX" -> l.targetX;
                case "targetZ" -> l.targetZ;
                case "angle" -> l.angle;
                case "penumbra" -> l.penumbra;
                case "windowId" -> l.windowId;
                case "elevation" -> l.elevation;
                case "deviation" -> l.deviation;
                default -> null;
            };
        }

        @Override
        public String set(String key, Object value) {
            Light c = l.copy();
            switch (key) {
                case "color" -> c.color = (String) value;
                case "windowId" -> {
                    Opening w = design.findOpening((String) value);
                    if (w == null || w.type != OpeningType.WINDOW) return "Elige una ventana.";
                    c.windowId = w.id;
                }
                default -> {
                    String error = checkField(fields(), key, value);
                    if (error != null) return error;
                    double v = (Double) value;
                    switch (key) {
                        case "intensity" -> c.intensity = v;
                        case "x" -> c.x = v;
                        case "y" -> c.y = v;
                        case "z" -> c.z = v;
                        case "targetX" -> c.targetX = v;
                        case "targetZ" -> c.targetZ = v;
                        case "angle" -> c.angle = v;
                        case "penumbra" -> c.penumbra = v;
                        case "elevation" -> c.elevation = v;
                        case "deviation" -> c.deviation = v;
                        default -> { return "Campo desconocido: " + key; }
                    }
                }
            }
            return design.changeLight(l, c);
        }
    }

    // ---------------------------------------------------------------- Muebles

    private record FurnitureEditor(Design design, Furniture f, DuplicateHandler onDuplicate) implements Editable {
        @Override
        public Object entity() {
            return f;
        }

        @Override
        public String title() {
            return design.label(f);
        }

        @Override
        public List<Field> fields() {
            RoomDims r = design.room();
            CatalogEntry e = design.catalog().get(f.type);
            List<Field> fields = new ArrayList<>();
            fields.add(Field.meters("x", "Posición X", 0, r.width()));
            fields.add(Field.meters("z", "Posición Z", 0, r.length()));
            fields.add(Field.number("rotation", "Rotación", -360, 360, 1, "°"));
            fields.add(Field.meters("width", "Ancho", e.widthRange().min(), e.widthRange().max()));
            fields.add(Field.meters("depth", "Profundidad", e.depthRange().min(), e.depthRange().max()));
            fields.add(Field.meters("height", "Alto", e.heightRange().min(), e.heightRange().max()));
            // Los modelos 3D traen sus propios colores y texturas.
            if (e.kind() == CatalogEntry.Kind.PARAMETRIC) fields.add(Field.color("color", "Color"));
            return fields;
        }

        @Override
        public Object get(String key) {
            return switch (key) {
                case "x" -> f.x;
                case "z" -> f.z;
                case "rotation" -> f.rotation;
                case "width" -> f.width;
                case "depth" -> f.depth;
                case "height" -> f.height;
                case "color" -> f.color;
                default -> null;
            };
        }

        @Override
        public String set(String key, Object value) {
            Furniture c = f.copy();
            if (key.equals("color")) {
                c.color = (String) value;
                return design.changeFurniture(f, c, false);
            }
            String error = checkField(fields(), key, value);
            if (error != null) return error;
            double v = (Double) value;
            switch (key) {
                case "x" -> c.x = v;
                case "z" -> c.z = v;
                case "rotation" -> c.rotation = v;
                case "width" -> c.width = v;
                case "depth" -> c.depth = v;
                case "height" -> c.height = v;
                default -> { return "Campo desconocido: " + key; }
            }
            return design.changeFurniture(f, c, key.equals("x") || key.equals("z"));
        }

        @Override
        public List<Action> actions() {
            return List.of(
                    new Action("⟲ 90°", "Girar a la izquierda (Shift+R)", () -> design.rotateFurniture(f, 90)),
                    new Action("⟳ 90°", "Girar a la derecha (R)", () -> design.rotateFurniture(f, -90)),
                    new Action("Duplicar", "Ctrl+D", () -> onDuplicate.duplicate(f)));
        }
    }

    // ---------------------------------------------------------------- Paredes y piso

    private record SurfaceEditor(Design design, Surface s) implements Editable {
        @Override
        public Object entity() {
            return s;
        }

        @Override
        public String title() {
            return design.label(s);
        }

        @Override
        public List<Field> fields() {
            List<Option> materials = new ArrayList<>();
            for (SurfaceMaterial m : SurfaceMaterial.forSurface(s.isFloor())) materials.add(new Option(m.id(), m.label()));
            return List.of(Field.select("material", "Material", materials), Field.color("color", "Color"));
        }

        @Override
        public Object get(String key) {
            return switch (key) {
                case "material" -> s.material.id();
                case "color" -> s.color;
                default -> null;
            };
        }

        @Override
        public String set(String key, Object value) {
            return switch (key) {
                // Al cambiar de material se toma su color típico; luego se puede ajustar.
                case "material" -> {
                    SurfaceMaterial m = SurfaceMaterial.fromId((String) value);
                    yield design.changeSurface(s, m, m.defaultColor());
                }
                case "color" -> design.changeSurface(s, s.material, (String) value);
                default -> "Campo desconocido: " + key;
            };
        }

        @Override
        public List<Action> actions() {
            List<Action> actions = new ArrayList<>();
            if (!s.isFloor()) {
                actions.add(new Action("Aplicar a todas", "Usar este material y color en las 4 paredes",
                        () -> design.applyToAllWalls(s)));
            }
            actions.add(new Action("Color original", "Volver al color típico de " + s.material.label().toLowerCase(),
                    () -> design.changeSurface(s, s.material, s.material.defaultColor())));
            return actions;
        }
    }
}
