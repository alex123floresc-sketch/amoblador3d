package pe.edu.unaj.amoblador3d.design;

import static pe.edu.unaj.amoblador3d.design.Design.MARGIN;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import pe.edu.unaj.amoblador3d.catalog.Catalog;
import pe.edu.unaj.amoblador3d.catalog.CatalogEntry;
import pe.edu.unaj.amoblador3d.model.Baseboard;
import pe.edu.unaj.amoblador3d.model.Column;
import pe.edu.unaj.amoblador3d.design.Field.Option;
import pe.edu.unaj.amoblador3d.model.Finish;
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

    /** Callback para "Diseñar a partir de este": abre el Diseñador de muebles. Devuelve un error o null. */
    public interface DesignHandler {
        String designFrom(Furniture f);
    }

    /** Colores sugeridos (el selector de color sigue permitiendo cualquier otro). */
    private static final List<Option> WALL_COLORS = List.of(
            new Option("#f2efe9", "Blanco hueso"), new Option("#fafafa", "Blanco"), new Option("#d9d7d1", "Gris perla"),
            new Option("#e3d5bf", "Arena"), new Option("#c97b5a", "Terracota"), new Option("#a8b89a", "Verde salvia"),
            new Option("#b7cfe0", "Celeste"), new Option("#5e6266", "Grafito"));
    private static final List<Option> FLOOR_COLORS = List.of(
            new Option("#c9a274", "Roble claro"), new Option("#b07d4f", "Cerezo"), new Option("#7a5235", "Nogal"),
            new Option("#ecebe7", "Blanco"), new Option("#9a9a96", "Gris cemento"), new Option("#b0603f", "Terracota"),
            new Option("#7d8a96", "Gris azulado"), new Option("#2d2d2d", "Negro"));
    private static final List<Option> BASEBOARD_COLORS = List.of(
            new Option("#f7f5f0", "Blanco"), new Option("#d9d7d1", "Gris perla"), new Option("#c9a274", "Roble claro"),
            new Option("#7a5235", "Nogal"), new Option("#2d2d2d", "Negro"));
    private static final List<Option> LIGHT_COLORS = List.of(
            new Option("#ffd6a0", "Cálida"), new Option("#ffe2b8", "Cálida suave"), new Option("#fff4e0", "Neutra"),
            new Option("#eef3ff", "Fría"));
    private static final List<Option> FURNITURE_COLORS = List.of(
            new Option("#d9c6a5", "Haya"), new Option("#a0764f", "Roble"), new Option("#6d4c33", "Nogal"),
            new Option("#f4f1ea", "Blanco"), new Option("#8a8f94", "Gris"), new Option("#2f3236", "Negro"),
            new Option("#5b7fa6", "Azul"), new Option("#7a9a6e", "Verde"));
    /** Telas, frentes y detalles: colores que suelen combinar con la madera o el blanco. */
    private static final List<Option> ACCENT_COLORS = List.of(
            new Option("#f4f1ea", "Crudo"), new Option("#e3d5bf", "Arena"), new Option("#5b7fa6", "Azul"),
            new Option("#2f4a6d", "Azul marino"), new Option("#8c2f39", "Vino"), new Option("#c97b5a", "Terracota"),
            new Option("#7a9a6e", "Verde"), new Option("#d9a441", "Mostaza"), new Option("#8a8f94", "Gris"),
            new Option("#2f3236", "Negro"), new Option("#c0c0c0", "Cromado"), new Option("#b08d57", "Bronce"));

    private Editors() {}

    /** Nombre del color si es uno de los sugeridos ("Blanco hueso"), o el código "#rrggbb". */
    public static String colorName(String color) {
        for (List<Option> list : List.of(WALL_COLORS, FLOOR_COLORS, FURNITURE_COLORS, ACCENT_COLORS, BASEBOARD_COLORS)) {
            for (Option o : list) if (o.value().equalsIgnoreCase(color)) return o.label();
        }
        return color;
    }

    public static Editable of(Design design, Object entity, DuplicateHandler onDuplicate) {
        return of(design, entity, onDuplicate, null);
    }

    /** @param onDesign "Diseñar a partir de este mueble" (null: no se muestra el botón) */
    public static Editable of(Design design, Object entity, DuplicateHandler onDuplicate, DesignHandler onDesign) {
        if (entity instanceof Opening o) return new OpeningEditor(design, o);
        if (entity instanceof Light l) return new LightEditor(design, l);
        if (entity instanceof Furniture f) return new FurnitureEditor(design, f, onDuplicate, onDesign);
        if (entity instanceof Surface s) return new SurfaceEditor(design, s);
        if (entity instanceof Column c) return new ColumnEditor(design, c);
        if (entity instanceof Baseboard b) return new BaseboardEditor(design, b);
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
            fields.add(Field.color("color", "Color", LIGHT_COLORS));
            fields.add(Field.slider("intensity", "Intensidad", 0, l.type.maxIntensity(), 0.5, l.type.unit()));
            if (l.type != LightType.SUN) {
                fields.add(Field.meters("x", "Posición X", 0.05, r.width() - 0.05));
                fields.add(Field.meters("y", "Altura (Y)", 0.05, r.height() - 0.05));
                fields.add(Field.meters("z", "Posición Z", 0.05, r.length() - 0.05));
            }
            if (l.type == LightType.SPOT) {
                fields.add(Field.meters("targetX", "Apunta a X", 0, r.width()));
                fields.add(Field.meters("targetZ", "Apunta a Z", 0, r.length()));
                fields.add(Field.slider("angle", "Apertura", 5, 80, 1, "°"));
                fields.add(Field.slider("penumbra", "Suavidad del borde", 0, 1, 0.05, ""));
            }
            if (l.type == LightType.SUN) {
                List<Option> windows = new ArrayList<>();
                for (Opening w : design.windows()) windows.add(new Option(w.id, design.label(w)));
                fields.add(Field.select("windowId", "Entra por", windows));
                fields.add(Field.slider("elevation", "Altura del sol", 5, 85, 1, "°"));
                fields.add(Field.slider("deviation", "Desvío lateral", -60, 60, 1, "°"));
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

    private record FurnitureEditor(Design design, Furniture f, DuplicateHandler onDuplicate, DesignHandler onDesign)
            implements Editable {
        private static final String AUTO = "auto";

        private CatalogEntry entry() {
            return design.catalog().get(f.type);
        }

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
            CatalogEntry e = entry();
            List<Field> fields = new ArrayList<>();
            fields.add(Field.section("Medidas"));
            fields.add(Field.meters("width", "Ancho", e.widthRange().min(), e.widthRange().max()));
            fields.add(Field.meters("depth", "Profundidad", e.depthRange().min(), e.depthRange().max()));
            fields.add(Field.meters("height", "Alto", e.heightRange().min(), Math.min(e.heightRange().max(), r.height())));
            fields.add(Field.section("Posición y giro"));
            fields.add(Field.meters("x", "Posición X", 0, r.width()));
            fields.add(Field.meters("z", "Posición Z", 0, r.length()));
            fields.add(Field.slider("rotation", "Rotación", 0, 359, 1, "°"));
            fields.add(Field.section("Apariencia"));
            if (e.hasMainColor()) fields.add(Field.color("color", "Color principal", FURNITURE_COLORS));
            // Los modelos 3D traen sus propios colores y texturas: solo se pueden teñir.
            if (e.kind() == CatalogEntry.Kind.MODEL) fields.add(Field.color("color", "Tinte (opcional)", FURNITURE_COLORS));
            if (e.accentLabel() != null) fields.add(Field.color("color2", e.accentLabel(), ACCENT_COLORS));
            if (e.kind() != CatalogEntry.Kind.MISSING) {
                List<Option> finishes = new ArrayList<>();
                finishes.add(new Option(AUTO, e.kind() == CatalogEntry.Kind.MODEL ? "Original" : "Por defecto"));
                for (Finish x : Finish.values()) finishes.add(new Option(x.id(), x.label()));
                fields.add(Field.select("finish", "Acabado", finishes));
            }
            fields.add(Field.section("Opciones"));
            fields.add(Field.checkbox("locked", "Bloqueado"));
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
                // Automático: se muestra el que se está viendo (el del diseño o el típico del mueble).
                case "color2" -> f.color2 != null ? f.color2
                        : entry().design() != null ? entry().design().color2 : Catalog.autoAccent(f.type, f.color);
                case "finish" -> f.finish == null ? AUTO : f.finish.id();
                case "locked" -> f.locked;
                default -> null;
            };
        }

        @Override
        public boolean isEnabled(String key) {
            return key.equals("locked") || !f.locked;
        }

        @Override
        public boolean canDelete() {
            return !f.locked;
        }

        @Override
        public String set(String key, Object value) {
            if (key.equals("locked")) {
                design.setLocked(List.of(f), Boolean.TRUE.equals(value));
                return null;
            }
            Furniture c = f.copy();
            switch (key) {
                case "color" -> {
                    c.color = (String) value;
                    return design.changeFurniture(f, c, false);
                }
                case "color2" -> {
                    c.color2 = (String) value;
                    return design.changeFurniture(f, c, false);
                }
                case "finish" -> {
                    c.finish = Finish.fromId((String) value); // "auto" -> null
                    return design.changeFurniture(f, c, false);
                }
                case "rotation" -> {
                    // Cualquier ángulo vale: -90 es 270, 450 es 90.
                    if (!(value instanceof Double d) || !Double.isFinite(d)) return "Rotación debe ser un número.";
                    c.rotation = ((d % 360) + 360) % 360;
                    return design.changeFurniture(f, c, false);
                }
                default -> { }
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

        /** Agranda o achica las tres medidas a la vez, sin salirse de los límites del mueble. */
        private String scale(double factor) {
            CatalogEntry e = entry();
            Furniture c = f.copy();
            c.width = Units.clamp(f.width * factor, e.widthRange().min(), e.widthRange().max());
            c.depth = Units.clamp(f.depth * factor, e.depthRange().min(), e.depthRange().max());
            c.height = Units.clamp(f.height * factor, e.heightRange().min(), e.heightRange().max());
            return design.changeFurniture(f, c, false);
        }

        private String originalSize() {
            CatalogEntry.Size s = entry().defaults();
            Furniture c = f.copy();
            c.width = s.width();
            c.depth = s.depth();
            c.height = s.height();
            return design.changeFurniture(f, c, false);
        }

        private String originalColors() {
            Furniture c = f.copy();
            c.color = entry().defaultColor();
            c.color2 = null;
            c.finish = null;
            return design.changeFurniture(f, c, false);
        }

        @Override
        public List<Action> actions() {
            boolean free = !f.locked;
            CatalogEntry e = entry();
            List<Action> actions = new ArrayList<>(List.of(
                    new Action("⟲ 90°", "Girar a la izquierda (Shift+R)", () -> design.rotateFurniture(f, 90), free),
                    new Action("⟳ 90°", "Girar a la derecha (R)", () -> design.rotateFurniture(f, -90), free),
                    new Action("Duplicar", "Ctrl+D", () -> onDuplicate.duplicate(f)),
                    new Action("− 10 %", "Achicar el mueble en sus tres medidas", () -> scale(0.9), free),
                    new Action("+ 10 %", "Agrandar el mueble en sus tres medidas", () -> scale(1.1), free),
                    new Action("Medidas originales", "Volver a las medidas del catálogo", this::originalSize, free)));
            boolean customized = !Objects.equals(f.color, e.defaultColor()) || f.color2 != null || f.finish != null;
            if (customized) {
                actions.add(new Action(e.kind() == CatalogEntry.Kind.MODEL ? "Colores originales" : "Colores por defecto",
                        "Quitar los colores y el acabado elegidos", this::originalColors, free));
            }
            boolean designable = e.kind() == CatalogEntry.Kind.PARAMETRIC || e.kind() == CatalogEntry.Kind.CUSTOM;
            if (onDesign != null && designable) {
                actions.add(new Action("✎ Diseñar a partir de este…",
                        "Abrir el Diseñador de muebles con una copia editable de este mueble, pieza por pieza",
                        () -> onDesign.designFrom(f)));
            }
            return actions;
        }
    }

    // ---------------------------------------------------------------- Varios muebles

    /** Acciones para un grupo de muebles seleccionados (devuelven un error o null). */
    public interface GroupHandler {
        String duplicate(List<Furniture> group);
    }

    /** Panel de varios muebles seleccionados: se mueven, giran, duplican, bloquean o eliminan juntos. */
    public static Editable group(Design design, List<Furniture> group, GroupHandler handler) {
        return new GroupEditor(design, List.copyOf(group), handler);
    }

    private record GroupEditor(Design design, List<Furniture> group, GroupHandler handler) implements Editable {
        @Override
        public Object entity() {
            return group;
        }

        @Override
        public String title() {
            return group.size() + " muebles seleccionados";
        }

        private boolean anyLocked() {
            return design.firstLocked(group) != null;
        }

        @Override
        public List<Field> fields() {
            return List.of(Field.checkbox("locked", "Bloqueados"));
        }

        @Override
        public Object get(String key) {
            return key.equals("locked") ? group.stream().allMatch(f -> f.locked) : null;
        }

        @Override
        public String set(String key, Object value) {
            if (!key.equals("locked")) return "Campo desconocido: " + key;
            design.setLocked(group, Boolean.TRUE.equals(value));
            return null;
        }

        @Override
        public boolean canDelete() {
            return !anyLocked();
        }

        @Override
        public List<Action> actions() {
            boolean free = !anyLocked();
            return List.of(
                    new Action("⟲ 90°", "Girar el grupo a la izquierda (Shift+R)", () -> design.rotateGroup(group, 90), free),
                    new Action("⟳ 90°", "Girar el grupo a la derecha (R)", () -> design.rotateGroup(group, -90), free),
                    new Action("Duplicar", "Ctrl+D", () -> handler.duplicate(group)));
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
            for (SurfaceMaterial m : SurfaceMaterial.forSurface(s)) materials.add(new Option(m.id(), m.label()));
            return List.of(Field.gallery("material", "Material", materials),
                    Field.color("color", "Color", s.isFloor() ? FLOOR_COLORS : WALL_COLORS));
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
            if (s.wallSide() != null) {
                actions.add(new Action("Aplicar a todas", "Usar este material y color en las 4 paredes",
                        () -> design.applyToAllWalls(s)));
            }
            actions.add(new Action("Color original", "Volver al color típico de " + s.material.label().toLowerCase(),
                    () -> design.changeSurface(s, s.material, s.material.defaultColor())));
            return actions;
        }
    }

    // ---------------------------------------------------------------- Columnas, muros internos y recortes

    private record ColumnEditor(Design design, Column c) implements Editable {
        @Override
        public Object entity() {
            return c;
        }

        @Override
        public String title() {
            return design.label(c);
        }

        private boolean notch() {
            return c.kind == Column.Kind.NOTCH;
        }

        @Override
        public List<Field> fields() {
            RoomDims r = design.room();
            List<Field> fields = new ArrayList<>();
            if (notch()) {
                List<Option> corners = new ArrayList<>();
                for (Column.Corner k : Column.Corner.values()) corners.add(new Option(k.id(), k.label()));
                fields.add(Field.select("corner", "Esquina", corners));
                fields.add(Field.meters("width", "Ancho (X)", Design.MIN_COLUMN, r.width() - Design.MIN_LEFT));
                fields.add(Field.meters("depth", "Largo (Z)", Design.MIN_COLUMN, r.length() - Design.MIN_LEFT));
            } else {
                fields.add(Field.meters("x", "Posición X", c.width / 2, r.width() - c.width / 2));
                fields.add(Field.meters("z", "Posición Z", c.depth / 2, r.length() - c.depth / 2));
                fields.add(Field.meters("width", "Ancho (X)", Design.MIN_COLUMN, r.width()));
                fields.add(Field.meters("depth", "Profundidad (Z)", Design.MIN_COLUMN, r.length()));
            }
            List<Option> materials = new ArrayList<>();
            for (SurfaceMaterial m : SurfaceMaterial.forWalls()) materials.add(new Option(m.id(), m.label()));
            fields.add(Field.gallery("material", "Material", materials));
            fields.add(Field.color("color", "Color", WALL_COLORS));
            return fields;
        }

        @Override
        public Object get(String key) {
            return switch (key) {
                case "corner" -> c.corner == null ? null : c.corner.id();
                case "x" -> c.x;
                case "z" -> c.z;
                case "width" -> c.width;
                case "depth" -> c.depth;
                case "material" -> c.material.id();
                case "color" -> c.color;
                default -> null;
            };
        }

        @Override
        public String set(String key, Object value) {
            switch (key) {
                case "material" -> {
                    SurfaceMaterial m = SurfaceMaterial.fromId((String) value);
                    return design.changeColumnFinish(c, m, m.defaultColor());
                }
                case "color" -> {
                    return design.changeColumnFinish(c, c.material, (String) value);
                }
                case "corner" -> {
                    Column k = c.copy();
                    k.corner = Column.Corner.fromId((String) value);
                    return design.changeColumn(c, k);
                }
                default -> {
                    String error = checkField(fields(), key, value);
                    if (error != null) return error;
                    double v = (Double) value;
                    Column k = c.copy();
                    switch (key) {
                        case "x" -> k.x = v;
                        case "z" -> k.z = v;
                        case "width" -> k.width = v;
                        case "depth" -> k.depth = v;
                        default -> { return "Campo desconocido: " + key; }
                    }
                    return design.changeColumn(c, k);
                }
            }
        }

        @Override
        public List<Action> actions() {
            List<Action> actions = new ArrayList<>();
            if (!notch()) {
                actions.add(new Action("Girar 90°", "Intercambiar ancho y profundidad (un muro de este-oeste pasa a norte-sur)",
                        () -> {
                            Column k = c.copy();
                            k.width = c.depth;
                            k.depth = c.width;
                            return design.changeColumn(c, k);
                        }));
            }
            actions.add(new Action("Igual que las paredes", "Usar el material y color de la pared norte",
                    () -> design.changeColumnFinish(c, design.wall(WallSide.NORTE).material, design.wall(WallSide.NORTE).color)));
            return actions;
        }
    }

    // ---------------------------------------------------------------- Zócalo

    private record BaseboardEditor(Design design, Baseboard b) implements Editable {
        @Override
        public Object entity() {
            return b;
        }

        @Override
        public String title() {
            return "Zócalo";
        }

        @Override
        public List<Field> fields() {
            return List.of(Field.number("height", "Alto (0 = sin zócalo)", 0, Baseboard.MAX_HEIGHT, 0.01, "m"),
                    Field.color("color", "Color", BASEBOARD_COLORS));
        }

        @Override
        public Object get(String key) {
            return switch (key) {
                case "height" -> b.height;
                case "color" -> b.color;
                default -> null;
            };
        }

        @Override
        public String set(String key, Object value) {
            return switch (key) {
                case "height" -> {
                    String error = checkField(fields(), key, value);
                    yield error != null ? error : design.changeBaseboard((Double) value, b.color);
                }
                case "color" -> design.changeBaseboard(b.height, (String) value);
                default -> "Campo desconocido: " + key;
            };
        }

        @Override
        public boolean canDelete() {
            return false;
        }

        @Override
        public List<Action> actions() {
            return List.of(new Action("Sin zócalo", "Alto 0", () -> design.changeBaseboard(0, b.color)),
                    new Action("Original", "8 cm, blanco", () -> design.changeBaseboard(Baseboard.DEFAULT_HEIGHT, Baseboard.DEFAULT_COLOR)));
        }
    }
}
