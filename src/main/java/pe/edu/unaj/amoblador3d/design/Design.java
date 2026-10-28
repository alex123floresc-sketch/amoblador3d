package pe.edu.unaj.amoblador3d.design;

import static pe.edu.unaj.amoblador3d.util.Units.checkRange;
import static pe.edu.unaj.amoblador3d.util.Units.round;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import pe.edu.unaj.amoblador3d.catalog.Catalog;
import pe.edu.unaj.amoblador3d.catalog.CatalogEntry;
import pe.edu.unaj.amoblador3d.model.Baseboard;
import pe.edu.unaj.amoblador3d.model.CameraView;
import pe.edu.unaj.amoblador3d.model.Column;
import pe.edu.unaj.amoblador3d.model.Footprint;
import pe.edu.unaj.amoblador3d.model.Furniture;
import pe.edu.unaj.amoblador3d.model.Light;
import pe.edu.unaj.amoblador3d.model.LightType;
import pe.edu.unaj.amoblador3d.model.Opening;
import pe.edu.unaj.amoblador3d.model.OpeningType;
import pe.edu.unaj.amoblador3d.model.Project;
import pe.edu.unaj.amoblador3d.model.RoomDims;
import pe.edu.unaj.amoblador3d.model.Surface;
import pe.edu.unaj.amoblador3d.model.SurfaceMaterial;
import pe.edu.unaj.amoblador3d.model.WallSide;
import pe.edu.unaj.amoblador3d.util.Outcome;
import pe.edu.unaj.amoblador3d.util.Units;

/**
 * El proyecto que se está editando y todas sus reglas: dónde caben las aberturas,
 * colisiones de muebles (con paredes, con otros muebles y con el giro de las puertas)
 * y luces que dependen de las ventanas.
 *
 * <p>No depende de JavaFX: la vista 3D se entera de los cambios con {@link #addListener}.
 */
public final class Design {
    /** Separación mínima de una abertura con esquinas, techo y otras aberturas. */
    public static final double MARGIN = 0.05;
    /** JavaFX ilumina cada objeto con un máximo de 3 luces (sin contar la ambiental). */
    public static final int MAX_ACTIVE_LIGHTS = 3;

    /** Qué parte cambió, para que la vista 3D reconstruya solo eso. */
    public enum Change { ROOM, SURFACES, LIGHTS, FURNITURE, VIEWS }

    private final Catalog catalog;
    private final IdGenerator ids = new IdGenerator();
    private final List<Consumer<Set<Change>>> listeners = new ArrayList<>();
    private Consumer<String> onWarning = message -> {};
    private Project project = new Project();

    public Design(Catalog catalog) {
        this.catalog = catalog;
    }

    public Catalog catalog() {
        return catalog;
    }

    public RoomDims room() {
        return project.room;
    }

    public double ambient() {
        return project.ambient;
    }

    public List<Opening> openings() {
        return List.copyOf(project.openings);
    }

    public List<Light> lights() {
        return List.copyOf(project.lights);
    }

    public List<Furniture> furniture() {
        return List.copyOf(project.furniture);
    }

    public List<CameraView> views() {
        return List.copyOf(project.views);
    }

    /** Guarda una vista de cámara (si ya hay una con ese nombre, la reemplaza). */
    public void saveView(CameraView view) {
        String name = view.name() == null ? "" : view.name().trim();
        if (name.length() > CameraView.MAX_NAME) name = name.substring(0, CameraView.MAX_NAME);
        CameraView v = new CameraView(name, view.kind(), view.x(), view.y(), view.z(), view.yaw(), view.pitch(), view.distance());
        if (!v.isValid()) throw new IllegalArgumentException("La vista no es válida.");
        int i = indexOfView(v.name());
        if (i >= 0) project.views.set(i, v);
        else project.views.add(v);
        fire(Change.VIEWS);
    }

    public void removeView(String name) {
        int i = indexOfView(name);
        if (i < 0) return;
        project.views.remove(i);
        fire(Change.VIEWS);
    }

    private int indexOfView(String name) {
        for (int i = 0; i < project.views.size(); i++) {
            if (project.views.get(i).name().equalsIgnoreCase(name.trim())) return i;
        }
        return -1;
    }

    public Surface ceiling() {
        return project.ceiling;
    }

    public Baseboard baseboard() {
        return project.baseboard;
    }

    public List<Column> columns() {
        return List.copyOf(project.columns);
    }

    public Surface floor() {
        return project.floor;
    }

    public Surface wall(WallSide side) {
        return project.walls.get(side);
    }

    /** Copia independiente del estado actual (para guardar o para deshacer). */
    public Project snapshot() {
        return project.copy();
    }

    public void addListener(Consumer<Set<Change>> listener) {
        listeners.add(listener);
    }

    /** Recibe avisos que no son respuesta directa a una acción (p. ej. "el sol se eliminó"). */
    public void setWarningHandler(Consumer<String> handler) {
        onWarning = handler;
    }

    private void fire(Change first, Change... rest) {
        Set<Change> changes = EnumSet.of(first, rest);
        for (Consumer<Set<Change>> l : listeners) l.accept(changes);
    }

    private void warn(List<String> warnings) {
        if (!warnings.isEmpty()) onWarning.accept(String.join("\n", warnings));
    }

    /** Todas las entidades seleccionables (piso, paredes, techo y zócalo no se pueden eliminar). */
    public List<Object> entities() {
        List<Object> all = new ArrayList<>(project.surfaces());
        all.add(project.baseboard);
        all.addAll(project.openings);
        all.addAll(project.lights);
        all.addAll(project.furniture);
        all.addAll(project.columns);
        return all;
    }

    /** Busca una entidad por id (para conservar la selección al deshacer). */
    public Object findById(String id) {
        for (Object e : entities()) if (idOf(e).equals(id)) return e;
        return null;
    }

    public static String idOf(Object entity) {
        if (entity instanceof Opening o) return o.id;
        if (entity instanceof Light l) return l.id;
        if (entity instanceof Furniture f) return f.id;
        if (entity instanceof Surface s) return s.id;
        if (entity instanceof Column c) return c.id;
        if (entity instanceof Baseboard) return Baseboard.ID;
        throw new IllegalArgumentException("Entidad desconocida: " + entity);
    }

