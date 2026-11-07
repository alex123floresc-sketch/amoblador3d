package pe.edu.unaj.amoblador3d.design;

import static pe.edu.unaj.amoblador3d.design.FSHFDesign.MARGIN;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalog;
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalogEntry;
import pe.edu.unaj.amoblador3d.model.FSHFBaseboard;
import pe.edu.unaj.amoblador3d.model.FSHFColumn;
import pe.edu.unaj.amoblador3d.design.FSHFField.Option;
import pe.edu.unaj.amoblador3d.model.FSHFFinish;
import pe.edu.unaj.amoblador3d.model.FSHFFurniture;
import pe.edu.unaj.amoblador3d.model.FSHFLight;
import pe.edu.unaj.amoblador3d.model.FSHFLightType;
import pe.edu.unaj.amoblador3d.model.FSHFOpening;
import pe.edu.unaj.amoblador3d.model.FSHFOpeningType;
import pe.edu.unaj.amoblador3d.model.FSHFRoomDims;
import pe.edu.unaj.amoblador3d.model.FSHFSurface;
import pe.edu.unaj.amoblador3d.model.FSHFSurfaceMaterial;
import pe.edu.unaj.amoblador3d.model.FSHFWallSide;
import pe.edu.unaj.amoblador3d.util.FSHFUnits;

/**
 * Campos del panel de propiedades para cada tipo de entidad.
 *
 * <p>Los límites de cada campo son los mismos que usan las reglas de {@link FSHFDesign}
 * (antes el panel permitía, por ejemplo, un alféizar de 0 m que luego se rechazaba).
 */
public final class FSHFEditors {
    /** Callback para el botón "Duplicar" de los muebles. */
    public interface DuplicateHandler {
        String duplicate(FSHFFurniture f);
    }

    /** Callback para "Diseñar a partir de este": abre el Diseñador de muebles. Devuelve un error o null. */
    public interface DesignHandler {
        String designFrom(FSHFFurniture f);
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

    private FSHFEditors() {}

    /** Nombre del color si es uno de los sugeridos ("Blanco hueso"), o el código "#rrggbb". */
    public static String colorName(String color) {
        for (List<Option> list : List.of(WALL_COLORS, FLOOR_COLORS, FURNITURE_COLORS, ACCENT_COLORS, BASEBOARD_COLORS)) {
            for (Option o : list) if (o.value().equalsIgnoreCase(color)) return o.label();
        }
        return color;
    }

    public static FSHFEditable of(FSHFDesign design, Object entity, DuplicateHandler onDuplicate) {
        return of(design, entity, onDuplicate, null);
    }

    /** @param onDesign "Diseñar a partir de este mueble" (null: no se muestra el botón) */
    public static FSHFEditable of(FSHFDesign design, Object entity, DuplicateHandler onDuplicate, DesignHandler onDesign) {
        if (entity instanceof FSHFOpening o) return new OpeningEditor(design, o);
        if (entity instanceof FSHFLight l) return new LightEditor(design, l);
        if (entity instanceof FSHFFurniture f) return new FurnitureEditor(design, f, onDuplicate, onDesign);
        if (entity instanceof FSHFSurface s) return new SurfaceEditor(design, s);
        if (entity instanceof FSHFColumn c) return new ColumnEditor(design, c);
        if (entity instanceof FSHFBaseboard b) return new BaseboardEditor(design, b);
        throw new IllegalArgumentException("Entidad desconocida: " + entity);
    }

    /** Revisa que un número esté dentro de los límites del campo. */
    private static String checkField(List<FSHFField> fields, String key, Object value) {
        for (FSHFField f : fields) {
            if (!f.key().equals(key)) continue;
            if (f.type() == FSHFField.Type.NUMBER) {
                if (!(value instanceof Double d)) return f.label() + " debe ser un número.";
                return FSHFUnits.checkRange(f.label(), d, f.min(), f.max());
            }
            return null;
        }
        return "Campo desconocido: " + key;
    }

    // ---------------------------------------------------------------- Aberturas

    private record OpeningEditor(FSHFDesign design, FSHFOpening o) implements FSHFEditable {
        @Override
        public Object entity() {
            return o;
        }

        @Override
        public String title() {
            return design.label(o);
        }