    /** Nombre visible: "Ventana 1 (norte)", "Bombilla 2", "Cama 1"... */
    public String label(Object entity) {
        if (entity instanceof Opening o) return o.type.label() + " " + suffix(o.id) + " (" + o.wall.id() + ")";
        if (entity instanceof Light l) return l.type.label() + " " + suffix(l.id);
        if (entity instanceof Furniture f) return catalog.get(f.type).name() + " " + suffix(f.id);
        if (entity instanceof Surface s) return s.isFloor() ? "Piso" : s.isCeiling() ? "Techo" : "Pared " + s.wallSide().id();
        if (entity instanceof Column c) {
            return c.kind == Column.Kind.NOTCH ? c.kind.label() + " (" + c.corner.id() + ")" : c.kind.label() + " " + suffix(c.id);
        }
        if (entity instanceof Baseboard) return "Zócalo";
        return String.valueOf(entity);
    }

    private static String suffix(String id) {
        return id.substring(id.lastIndexOf('-') + 1);
    }

    // ---------------------------------------------------------------- Proyecto

    /**
     * Reemplaza todo el contenido por el del proyecto (que ya debe estar validado,
     * ver {@code ProjectJson}). Devuelve avisos, por ejemplo ventanas que no cabían.
     */
    public List<String> load(Project data) {
        project = data.copy();
        ids.reset();
        for (Object e : entities()) ids.register(idOf(e));
        List<String> warnings = fitColumnsToRoom();
        warnings.addAll(fitOpenings());
        warnings.addAll(refreshSunLights());
        fire(Change.ROOM, Change.SURFACES, Change.LIGHTS, Change.FURNITURE, Change.VIEWS);
        return warnings;
    }

    /** Habitación por defecto: una ventana, una puerta, una bombilla y el sol. */
    public void loadDefault() {
        Project p = new Project();
        load(p);
        addOpening(OpeningType.WINDOW);
        addOpening(OpeningType.DOOR);
        addLight(LightType.POINT);
        addLight(LightType.SUN);
    }

    /** Cambia las medidas de la habitación y acomoda lo que haya dentro. Devuelve avisos. */
    public List<String> setRoom(RoomDims dims) {
        project.room = dims.rounded();
        List<String> warnings = fitColumnsToRoom();
        warnings.addAll(fitOpenings());
        warnings.addAll(fitLightsToRoom());
        warnings.addAll(fitFurnitureToRoom());
        fire(Change.ROOM, Change.LIGHTS, Change.FURNITURE);
        return warnings;
    }

    public void setAmbient(double intensity) {
        project.ambient = Units.clamp(intensity, 0, 2);
        fire(Change.LIGHTS);
    }

    /** Elimina una abertura, luz o mueble. Devuelve un error (mueble bloqueado) o null. */
    public String remove(Object entity) {
        if (entity instanceof Furniture f && f.locked) return label(f) + LOCKED;
        if (entity instanceof Opening o) {
            project.openings.remove(o);
            openingsChanged();
        } else if (entity instanceof Light l) {
            project.lights.remove(l);
            fire(Change.LIGHTS);
        } else if (entity instanceof Furniture f) {
            project.furniture.remove(f);
            fire(Change.FURNITURE);
        } else if (entity instanceof Column c) {
            project.columns.remove(c);
            fire(Change.ROOM);
        }
        return null;
    }

    // ---------------------------------------------------------------- Paredes y piso

    /** Cambia el material y/o el color de una pared, el piso o el techo. Devuelve un error o null. */
    String changeSurface(Surface s, SurfaceMaterial material, String color) {
        if (!material.allowedOn(s)) {
            return material.label() + " no se puede usar " + (s.isFloor() ? "en el piso." : s.isCeiling() ? "en el techo." : "en las paredes.");
        }
        s.material = material;
        s.color = color;
        fire(Change.SURFACES);
        return null;
    }

    /** Copia el acabado de una pared a las otras tres. */
    String applyToAllWalls(Surface source) {
        for (Surface w : project.walls.values()) {
            w.material = source.material;
            w.color = source.color;
        }
        fire(Change.SURFACES);
        return null;
    }

    /** Alto (0 = sin zócalo) y color del zócalo. Devuelve un error o null. */
    String changeBaseboard(double height, String color) {
        String err = checkRange("El alto del zócalo", height, 0, Baseboard.MAX_HEIGHT);
        if (err != null) return err;
        project.baseboard.height = round(height);
        project.baseboard.color = color;
        fire(Change.ROOM);
        return null;
    }

    // ---------------------------------------------------------------- Aberturas

    public double wallLength(WallSide wall) {
        return project.room.wallLength(wall);
    }

    /** Agrega una ventana o puerta en el primer lugar libre. */
    public Outcome<Opening> addOpening(OpeningType type) {
        Opening o = Opening.create(ids.next(type.id()), type);
        // Las puertas empiezan por el sur y las ventanas por el norte.
        List<WallSide> order = type == OpeningType.DOOR
                ? List.of(WallSide.SUR, WallSide.OESTE, WallSide.ESTE, WallSide.NORTE)
                : List.of(WallSide.NORTE, WallSide.ESTE, WallSide.OESTE, WallSide.SUR);
        for (WallSide wall : order) {
            o.wall = wall;
            Double offset = findFreeOffset(o, null);
            if (offset != null) {
                o.offset = offset;
                project.openings.add(o);
                openingsChanged();
                return Outcome.ok(o);
            }
        }
        String what = type == OpeningType.DOOR ? "puerta" : "ventana";
        return Outcome.fail("No hay espacio libre en las paredes para otra " + what + ".");
    }

    /**
     * Revisa que la abertura quepa en su pared y no choque con otras.
     *
     * @param self           la abertura que se está editando (se ignora a sí misma), o null
     * @param checkFurniture si es una puerta, comprobar también que su giro no choque con muebles
     * @return un mensaje de error o null
     */
    String validateOpening(Opening o, Opening self, boolean checkFurniture) {
        double len = wallLength(o.wall);
        double h = project.room.height();
        boolean door = o.type == OpeningType.DOOR;
        String name = door ? "La puerta" : "La ventana";

        String err = checkRange("El ancho", o.width, 0.3, len - 2 * MARGIN);
        if (err == null) err = checkRange("La posición", o.offset, MARGIN, len - o.width - MARGIN);
        if (err == null && door) err = checkRange("El alto", o.height, 1.5, h - MARGIN);
        if (err == null && !door) err = checkRange("El alféizar", o.sill, MARGIN, h - o.height - MARGIN);
        if (err == null && !door) err = checkRange("El alto", o.height, 0.3, h - o.sill - MARGIN);
        if (err != null) return err;
        if (door && o.sill != 0) return "Las puertas siempre empiezan en el piso.";

        for (Opening other : project.openings) {
            if (other == self || other.wall != o.wall) continue;
            boolean separated = o.offset >= other.offset + other.width + MARGIN || o.offset + o.width <= other.offset - MARGIN;
            if (!separated) return name + " se superpone con " + label(other) + ".";
        }
        for (Column c : project.columns) {
            if (covers(c, o)) return name + " quedaría tapada por " + label(c).toLowerCase() + ".";
            if (door && c.footprint().overlaps(doorZone(o))) return name + " no podría abrirse: choca con " + label(c).toLowerCase() + ".";
        }
        // Corrección: antes se podía poner una puerta encima de un mueble y quedaba bloqueada.
        if (door && checkFurniture) {
            Footprint zone = doorZone(o);
            for (Furniture f : project.furniture) {
                if (Footprint.of(f).overlaps(zone)) return name + " no podría abrirse: choca con " + label(f) + ".";
            }
        }
        return null;
    }

    /** Busca una posición libre en la pared (primero el centro). Devuelve el offset o null. */
    private Double findFreeOffset(Opening o, Opening self) {
        double len = wallLength(o.wall);
        List<Double> candidates = new ArrayList<>();
        candidates.add(round((len - o.width) / 2));
        int steps = (int) Math.floor((len - o.width - 2 * MARGIN) / 0.05 + 1e-9);
        for (int i = 0; i <= steps; i++) candidates.add(round(MARGIN + i * 0.05));
        Opening test = o.copy();
        for (double offset : candidates) {
            test.offset = offset;
            if (validateOpening(test, self, true) == null) return offset;
        }
        return null;
    }

    /**
     * Aplica los cambios de una abertura si son válidos.
     * Si cambió de pared, se busca un lugar libre en la nueva pared.
     */
    String changeOpening(Opening o, Opening candidate) {
        if (candidate.wall != o.wall) {
            Double offset = findFreeOffset(candidate, o);
            if (offset == null) return "No hay espacio libre en la pared " + candidate.wall.id() + ".";
            candidate.offset = offset;
        }
        candidate.offset = round(candidate.offset);
        candidate.width = round(candidate.width);
        candidate.height = round(candidate.height);
        candidate.sill = round(candidate.sill);
        String error = validateOpening(candidate, o, true);
        if (error != null) return error;
        o.wall = candidate.wall;
        o.offset = candidate.offset;
        o.width = candidate.width;
        o.height = candidate.height;
        o.sill = candidate.sill;
        openingsChanged();
        return null;
    }

    private void openingsChanged() {
        List<String> warnings = refreshSunLights();
        fire(Change.ROOM, Change.LIGHTS);
        warn(warnings);
    }

    /**
     * Tras cambiar las medidas, intenta que cada abertura siga cabiendo
     * (achicándola o moviéndola). Si no es posible, la elimina.
     */
    private List<String> fitOpenings() {
        List<String> warnings = new ArrayList<>();
        double h = project.room.height();
        List<Opening> all = new ArrayList<>(project.openings);
        project.openings.clear(); // se van agregando las que sí caben
        for (Opening o : all) {
            double len = wallLength(o.wall);
            Opening fitted = o.copy();
            fitted.width = round(Math.min(o.width, len - 0.1));
            if (o.type == OpeningType.DOOR) {
                fitted.height = round(Math.min(o.height, h - 0.05));
                fitted.sill = 0;
            } else {
                fitted.height = round(Math.min(o.height, h - 0.15));
                fitted.sill = round(Math.max(0.05, Math.min(o.sill, h - fitted.height - 0.05)));
            }
            fitted.offset = round(Math.max(0.05, Math.min(o.offset, len - fitted.width - 0.05)));
            // Los muebles no impiden conservar la puerta: fitFurnitureToRoom avisará si bloquean el paso.
            if (validateOpening(fitted, o, false) != null) {
                warnings.add(label(o) + " ya no cabía y se eliminó.");
            } else {
                o.offset = fitted.offset;
                o.width = fitted.width;
                o.height = fitted.height;
                o.sill = fitted.sill;
                project.openings.add(o);
            }
        }
        return warnings;
    }

    /**
     * Zona de giro de una puerta: un cuadrado de lado = ancho de la puerta, pegado a la
     * pared por el lado interior. Ningún mueble puede invadirla.
     */
    public Footprint doorZone(Opening door) {
        double w = door.width, along = door.offset + w / 2;
        RoomDims r = project.room;
        return switch (door.wall) {
            case NORTE -> new Footprint(along, w / 2, w / 2, w / 2, 0);
            case SUR -> new Footprint(along, r.length() - w / 2, w / 2, w / 2, 0);
            case OESTE -> new Footprint(w / 2, along, w / 2, w / 2, 0);
            case ESTE -> new Footprint(r.width() - w / 2, along, w / 2, w / 2, 0);
        };
    }

    public Opening findOpening(String id) {
        for (Opening o : project.openings) if (o.id.equals(id)) return o;
        return null;
    }

    // ---------------------------------------------------------------- Luces

    public Outcome<Light> addLight(LightType type) {
        RoomDims r = project.room;
        Light l = new Light(null, type);
        switch (type) {
            case POINT -> {
                l.x = r.width() / 2;
                l.y = r.height() - 0.25;
                l.z = r.length() / 2;
            }
            case SPOT -> {
                l.x = r.width() * 0.25;
                l.y = r.height() - 0.05;
                l.z = r.length() * 0.3;
                l.targetX = l.x;
                l.targetZ = l.z;
                l.angle = 30;
                l.penumbra = 0.4;
            }
            case SUN -> {
                Opening window = firstWindow();
                if (window == null) return Outcome.fail("Agrega primero una ventana: la luz del sol entra por ella.");
                l.windowId = window.id;
                l.elevation = 35;
                l.deviation = 20;
            }
        }
        l.id = ids.next(type.id());
        roundLight(l);
        project.lights.add(l);
        fire(Change.LIGHTS);
        if (project.lights.size() > MAX_ACTIVE_LIGHTS) {
            onWarning.accept("JavaFX ilumina cada objeto con " + MAX_ACTIVE_LIGHTS
                    + " luces como máximo: las luces adicionales pueden no verse.");
        }
        return Outcome.ok(l);
    }