        @Override
        public List<FSHFField> fields() {
            double len = design.wallLength(o.wall);
            double h = design.room().height();
            List<Option> walls = new ArrayList<>();
            for (FSHFWallSide w : FSHFWallSide.values()) walls.add(new Option(w.id(), w.label()));
            List<FSHFField> fields = new ArrayList<>();
            if (o.type.isDoor()) {
                List<Option> types = new ArrayList<>();
                for (FSHFOpeningType t : FSHFOpeningType.doors()) types.add(new Option(t.id(), t.label()));
                fields.add(FSHFField.select("type", "Tipo de puerta", types));
            }
            fields.add(FSHFField.select("wall", "Pared", walls));
            fields.add(FSHFField.meters("offset", "Posición desde esquina", MARGIN, len - o.width - MARGIN));
            fields.add(FSHFField.meters("width", "Ancho", 0.3, len - o.offset - MARGIN));
            if (o.type.isDoor()) {
                fields.add(FSHFField.meters("height", "Alto", 1.5, h - MARGIN));
            } else {
                fields.add(FSHFField.meters("height", "Alto", 0.3, h - o.sill - MARGIN));
                fields.add(FSHFField.meters("sill", "Altura alféizar", MARGIN, h - o.height - MARGIN));
            }
            return fields;
        }