    String changeLight(Light l, Light candidate) {
        roundLight(candidate);
        if (candidate.type != LightType.SUN) {
            for (Column c : project.columns) {
                if (inside(c, candidate.x, candidate.z)) return "Quedaría dentro de " + label(c).toLowerCase() + ".";
            }
        }
        l.color = candidate.color;
        l.intensity = candidate.intensity;
        l.castShadow = candidate.castShadow;
        l.x = candidate.x;
        l.y = candidate.y;
        l.z = candidate.z;
        l.targetX = candidate.targetX;
        l.targetZ = candidate.targetZ;
        l.angle = candidate.angle;
        l.penumbra = candidate.penumbra;
        l.windowId = candidate.windowId;
        l.elevation = candidate.elevation;
        l.deviation = candidate.deviation;
        fire(Change.LIGHTS);
        return null;
    }

    public List<Opening> windows() {
        return project.openings.stream().filter(o -> o.type == OpeningType.WINDOW).toList();
    }

    private Opening firstWindow() {
        List<Opening> windows = windows();
        return windows.isEmpty() ? null : windows.get(0);
    }

    /** Si la ventana de un sol desaparece, lo pasa a otra ventana (o lo elimina si no hay). */
    private List<String> refreshSunLights() {
        List<String> warnings = new ArrayList<>();
        for (Light l : List.copyOf(project.lights)) {
            if (l.type != LightType.SUN) continue;
            Opening window = findOpening(l.windowId == null ? "" : l.windowId);
            if (window != null && window.type == OpeningType.WINDOW) continue;
            Opening first = firstWindow();
            if (first == null) {
                warnings.add(label(l) + " se eliminó porque ya no hay ventanas.");
                project.lights.remove(l);
            } else {
                l.windowId = first.id;
            }
        }
        return warnings;
    }

    /** Tras cambiar las medidas de la habitación, mete las luces dentro. */
    private List<String> fitLightsToRoom() {
        RoomDims r = project.room;
        for (Light l : project.lights) {
            if (l.type == LightType.SUN) continue;
            l.x = clampInside(l.x, r.width());
            l.y = clampInside(l.y, r.height());
            l.z = clampInside(l.z, r.length());
            if (l.type == LightType.SPOT) {
                l.targetX = clampInside(l.targetX, r.width());
                l.targetZ = clampInside(l.targetZ, r.length());
            }
            roundLight(l);
        }
        return refreshSunLights();
    }

    private static double clampInside(double v, double max) {
        return Units.clamp(v, 0.05, max - 0.05);
    }

    private static void roundLight(Light l) {
        l.intensity = round(l.intensity);
        l.x = round(l.x);
        l.y = round(l.y);
        l.z = round(l.z);
        l.targetX = round(l.targetX);
        l.targetZ = round(l.targetZ);
        l.angle = round(l.angle);
        l.penumbra = round(l.penumbra);
        l.elevation = round(l.elevation);
        l.deviation = round(l.deviation);
    }

    // ---------------------------------------------------------------- Muebles

    /** Agrega un mueble del catálogo en el lugar libre más cercano al centro. */
    public Outcome<Furniture> addFurniture(String type) {
        CatalogEntry e = catalog.get(type);
        CatalogEntry.Size s = e.defaults();
        Furniture f = new Furniture(ids.next(type), type, 0, 0, 0, s.width(), s.depth(), s.height(), e.defaultColor());
        return placeNew(f, e.name(), null);
    }

    /** Agrega un mueble del catálogo en (x, z) o, si ahí no cabe, en el lugar libre más cercano. */
    public Outcome<Furniture> addFurnitureAt(String type, double x, double z) {
        CatalogEntry e = catalog.get(type);
        if (e.kind() == CatalogEntry.Kind.MISSING) return Outcome.fail("No existe \"" + type + "\" en el catálogo.");
        CatalogEntry.Size s = e.defaults();
        RoomDims r = project.room;
        Furniture f = new Furniture(ids.next(type), type, round(Units.clamp(x, 0, r.width())), round(Units.clamp(z, 0, r.length())),
                0, s.width(), s.depth(), s.height(), e.defaultColor());
        return placeNew(f, e.name(), f);
    }

    /** Crea una copia del mueble en el lugar libre más cercano al original. */
    public Outcome<Furniture> duplicate(Furniture source) {
        Furniture f = source.copy();
        f.id = ids.next(source.type);
        f.locked = false;
        return placeNew(f, catalog.get(source.type).name(), source);
    }

    private Outcome<Furniture> placeNew(Furniture f, String name, Furniture near) {
        double[] spot = findFreeSpot(f, near);
        if (spot == null) {
            // Corrección: antes nunca se probaba girar el mueble, y en una habitación de 4 x 3.5 m
            // solo cabía una cama aunque girada cabía otra.
            Furniture turned = f.copy();
            turned.rotation = (f.rotation + 90) % 360;
            spot = findFreeSpot(turned, near);
            if (spot != null) f.rotation = turned.rotation;
        }
        if (spot == null) {
            return Outcome.fail("No hay espacio libre para " + name.toLowerCase() + " ("
                    + Units.number(f.width) + " x " + Units.number(f.depth) + " m).");
        }
        f.x = spot[0];
        f.z = spot[1];
        project.furniture.add(f);
        fire(Change.FURNITURE);
        return Outcome.ok(f);
    }

    /** Revisa si el mueble puede estar con la posición/medidas de {@code candidate}. Devuelve un error o null. */
    public String checkFurniture(Furniture self, Furniture candidate) {
        return check(candidate, self == null ? Set.of() : Collections.singleton(self));
    }