        @Override
        public Object get(String key) {
            return switch (key) {
                case "type" -> o.type.id();
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
            FSHFOpening c = o.copy();
            if (key.equals("type")) {
                FSHFOpeningType t = FSHFOpeningType.fromId((String) value);
                if (!t.isDoor()) return "Elige un tipo de puerta.";
                // Primero con las medidas típicas del tipo nuevo; si no caben, con las de ahora.
                c.type = t;
                c.width = t.defaultWidth();
                c.height = Math.min(t.defaultHeight(), design.room().height() - MARGIN);
                if (design.changeOpening(o, c) == null) return null;
                FSHFOpening same = o.copy();
                same.type = t;
                return design.changeOpening(o, same);
            }
            if (key.equals("wall")) {
                c.wall = FSHFWallSide.fromId((String) value);
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

    private record LightEditor(FSHFDesign design, FSHFLight l) implements FSHFEditable {
        @Override
        public Object entity() {
            return l;
        }

        @Override
        public String title() {
            return design.label(l);
        }

        @Override
        public List<FSHFField> fields() {
            FSHFRoomDims r = design.room();
            List<FSHFField> fields = new ArrayList<>();
            fields.add(FSHFField.color("color", "Color", LIGHT_COLORS));
            fields.add(FSHFField.slider("intensity", "Intensidad", 0, l.type.maxIntensity(), 0.5, l.type.unit()));
            if (l.type != FSHFLightType.SUN) {
                fields.add(FSHFField.meters("x", "Posición X", 0.05, r.width() - 0.05));
                fields.add(FSHFField.meters("y", "Altura (Y)", 0.05, r.height() - 0.05));
                fields.add(FSHFField.meters("z", "Posición Z", 0.05, r.length() - 0.05));
            }
            if (l.type == FSHFLightType.SPOT) {
                fields.add(FSHFField.meters("targetX", "Apunta a X", 0, r.width()));
                fields.add(FSHFField.meters("targetZ", "Apunta a Z", 0, r.length()));
                fields.add(FSHFField.slider("angle", "Apertura", 5, 80, 1, "°"));
                fields.add(FSHFField.slider("penumbra", "Suavidad del borde", 0, 1, 0.05, ""));
            }
            if (l.type == FSHFLightType.SUN) {
                List<Option> windows = new ArrayList<>();
                for (FSHFOpening w : design.windows()) windows.add(new Option(w.id, design.label(w)));
                fields.add(FSHFField.select("windowId", "Entra por", windows));
                fields.add(FSHFField.slider("elevation", "Altura del sol", 5, 85, 1, "°"));
                fields.add(FSHFField.slider("deviation", "Desvío lateral", -60, 60, 1, "°"));
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
            FSHFLight c = l.copy();
            switch (key) {
                case "color" -> c.color = (String) value;
                case "windowId" -> {
                    FSHFOpening w = design.findOpening((String) value);
                    if (w == null || w.type != FSHFOpeningType.WINDOW) return "Elige una ventana.";
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

    private record FurnitureEditor(FSHFDesign design, FSHFFurniture f, DuplicateHandler onDuplicate, DesignHandler onDesign)
            implements FSHFEditable {
        private static final String AUTO = "auto";

        private FSHFCatalogEntry entry() {
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
        public List<FSHFField> fields() {
            FSHFRoomDims r = design.room();
            FSHFCatalogEntry e = entry();
            List<FSHFField> fields = new ArrayList<>();
            fields.add(FSHFField.section("Medidas"));
            fields.add(FSHFField.meters("width", "Ancho", e.widthRange().min(), e.widthRange().max()));
            fields.add(FSHFField.meters("depth", "Profundidad", e.depthRange().min(), e.depthRange().max()));
            fields.add(FSHFField.meters("height", "Alto", e.heightRange().min(), Math.min(e.heightRange().max(), r.height())));
            fields.add(FSHFField.section("Posición y giro"));
            fields.add(FSHFField.meters("x", "Posición X", 0, r.width()));
            fields.add(FSHFField.meters("z", "Posición Z", 0, r.length()));
            fields.add(FSHFField.slider("rotation", "Rotación", 0, 359, 1, "°"));
            fields.add(FSHFField.section("Apariencia"));
            if (e.hasMainColor()) fields.add(FSHFField.color("color", "Color principal", FURNITURE_COLORS));
            // Los modelos 3D traen sus propios colores y texturas: solo se pueden teñir.
            if (e.kind() == FSHFCatalogEntry.Kind.MODEL) fields.add(FSHFField.color("color", "Tinte (opcional)", FURNITURE_COLORS));
            if (e.accentLabel() != null) fields.add(FSHFField.color("color2", e.accentLabel(), ACCENT_COLORS));
            if (e.kind() != FSHFCatalogEntry.Kind.MISSING) {
                List<Option> finishes = new ArrayList<>();
                finishes.add(new Option(AUTO, e.kind() == FSHFCatalogEntry.Kind.MODEL ? "Original" : "Por defecto"));
                for (FSHFFinish x : FSHFFinish.values()) finishes.add(new Option(x.id(), x.label()));
                fields.add(FSHFField.select("finish", "Acabado", finishes));
            }
            fields.add(FSHFField.section("Opciones"));
            fields.add(FSHFField.checkbox("locked", "Bloqueado"));
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
                        : entry().design() != null ? entry().design().color2 : FSHFCatalog.autoAccent(f.type, f.color);
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
            FSHFFurniture c = f.copy();
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
                    c.finish = FSHFFinish.fromId((String) value); // "auto" -> null
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
            FSHFCatalogEntry e = entry();
            FSHFFurniture c = f.copy();
            c.width = FSHFUnits.clamp(f.width * factor, e.widthRange().min(), e.widthRange().max());
            c.depth = FSHFUnits.clamp(f.depth * factor, e.depthRange().min(), e.depthRange().max());
            c.height = FSHFUnits.clamp(f.height * factor, e.heightRange().min(), e.heightRange().max());
            return design.changeFurniture(f, c, false);
        }

        private String originalSize() {
            FSHFCatalogEntry.Size s = entry().defaults();
            FSHFFurniture c = f.copy();
            c.width = s.width();
            c.depth = s.depth();
            c.height = s.height();
            return design.changeFurniture(f, c, false);
        }

        private String originalColors() {
            FSHFFurniture c = f.copy();
            c.color = entry().defaultColor();
            c.color2 = null;
            c.finish = null;
            return design.changeFurniture(f, c, false);
        }

        @Override
        public List<Action> actions() {
            boolean free = !f.locked;
            FSHFCatalogEntry e = entry();
            List<Action> actions = new ArrayList<>(List.of(
                    new Action("⟲ 90°", "Girar a la izquierda (Shift+R)", () -> design.rotateFurniture(f, 90), free),
                    new Action("⟳ 90°", "Girar a la derecha (R)", () -> design.rotateFurniture(f, -90), free),
                    new Action("Duplicar", "Ctrl+D", () -> onDuplicate.duplicate(f)),
                    new Action("− 10 %", "Achicar el mueble en sus tres medidas", () -> scale(0.9), free),
                    new Action("+ 10 %", "Agrandar el mueble en sus tres medidas", () -> scale(1.1), free),
                    new Action("Medidas originales", "Volver a las medidas del catálogo", this::originalSize, free)));
            boolean customized = !Objects.equals(f.color, e.defaultColor()) || f.color2 != null || f.finish != null;
            if (customized) {
                actions.add(new Action(e.kind() == FSHFCatalogEntry.Kind.MODEL ? "Colores originales" : "Colores por defecto",
                        "Quitar los colores y el acabado elegidos", this::originalColors, free));
            }
            boolean designable = (e.kind() == FSHFCatalogEntry.Kind.PARAMETRIC && !FSHFCatalog.isStair(f.type))
                    || e.kind() == FSHFCatalogEntry.Kind.CUSTOM;
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
        String duplicate(List<FSHFFurniture> group);
    }

    /** Panel de varios muebles seleccionados: se mueven, giran, duplican, bloquean o eliminan juntos. */
    public static FSHFEditable group(FSHFDesign design, List<FSHFFurniture> group, GroupHandler handler) {
        return new GroupEditor(design, List.copyOf(group), handler);
    }

    private record GroupEditor(FSHFDesign design, List<FSHFFurniture> group, GroupHandler handler) implements FSHFEditable {
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
        public List<FSHFField> fields() {
            return List.of(FSHFField.checkbox("locked", "Bloqueados"));
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

    private record SurfaceEditor(FSHFDesign design, FSHFSurface s) implements FSHFEditable {
        @Override
        public Object entity() {
            return s;
        }

        @Override
        public String title() {
            return design.label(s);
        }

        @Override
        public List<FSHFField> fields() {
            List<Option> materials = new ArrayList<>();
            for (FSHFSurfaceMaterial m : FSHFSurfaceMaterial.forSurface(s)) materials.add(new Option(m.id(), m.label()));
            return List.of(FSHFField.gallery("material", "Material", materials),
                    FSHFField.color("color", "Color", s.isFloor() ? FLOOR_COLORS : WALL_COLORS));
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
                    FSHFSurfaceMaterial m = FSHFSurfaceMaterial.fromId((String) value);
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

    private record ColumnEditor(FSHFDesign design, FSHFColumn c) implements FSHFEditable {
        @Override
        public Object entity() {
            return c;
        }

        @Override
        public String title() {
            return design.label(c);
        }

        private boolean notch() {
            return c.kind == FSHFColumn.Kind.NOTCH;
        }

        @Override
        public List<FSHFField> fields() {
            FSHFRoomDims r = design.room();
            List<FSHFField> fields = new ArrayList<>();
            if (notch()) {
                List<Option> corners = new ArrayList<>();
                for (FSHFColumn.Corner k : FSHFColumn.Corner.values()) corners.add(new Option(k.id(), k.label()));
                fields.add(FSHFField.select("corner", "Esquina", corners));
                fields.add(FSHFField.meters("width", "Ancho (X)", FSHFDesign.MIN_COLUMN, r.width() - FSHFDesign.MIN_LEFT));
                fields.add(FSHFField.meters("depth", "Largo (Z)", FSHFDesign.MIN_COLUMN, r.length() - FSHFDesign.MIN_LEFT));
            } else {
                fields.add(FSHFField.meters("x", "Posición X", c.width / 2, r.width() - c.width / 2));
                fields.add(FSHFField.meters("z", "Posición Z", c.depth / 2, r.length() - c.depth / 2));
                fields.add(FSHFField.meters("width", "Ancho (X)", FSHFDesign.MIN_COLUMN, r.width()));
                fields.add(FSHFField.meters("depth", "Profundidad (Z)", FSHFDesign.MIN_COLUMN, r.length()));
            }
            List<Option> materials = new ArrayList<>();
            for (FSHFSurfaceMaterial m : FSHFSurfaceMaterial.forWalls()) materials.add(new Option(m.id(), m.label()));
            fields.add(FSHFField.gallery("material", "Material", materials));
            fields.add(FSHFField.color("color", "Color", WALL_COLORS));
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
                    FSHFSurfaceMaterial m = FSHFSurfaceMaterial.fromId((String) value);
                    return design.changeColumnFinish(c, m, m.defaultColor());
                }
                case "color" -> {
                    return design.changeColumnFinish(c, c.material, (String) value);
                }
                case "corner" -> {
                    FSHFColumn k = c.copy();
                    k.corner = FSHFColumn.Corner.fromId((String) value);
                    return design.changeColumn(c, k);
                }
                default -> {
                    String error = checkField(fields(), key, value);
                    if (error != null) return error;
                    double v = (Double) value;
                    FSHFColumn k = c.copy();
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
                            FSHFColumn k = c.copy();
                            k.width = c.depth;
                            k.depth = c.width;
                            return design.changeColumn(c, k);
                        }));
            }
            actions.add(new Action("Igual que las paredes", "Usar el material y color de la pared norte",
                    () -> design.changeColumnFinish(c, design.wall(FSHFWallSide.NORTE).material, design.wall(FSHFWallSide.NORTE).color)));
            return actions;
        }
    }

    // ---------------------------------------------------------------- Zócalo

    private record BaseboardEditor(FSHFDesign design, FSHFBaseboard b) implements FSHFEditable {
        @Override
        public Object entity() {
            return b;
        }

        @Override
        public String title() {
            return "Zócalo";
        }

        @Override
        public List<FSHFField> fields() {
            return List.of(FSHFField.number("height", "Alto (0 = sin zócalo)", 0, FSHFBaseboard.MAX_HEIGHT, 0.01, "m"),
                    FSHFField.color("color", "Color", BASEBOARD_COLORS));
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
                    new Action("Original", "8 cm, blanco", () -> design.changeBaseboard(FSHFBaseboard.DEFAULT_HEIGHT, FSHFBaseboard.DEFAULT_COLOR)));
        }
    }
}