    /** Igual que {@link #checkFurniture}, sin chocar con los muebles de {@code ignore} (los de un grupo que se mueve). */
    private String check(Furniture candidate, Set<Furniture> ignore) {
        RoomDims r = project.room;
        if (candidate.height > r.height() + 1e-9) {
            return "Es más alto (" + Units.number(candidate.height) + " m) que la habitación ("
                    + Units.number(r.height()) + " m).";
        }
        Footprint fp = Footprint.of(candidate);
        if (!fp.insideRoom(r.width(), r.length())) return "No cabe dentro de la habitación en esa posición.";
        for (Furniture other : project.furniture) {
            if (!ignore.contains(other) && fp.overlaps(Footprint.of(other))) return "Choca con " + label(other) + ".";
        }
        for (Column c : project.columns) {
            if (fp.overlaps(c.footprint())) return "Choca con " + label(c).toLowerCase() + ".";
        }
        for (Opening o : project.openings) {
            if (o.type == OpeningType.DOOR && fp.overlaps(doorZone(o))) return "Bloquea el paso de " + label(o) + ".";
        }
        return null;
    }

    /**
     * Mueve el mueble lo más cerca posible de (x, z), usado al arrastrar.
     * Si choca, intenta deslizarlo solo en X o solo en Z (así "resbala" por las
     * paredes y otros muebles en lugar de quedarse pegado).
     */
    public boolean moveFurniture(Furniture f, double x, double z) {
        if (f.locked) return false;
        RoomDims r = project.room;
        Footprint target = Footprint.of(f).withCenter(round(x), round(z)).clampToRoom(r.width(), r.length());
        double[][] tries = {{target.x(), target.z()}, {target.x(), f.z}, {f.x, target.z()}};
        Furniture candidate = f.copy();
        for (double[] pos : tries) {
            candidate.x = round(pos[0]);
            candidate.z = round(pos[1]);
            if (candidate.x == f.x && candidate.z == f.z) continue;
            if (checkFurniture(f, candidate) == null) {
                f.x = candidate.x;
                f.z = candidate.z;
                fire(Change.FURNITURE);
                return true;
            }
        }
        return false;
    }

    /** Aplica medidas, giro, posición o color si el resultado es válido. */
    String changeFurniture(Furniture f, Furniture candidate, boolean positionEdited) {
        if (f.locked) return label(f) + LOCKED;
        candidate.rotation = ((candidate.rotation % 360) + 360) % 360;
        candidate.x = round(candidate.x);
        candidate.z = round(candidate.z);
        candidate.width = round(candidate.width);
        candidate.depth = round(candidate.depth);
        candidate.height = round(candidate.height);
        // Corrección: antes un mueble que ya chocaba con algo (p. ej. tras achicar la habitación)
        // no dejaba cambiar su color. Si solo cambia el aspecto, no hay nada que revisar.
        boolean sameGeometry = candidate.x == f.x && candidate.z == f.z && candidate.rotation == f.rotation
                && candidate.width == f.width && candidate.depth == f.depth && candidate.height == f.height;
        if (!sameGeometry) {
            // Si al girar o agrandar el mueble se sale por una pared, lo empujamos hacia adentro.
            if (!positionEdited) {
                RoomDims r = project.room;
                Footprint fp = Footprint.of(candidate).clampToRoom(r.width(), r.length());
                candidate.x = round(fp.x());
                candidate.z = round(fp.z());
            }
            String conflict = checkFurniture(f, candidate);
            if (conflict != null) return conflict;
        }
        f.x = candidate.x;
        f.z = candidate.z;
        f.rotation = candidate.rotation;
        f.width = candidate.width;
        f.depth = candidate.depth;
        f.height = candidate.height;
        f.color = candidate.color;
        f.color2 = candidate.color2;
        f.finish = candidate.finish;
        fire(Change.FURNITURE);
        return null;
    }

    /**
     * Convierte el mueble en otro del catálogo (p. ej. en el diseño hecho a partir de él), con las
     * medidas y colores iniciales del nuevo tipo y en el mismo lugar. Devuelve un error o null.
     */
    public String replaceType(Furniture f, String type) {
        if (f.locked) return label(f) + LOCKED;
        CatalogEntry e = catalog.get(type);
        if (e.kind() == CatalogEntry.Kind.MISSING) return "No existe \"" + type + "\" en el catálogo.";
        Furniture c = f.copy();
        c.type = type;
        c.width = e.defaults().width();
        c.depth = e.defaults().depth();
        c.height = e.defaults().height();
        c.color = e.defaultColor();
        c.color2 = null;
        c.finish = null;
        RoomDims r = project.room;
        Footprint fp = Footprint.of(c).clampToRoom(r.width(), r.length());
        c.x = round(fp.x());
        c.z = round(fp.z());
        String conflict = checkFurniture(f, c);
        if (conflict != null) return conflict;
        String oldPrefix = f.type;
        f.type = type;
        f.x = c.x;
        f.z = c.z;
        f.width = c.width;
        f.depth = c.depth;
        f.height = c.height;
        f.color = c.color;
        f.color2 = null;
        f.finish = null;
        // El id sigue el nombre del tipo ("cama-2" -> "diseno-xyz-1") para que la etiqueta no confunda.
        if (f.id.startsWith(oldPrefix + "-")) f.id = ids.next(type);
        fire(Change.FURNITURE);
        return null;
    }

    /**
     * Cambia medidas, posición y giro a la vez (asas de la vista 3D). Con {@code keepPosition} el
     * centro se queda donde se pidió; si no, se empuja hacia adentro si se sale por una pared.
     * Devuelve un error o null.
     */
    public String reshape(Furniture f, Furniture candidate, boolean keepPosition) {
        return changeFurniture(f, candidate, keepPosition);
    }

    public String rotateFurniture(Furniture f, double degrees) {
        Furniture candidate = f.copy();
        candidate.rotation = f.rotation + degrees;
        return changeFurniture(f, candidate, false);
    }

    /** Tras cambiar las medidas de la habitación, mete los muebles dentro. Devuelve avisos. */
    private List<String> fitFurnitureToRoom() {
        RoomDims r = project.room;
        List<String> warnings = new ArrayList<>();
        for (Furniture f : project.furniture) {
            Footprint fp = Footprint.of(f).clampToRoom(r.width(), r.length());
            f.x = round(fp.x());
            f.z = round(fp.z());
            String error = checkFurniture(f, f);
            if (error != null) warnings.add(label(f) + ": " + error);
        }
        return warnings;
    }

    /**
     * Prueba posiciones en una cuadrícula de 10 cm, empezando por las más
     * cercanas a {@code near} (por defecto, el centro de la habitación).
     */
    private double[] findFreeSpot(Furniture f, Furniture near) {
        RoomDims r = project.room;
        double ox = near != null ? near.x : r.width() / 2;
        double oz = near != null ? near.z : r.length() / 2;
        List<double[]> candidates = new ArrayList<>();
        int nx = (int) Math.floor(r.width() * 10 + 1e-9), nz = (int) Math.floor(r.length() * 10 + 1e-9);
        for (int i = 0; i <= nx; i++) {
            for (int j = 0; j <= nz; j++) candidates.add(new double[] {round(i * 0.1), round(j * 0.1)});
        }
        candidates.sort(Comparator.comparingDouble(p -> Math.hypot(p[0] - ox, p[1] - oz)));
        Furniture test = f.copy();
        for (double[] p : candidates) {
            test.x = p[0];
            test.z = p[1];
            if (checkFurniture(null, test) == null) return p;
        }
        return null;
    }

    // ---------------------------------------------------------------- Bloqueo y grupos de muebles

    private static final String LOCKED = " está bloqueado: desbloquéalo para cambiarlo.";

    /** Bloquea o desbloquea muebles (un mueble bloqueado no se mueve, gira, cambia ni elimina). */
    public void setLocked(Collection<Furniture> group, boolean locked) {
        for (Furniture f : group) f.locked = locked;
        fire(Change.FURNITURE);
    }

    /** El primer mueble bloqueado del grupo, o null. */
    public Furniture firstLocked(Collection<Furniture> group) {
        for (Furniture f : group) if (f.locked) return f;
        return null;
    }

    private static Set<Furniture> identitySet(Collection<Furniture> items) {
        Set<Furniture> set = Collections.newSetFromMap(new IdentityHashMap<>());
        set.addAll(items);
        return set;
    }

    private static Snapper.Box boxOf(Collection<Furniture> group) {
        Snapper.Box box = null;
        for (Furniture f : group) {
            Snapper.Box b = Snapper.Box.of(Footprint.of(f));
            box = box == null ? b : box.union(b);
        }
        return box;
    }

    /**
     * Revisa que el grupo pueda quedar como {@code candidates} (misma posición en la lista):
     * cada uno contra la habitación, las puertas y los muebles de fuera del grupo, y entre ellos.
     */
    private String checkGroup(List<Furniture> group, List<Furniture> candidates) {
        Set<Furniture> ignore = identitySet(group);
        for (int i = 0; i < candidates.size(); i++) {
            String err = check(candidates.get(i), ignore);
            if (err != null) return group.size() == 1 ? err : label(group.get(i)) + ": " + err;
        }
        for (int i = 0; i < candidates.size(); i++) {
            Footprint a = Footprint.of(candidates.get(i));
            for (int j = i + 1; j < candidates.size(); j++) {
                if (a.overlaps(Footprint.of(candidates.get(j)))) {
                    return label(group.get(i)) + " chocaría con " + label(group.get(j)) + ".";
                }
            }
        }
        return null;
    }

    private static List<Furniture> shifted(List<Furniture> group, double dx, double dz) {
        List<Furniture> result = new ArrayList<>();
        for (Furniture f : group) {
            Furniture c = f.copy();
            c.x = round(f.x + dx);
            c.z = round(f.z + dz);
            result.add(c);
        }
        return result;
    }

    private void applyGroup(List<Furniture> group, List<Furniture> candidates) {
        for (int i = 0; i < group.size(); i++) {
            Furniture f = group.get(i), c = candidates.get(i);
            f.x = c.x;
            f.z = c.z;
            f.rotation = c.rotation;
        }
        fire(Change.FURNITURE);
    }

    /** Mueve todos los muebles del grupo a la vez (o ninguno). Devuelve un error o null. */
    public String moveGroup(List<Furniture> group, double dx, double dz) {
        Furniture locked = firstLocked(group);
        if (locked != null) return label(locked) + LOCKED;
        List<Furniture> candidates = shifted(group, round(dx), round(dz));
        String err = checkGroup(group, candidates);
        if (err == null) applyGroup(group, candidates);
        return err;
    }

    /**
     * Arrastre con el ratón: lleva el grupo para que {@code anchor} (el mueble que se agarró)
     * quede lo más cerca posible de (x, z). Con {@code magnet}, el grupo se pega a paredes y
     * a otros muebles cercanos. Si choca, intenta deslizarlo solo en X o solo en Z.
     *
     * @return las guías del imán (vacía si no se pegó a nada), o null si no se pudo mover
     */
    public List<Snapper.Guide> drag(List<Furniture> group, Furniture anchor, double x, double z, boolean magnet) {
        if (firstLocked(group) != null) return null;
        RoomDims r = project.room;
        Snapper.Box box = boxOf(group);
        double dx = x - anchor.x, dz = z - anchor.z;
        // Que el grupo completo quede dentro de la habitación.
        dx = Units.clamp(dx, -box.minX(), r.width() - box.maxX());
        dz = Units.clamp(dz, -box.minZ(), r.length() - box.maxZ());

        record Try(double dx, double dz) {}
        List<Try> tries = new ArrayList<>();
        List<Snapper.Box> others = new ArrayList<>();
        if (magnet) {
            Set<Furniture> members = identitySet(group);
            for (Furniture f : project.furniture) if (!members.contains(f)) others.add(Snapper.Box.of(Footprint.of(f)));
            for (Column c : project.columns) others.add(Snapper.Box.of(c.footprint()));
            Snapper.Result snap = Snapper.snap(box.shift(dx, dz), r.width(), r.length(), others);
            // Pegado en los dos ejes; si así choca, pegado solo en uno.
            if (snap.guideX() != null && snap.guideZ() != null) tries.add(new Try(dx + snap.dx(), dz + snap.dz()));
            if (snap.guideX() != null) tries.add(new Try(dx + snap.dx(), dz));
            if (snap.guideZ() != null) tries.add(new Try(dx, dz + snap.dz()));
        }
        tries.add(new Try(dx, dz));
        tries.add(new Try(dx, 0));
        tries.add(new Try(0, dz));
        for (Try t : tries) {
            List<Furniture> candidates = shifted(group, round(t.dx), round(t.dz));
            if (checkGroup(group, candidates) != null) continue;
            boolean moved = false;
            for (int i = 0; i < group.size(); i++) {
                moved |= candidates.get(i).x != group.get(i).x || candidates.get(i).z != group.get(i).z;
            }
            if (moved) applyGroup(group, candidates);
            if (!magnet) return List.of();
            // Guías de lo que quedó alineado en la posición final (al centímetro).
            Snapper.Result now = Snapper.snap(boxOf(group), r.width(), r.length(), others);
            List<Snapper.Guide> guides = new ArrayList<>();
            if (now.guideX() != null && Math.abs(now.dx()) < 0.006) guides.add(now.guideX());
            if (now.guideZ() != null && Math.abs(now.dz()) < 0.006) guides.add(now.guideZ());
            return guides;
        }
        return null;
    }

    /**
     * Gira el grupo alrededor de su centro, como si fuera un solo mueble. Si al girar se sale
     * por una pared, se empuja hacia adentro. Devuelve un error o null.
     */
    public String rotateGroup(List<Furniture> group, double degrees) {
        if (group.size() == 1) return rotateFurniture(group.get(0), degrees);
        Furniture locked = firstLocked(group);
        if (locked != null) return label(locked) + LOCKED;
        Snapper.Box box = boxOf(group);
        double cx = box.centerX(), cz = box.centerZ();
        double a = Math.toRadians(degrees), c = Math.cos(a), s = Math.sin(a);
        List<Furniture> candidates = new ArrayList<>();
        for (Furniture f : group) {
            Furniture k = f.copy();
            double dx = f.x - cx, dz = f.z - cz;
            // Mismo sentido de giro que la rotación de cada mueble (ver Footprint).
            k.x = cx + dx * c + dz * s;
            k.z = cz - dx * s + dz * c;
            k.rotation = (((f.rotation + degrees) % 360) + 360) % 360;
            candidates.add(k);
        }
        RoomDims r = project.room;
        Snapper.Box turned = boxOf(candidates);
        double sx = Units.clamp(0, -turned.minX(), r.width() - turned.maxX());
        double sz = Units.clamp(0, -turned.minZ(), r.length() - turned.maxZ());
        for (Furniture k : candidates) {
            k.x = round(k.x + sx);
            k.z = round(k.z + sz);
        }
        String err = checkGroup(group, candidates);
        if (err == null) applyGroup(group, candidates);
        return err;
    }

    /** Duplica el grupo en el lugar libre más cercano, conservando cómo están ubicados entre sí. */
    public Outcome<List<Furniture>> duplicateGroup(List<Furniture> source) {
        return placeGroup(source, false);
    }

    /**
     * Agrega muebles copiados (de este u otro proyecto). Si su lugar original está libre se
     * quedan ahí; si no, se buscan el lugar libre más cercano.
     */
    public Outcome<List<Furniture>> paste(List<Furniture> items) {
        return placeGroup(items, true);
    }

    private Outcome<List<Furniture>> placeGroup(List<Furniture> items, boolean samePlaceAllowed) {
        if (items.isEmpty()) return Outcome.fail("No hay muebles para agregar.");
        List<Furniture> copies = new ArrayList<>();
        for (Furniture item : items) {
            Furniture c = item.copy();
            c.id = ids.next(item.type);
            c.locked = false;
            c.x = round(c.x);
            c.z = round(c.z);
            copies.add(c);
        }
        RoomDims r = project.room;
        // Desplazamientos en una cuadrícula de 10 cm, del más cercano al más lejano.
        List<double[]> offsets = new ArrayList<>();
        int nx = (int) Math.floor(r.width() * 10 + 1e-9), nz = (int) Math.floor(r.length() * 10 + 1e-9);
        for (int i = -nx; i <= nx; i++) {
            for (int j = -nz; j <= nz; j++) {
                if (i == 0 && j == 0 && !samePlaceAllowed) continue;
                offsets.add(new double[] {round(i * 0.1), round(j * 0.1)});
            }
        }
        offsets.sort(Comparator.comparingDouble(o -> Math.hypot(o[0], o[1])));
        for (double[] o : offsets) {
            List<Furniture> candidates = shifted(copies, o[0], o[1]);
            if (checkGroup(candidates, candidates) == null) {
                project.furniture.addAll(candidates);
                fire(Change.FURNITURE);
                return Outcome.ok(candidates);
            }
        }
        // Juntos no caben: se acomoda cada uno por separado (puede girarse).
        List<Furniture> placed = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        for (Furniture c : copies) {
            Outcome<Furniture> out = placeNew(c, catalog.get(c.type).name(), c);
            if (out.failed()) failed.add(catalog.get(c.type).name());
            else placed.add(out.value());
        }
        if (placed.isEmpty()) return Outcome.fail("No hay espacio libre para " + String.join(", ", failed).toLowerCase() + ".");
        if (!failed.isEmpty()) onWarning.accept("No hubo espacio para: " + String.join(", ", failed) + ".");
        return Outcome.ok(placed);
    }

    // ---------------------------------------------------------------- Columnas, muros internos y recortes en L

    /** A esta distancia (m) se considera que una columna toca una pared. */
    private static final double TOUCH = 0.005;
    public static final double MIN_COLUMN = 0.05;
    /** Un recorte en L debe dejar al menos esto de habitación en cada sentido. */
    public static final double MIN_LEFT = 0.5;

    /** Agrega una columna o un muro interno en el lugar libre más cercano al centro. */
    public Outcome<Column> addColumn(Column.Kind kind) {
        if (kind == Column.Kind.NOTCH) throw new IllegalArgumentException("Los recortes se agregan con addNotch.");
        Column c = new Column(ids.next(kind.id()), kind, 0, 0, kind.defaultWidth(), kind.defaultDepth());
        Surface wall = project.walls.get(WallSide.NORTE); // mismo acabado que las paredes
        c.material = wall.material;
        c.color = wall.color;
        RoomDims r = project.room;
        List<double[]> candidates = new ArrayList<>();
        int nx = (int) Math.floor(r.width() * 10 + 1e-9), nz = (int) Math.floor(r.length() * 10 + 1e-9);
        for (int i = 0; i <= nx; i++) {
            for (int j = 0; j <= nz; j++) candidates.add(new double[] {round(i * 0.1), round(j * 0.1)});
        }
        candidates.sort(Comparator.comparingDouble(p -> Math.hypot(p[0] - r.width() / 2, p[1] - r.length() / 2)));
        Column test = c.copy();
        for (double[] p : candidates) {
            test.x = p[0];
            test.z = p[1];
            if (checkColumn(test, null) == null) {
                c.x = p[0];
                c.z = p[1];
                project.columns.add(c);
                fire(Change.ROOM);
                return Outcome.ok(c);
            }
        }
        return Outcome.fail("No hay espacio libre para " + (kind == Column.Kind.WALL ? "un muro interno" : "una columna") + " ("
                + Units.number(c.width) + " x " + Units.number(c.depth) + " m).");
    }

    /**
     * Convierte la habitación en una "L" quitándole una esquina de {@code width} (en X) por
     * {@code depth} (en Z). El recorte se ve como muro y se descuenta del piso.
     */
    public Outcome<Column> addNotch(Column.Corner corner, double width, double depth) {
        for (Column other : project.columns) {
            if (other.kind == Column.Kind.NOTCH && other.corner == corner) {
                return Outcome.fail("La esquina " + corner.id() + " ya tiene un recorte: cambia sus medidas en el panel.");
            }
        }
        Column c = new Column(ids.next("notch"), Column.Kind.NOTCH, 0, 0, round(width), round(depth));
        c.corner = corner;
        Surface wall = project.walls.get(corner.south ? WallSide.SUR : WallSide.NORTE);
        c.material = wall.material;
        c.color = wall.color;
        c.placeInCorner(project.room);
        String err = checkColumn(c, null);
        if (err != null) return Outcome.fail(err);
        project.columns.add(c);
        fire(Change.ROOM);
        return Outcome.ok(c);
    }

    /** Revisa que la columna pueda quedar así. Devuelve un error o null. */
    public String checkColumn(Column c, Column self) {
        RoomDims r = project.room;
        String err = checkRange("El ancho", c.width, MIN_COLUMN, r.width());
        if (err == null) err = checkRange("La profundidad", c.depth, MIN_COLUMN, r.length());
        if (err != null) return err;
        if (c.kind == Column.Kind.NOTCH && (c.width > r.width() - MIN_LEFT + 1e-9 || c.depth > r.length() - MIN_LEFT + 1e-9)) {
            return "El recorte debe dejar al menos " + Units.number(MIN_LEFT) + " m de habitación a cada lado.";
        }
        if (c.minX() < -1e-9 || c.minZ() < -1e-9 || c.maxX() > r.width() + 1e-9 || c.maxZ() > r.length() + 1e-9) {
            return "No cabe dentro de la habitación en esa posición.";
        }
        Footprint fp = c.footprint();
        for (Column other : project.columns) {
            if (other != self && fp.overlaps(other.footprint())) return "Se superpone con " + label(other).toLowerCase() + ".";
        }
        for (Furniture f : project.furniture) {
            if (fp.overlaps(Footprint.of(f))) return "Choca con " + label(f) + ".";
        }
        for (Light l : project.lights) {
            if (l.type != LightType.SUN && inside(c, l.x, l.z)) return "Quedaría encima de " + label(l).toLowerCase() + ".";
        }
        for (Opening o : project.openings) {
            if (covers(c, o)) return "Taparía " + label(o) + ".";
            if (o.type == OpeningType.DOOR && fp.overlaps(doorZone(o))) return "Bloquearía el paso de " + label(o) + ".";
        }
        return null;
    }

    private static boolean inside(Column c, double x, double z) {
        return x > c.minX() - 0.05 && x < c.maxX() + 0.05 && z > c.minZ() - 0.05 && z < c.maxZ() + 0.05;
    }

    /** true si la columna está pegada a la pared de la abertura y tapa (parte de) su ancho. */
    private boolean covers(Column c, Opening o) {
        RoomDims r = project.room;
        boolean touches = switch (o.wall) {
            case NORTE -> c.minZ() <= TOUCH;
            case SUR -> c.maxZ() >= r.length() - TOUCH;
            case OESTE -> c.minX() <= TOUCH;
            case ESTE -> c.maxX() >= r.width() - TOUCH;
        };
        if (!touches) return false;
        double a0 = o.wall.alongX() ? c.minX() : c.minZ(), a1 = o.wall.alongX() ? c.maxX() : c.maxZ();
        return a0 < o.offset + o.width + MARGIN - 1e-9 && a1 > o.offset - MARGIN + 1e-9;
    }

    /** Aplica posición y medidas si el resultado es válido (los recortes se quedan en su esquina). */
    String changeColumn(Column c, Column candidate) {
        candidate.x = round(candidate.x);
        candidate.z = round(candidate.z);
        candidate.width = round(candidate.width);
        candidate.depth = round(candidate.depth);
        if (candidate.kind == Column.Kind.NOTCH) candidate.placeInCorner(project.room);
        String err = checkColumn(candidate, c);
        if (err != null) return err;
        c.x = candidate.x;
        c.z = candidate.z;
        c.width = candidate.width;
        c.depth = candidate.depth;
        c.corner = candidate.corner;
        fire(Change.ROOM);
        return null;
    }

    String changeColumnFinish(Column c, SurfaceMaterial material, String color) {
        if (!material.allowedOnWalls()) return material.label() + " no se puede usar en muros.";
        c.material = material;
        c.color = color;
        fire(Change.ROOM);
        return null;
    }

    /**
     * Tras cambiar las medidas de la habitación: los recortes siguen en su esquina y las columnas
     * se meten dentro. Lo que ya no cabe se elimina; lo que choca con algo se avisa.
     */
    private List<String> fitColumnsToRoom() {
        RoomDims r = project.room;
        List<String> warnings = new ArrayList<>();
        for (Column c : List.copyOf(project.columns)) {
            if (c.kind == Column.Kind.NOTCH) {
                c.width = round(Math.min(c.width, r.width() - MIN_LEFT));
                c.depth = round(Math.min(c.depth, r.length() - MIN_LEFT));
                c.placeInCorner(r);
            } else {
                c.x = round(Units.clamp(c.x, c.width / 2, r.width() - c.width / 2));
                c.z = round(Units.clamp(c.z, c.depth / 2, r.length() - c.depth / 2));
            }
            if (c.width < MIN_COLUMN || c.depth < MIN_COLUMN || c.width > r.width() + 1e-9 || c.depth > r.length() + 1e-9) {
                project.columns.remove(c);
                warnings.add(label(c) + " ya no cabía y se eliminó.");
            }
        }
        return warnings;
    }
}
