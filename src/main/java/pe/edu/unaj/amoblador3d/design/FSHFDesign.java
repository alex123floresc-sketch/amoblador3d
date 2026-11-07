package pe.edu.unaj.amoblador3d.design;

import static pe.edu.unaj.amoblador3d.util.FSHFUnits.checkRange;
import static pe.edu.unaj.amoblador3d.util.FSHFUnits.round;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalog;
import pe.edu.unaj.amoblador3d.catalog.FSHFCatalogEntry;
import pe.edu.unaj.amoblador3d.model.FSHFBaseboard;
import pe.edu.unaj.amoblador3d.model.FSHFCameraView;
import pe.edu.unaj.amoblador3d.model.FSHFColumn;
import pe.edu.unaj.amoblador3d.model.FSHFFootprint;
import pe.edu.unaj.amoblador3d.model.FSHFFurniture;
import pe.edu.unaj.amoblador3d.model.FSHFLight;
import pe.edu.unaj.amoblador3d.model.FSHFLightType;
import pe.edu.unaj.amoblador3d.model.FSHFOpening;
import pe.edu.unaj.amoblador3d.model.FSHFOpeningType;
import pe.edu.unaj.amoblador3d.model.FSHFProject;
import pe.edu.unaj.amoblador3d.model.FSHFRoomDims;
import pe.edu.unaj.amoblador3d.model.FSHFSurface;
import pe.edu.unaj.amoblador3d.model.FSHFSurfaceMaterial;
import pe.edu.unaj.amoblador3d.model.FSHFWallSide;
import pe.edu.unaj.amoblador3d.util.FSHFOutcome;
import pe.edu.unaj.amoblador3d.util.FSHFUnits;

public final class FSHFDesign {
    public static final double MARGIN = 0.05;
    public static final int MAX_ACTIVE_LIGHTS = 3;

    public enum Change { ROOM, SURFACES, LIGHTS, FURNITURE, VIEWS }

    private final FSHFCatalog catalog;
    private final FSHFIdGenerator ids = new FSHFIdGenerator();
    private final List<Consumer<Set<Change>>> listeners = new ArrayList<>();
    private Consumer<String> onWarning = message -> {};
    private FSHFProject project = new FSHFProject();

    public FSHFDesign(FSHFCatalog catalog) {
        this.catalog = catalog;
    }

    public FSHFCatalog catalog() {
        return catalog;
    }

    public FSHFRoomDims room() {
        return project.room;
    }

    public double ambient() {
        return project.ambient;
    }

    public List<FSHFOpening> openings() {
        return List.copyOf(project.openings);
    }

    public List<FSHFLight> lights() {
        return List.copyOf(project.lights);
    }

    public List<FSHFFurniture> furniture() {
        return List.copyOf(project.furniture);
    }

    public List<FSHFCameraView> views() {
        return List.copyOf(project.views);
    }

    public void saveView(FSHFCameraView view) {
        String name = view.name() == null ? "" : view.name().trim();
        if (name.length() > FSHFCameraView.MAX_NAME) name = name.substring(0, FSHFCameraView.MAX_NAME);
        FSHFCameraView v = new FSHFCameraView(name, view.kind(), view.x(), view.y(), view.z(), view.yaw(), view.pitch(), view.distance());
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

    public FSHFSurface ceiling() {
        return project.ceiling;
    }

    public FSHFBaseboard baseboard() {
        return project.baseboard;
    }

    public List<FSHFColumn> columns() {
        return List.copyOf(project.columns);
    }

    public FSHFSurface floor() {
        return project.floor;
    }

    public FSHFSurface wall(FSHFWallSide side) {
        return project.walls.get(side);
    }

    public FSHFProject snapshot() {
        return project.copy();
    }

    public void addListener(Consumer<Set<Change>> listener) {
        listeners.add(listener);
    }

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

    public List<Object> entities() {
        List<Object> all = new ArrayList<>(project.surfaces());
        all.add(project.baseboard);
        all.addAll(project.openings);
        all.addAll(project.lights);
        all.addAll(project.furniture);
        all.addAll(project.columns);
        return all;
    }

    public Object findById(String id) {
        for (Object e : entities()) if (idOf(e).equals(id)) return e;
        return null;
    }

    public static String idOf(Object entity) {
        if (entity instanceof FSHFOpening o) return o.id;
        if (entity instanceof FSHFLight l) return l.id;
        if (entity instanceof FSHFFurniture f) return f.id;
        if (entity instanceof FSHFSurface s) return s.id;
        if (entity instanceof FSHFColumn c) return c.id;
        if (entity instanceof FSHFBaseboard) return FSHFBaseboard.ID;
        throw new IllegalArgumentException("Entidad desconocida: " + entity);
    }

    public String label(Object entity) {
        if (entity instanceof FSHFOpening o) return o.type.label() + " " + suffix(o.id) + " (" + o.wall.id() + ")";
        if (entity instanceof FSHFLight l) return l.type.label() + " " + suffix(l.id);
        if (entity instanceof FSHFFurniture f) return catalog.get(f.type).name() + " " + suffix(f.id);
        if (entity instanceof FSHFSurface s) return s.isFloor() ? "Piso" : s.isCeiling() ? "Techo" : "Pared " + s.wallSide().id();
        if (entity instanceof FSHFColumn c) {
            return c.kind == FSHFColumn.Kind.NOTCH ? c.kind.label() + " (" + c.corner.id() + ")" : c.kind.label() + " " + suffix(c.id);
        }
        if (entity instanceof FSHFBaseboard) return "Zócalo";
        return String.valueOf(entity);
    }

    private static String suffix(String id) {
        return id.substring(id.lastIndexOf('-') + 1);
    }

    public List<String> load(FSHFProject data) {
        project = data.copy();
        ids.reset();
        for (Object e : entities()) ids.register(idOf(e));
        List<String> warnings = fitColumnsToRoom();
        warnings.addAll(fitOpenings());
        warnings.addAll(refreshSunLights());
        fire(Change.ROOM, Change.SURFACES, Change.LIGHTS, Change.FURNITURE, Change.VIEWS);
        return warnings;
    }

    public void loadDefault() {
        FSHFProject p = new FSHFProject();
        load(p);
        addOpening(FSHFOpeningType.WINDOW);
        addOpening(FSHFOpeningType.DOOR);
        addLight(FSHFLightType.POINT);
        addLight(FSHFLightType.SUN);
    }

    public List<String> setRoom(FSHFRoomDims dims) {
        project.room = dims.rounded();
        List<String> warnings = fitColumnsToRoom();
        warnings.addAll(fitOpenings());
        warnings.addAll(fitLightsToRoom());
        warnings.addAll(fitFurnitureToRoom());
        fire(Change.ROOM, Change.LIGHTS, Change.FURNITURE);
        return warnings;
    }

    public void setAmbient(double intensity) {
        project.ambient = FSHFUnits.clamp(intensity, 0, 2);
        fire(Change.LIGHTS);
    }

    public String remove(Object entity) {
        if (entity instanceof FSHFFurniture f && f.locked) return label(f) + LOCKED;
        if (entity instanceof FSHFOpening o) {
            project.openings.remove(o);
            openingsChanged();
        } else if (entity instanceof FSHFLight l) {
            project.lights.remove(l);
            fire(Change.LIGHTS);
        } else if (entity instanceof FSHFFurniture f) {
            project.furniture.remove(f);
            fire(Change.FURNITURE);
        } else if (entity instanceof FSHFColumn c) {
            project.columns.remove(c);
            fire(Change.ROOM);
        }
        return null;
    }

    String changeSurface(FSHFSurface s, FSHFSurfaceMaterial material, String color) {
        if (!material.allowedOn(s)) {
            return material.label() + " no se puede usar " + (s.isFloor() ? "en el piso." : s.isCeiling() ? "en el techo." : "en las paredes.");
        }
        s.material = material;
        s.color = color;
        fire(Change.SURFACES);
        return null;
    }

    String applyToAllWalls(FSHFSurface source) {
        for (FSHFSurface w : project.walls.values()) {
            w.material = source.material;
            w.color = source.color;
        }
        fire(Change.SURFACES);
        return null;
    }

    String changeBaseboard(double height, String color) {
        String err = checkRange("El alto del zócalo", height, 0, FSHFBaseboard.MAX_HEIGHT);
        if (err != null) return err;
        project.baseboard.height = round(height);
        project.baseboard.color = color;
        fire(Change.ROOM);
        return null;
    }

    public double wallLength(FSHFWallSide wall) {
        return project.room.wallLength(wall);
    }

    public FSHFOutcome<FSHFOpening> addOpening(FSHFOpeningType type) {
        return addOpening(type, type.isDoor()
                ? List.of(FSHFWallSide.SUR, FSHFWallSide.OESTE, FSHFWallSide.ESTE, FSHFWallSide.NORTE)
                : List.of(FSHFWallSide.NORTE, FSHFWallSide.ESTE, FSHFWallSide.OESTE, FSHFWallSide.SUR));
    }

    public FSHFOutcome<FSHFOpening> addOpening(FSHFOpeningType type, List<FSHFWallSide> order) {
        FSHFOpening o = FSHFOpening.create(ids.next(type.id()), type);

        o.height = round(Math.min(o.height, project.room.height() - MARGIN));
        for (FSHFWallSide wall : order) {
            o.wall = wall;
            Double offset = findFreeOffset(o, null);
            if (offset != null) {
                o.offset = offset;
                project.openings.add(o);
                openingsChanged();
                return FSHFOutcome.ok(o);
            }
        }
        return FSHFOutcome.fail("No hay espacio libre en " + (order.size() == 1 ? "esa pared" : "las paredes") + " para: "
                + type.label().toLowerCase() + " (" + FSHFUnits.number(o.width) + " m).");
    }

    String validateOpening(FSHFOpening o, FSHFOpening self, boolean checkFurniture) {
        double len = wallLength(o.wall);
        double h = project.room.height();
        boolean door = o.type.isDoor();
        String name = door ? "La puerta" : "La ventana";

        String err = checkRange("El ancho", o.width, 0.3, len - 2 * MARGIN);
        if (err == null) err = checkRange("La posición", o.offset, MARGIN, len - o.width - MARGIN);
        if (err == null && door) err = checkRange("El alto", o.height, 1.5, h - MARGIN);
        if (err == null && !door) err = checkRange("El alféizar", o.sill, MARGIN, h - o.height - MARGIN);
        if (err == null && !door) err = checkRange("El alto", o.height, 0.3, h - o.sill - MARGIN);
        if (err != null) return err;
        if (door && o.sill != 0) return "Las puertas siempre empiezan en el piso.";

        for (FSHFOpening other : project.openings) {
            if (other == self || other.wall != o.wall) continue;
            boolean separated = o.offset >= other.offset + other.width + MARGIN || o.offset + o.width <= other.offset - MARGIN;
            if (!separated) return name + " se superpone con " + label(other) + ".";
        }
        for (FSHFColumn c : project.columns) {
            if (covers(c, o)) return name + " quedaría tapada por " + label(c).toLowerCase() + ".";
            if (door && c.footprint().overlaps(doorZone(o))) return name + " no podría abrirse: choca con " + label(c).toLowerCase() + ".";
        }

        if (door && checkFurniture) {
            FSHFFootprint zone = doorZone(o);
            for (FSHFFurniture f : project.furniture) {
                if (FSHFFootprint.of(f).overlaps(zone)) return name + " no podría abrirse: choca con " + label(f) + ".";
            }
        }
        return null;
    }

    private Double findFreeOffset(FSHFOpening o, FSHFOpening self) {
        double len = wallLength(o.wall);
        List<Double> candidates = new ArrayList<>();
        candidates.add(round((len - o.width) / 2));
        int steps = (int) Math.floor((len - o.width - 2 * MARGIN) / 0.05 + 1e-9);
        for (int i = 0; i <= steps; i++) candidates.add(round(MARGIN + i * 0.05));
        FSHFOpening test = o.copy();
        for (double offset : candidates) {
            test.offset = offset;
            if (validateOpening(test, self, true) == null) return offset;
        }
        return null;
    }

    String changeOpening(FSHFOpening o, FSHFOpening candidate) {
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
        o.type = candidate.type;
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

    private List<String> fitOpenings() {
        List<String> warnings = new ArrayList<>();
        double h = project.room.height();
        List<FSHFOpening> all = new ArrayList<>(project.openings);
        project.openings.clear();
        for (FSHFOpening o : all) {
            double len = wallLength(o.wall);
            FSHFOpening fitted = o.copy();
            fitted.width = round(Math.min(o.width, len - 0.1));
            if (o.type.isDoor()) {
                fitted.height = round(Math.min(o.height, h - 0.05));
                fitted.sill = 0;
            } else {
                fitted.height = round(Math.min(o.height, h - 0.15));
                fitted.sill = round(Math.max(0.05, Math.min(o.sill, h - fitted.height - 0.05)));
            }
            fitted.offset = round(Math.max(0.05, Math.min(o.offset, len - fitted.width - 0.05)));

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

    public FSHFFootprint doorZone(FSHFOpening door) {
        double w = door.width, along = door.offset + w / 2;
        FSHFRoomDims r = project.room;
        return switch (door.wall) {
            case NORTE -> new FSHFFootprint(along, w / 2, w / 2, w / 2, 0);
            case SUR -> new FSHFFootprint(along, r.length() - w / 2, w / 2, w / 2, 0);
            case OESTE -> new FSHFFootprint(w / 2, along, w / 2, w / 2, 0);
            case ESTE -> new FSHFFootprint(r.width() - w / 2, along, w / 2, w / 2, 0);
        };
    }

    public FSHFOpening findOpening(String id) {
        for (FSHFOpening o : project.openings) if (o.id.equals(id)) return o;
        return null;
    }

    public FSHFOutcome<FSHFLight> addLight(FSHFLightType type) {
        FSHFRoomDims r = project.room;
        FSHFLight l = new FSHFLight(null, type);
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
                FSHFOpening window = firstWindow();
                if (window == null) return FSHFOutcome.fail("Agrega primero una ventana: la luz del sol entra por ella.");
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
        return FSHFOutcome.ok(l);
    }

    String changeLight(FSHFLight l, FSHFLight candidate) {
        roundLight(candidate);
        if (candidate.type != FSHFLightType.SUN) {
            for (FSHFColumn c : project.columns) {
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

    public List<FSHFOpening> windows() {
        return project.openings.stream().filter(o -> o.type == FSHFOpeningType.WINDOW).toList();
    }

    private FSHFOpening firstWindow() {
        List<FSHFOpening> windows = windows();
        return windows.isEmpty() ? null : windows.get(0);
    }

    private List<String> refreshSunLights() {
        List<String> warnings = new ArrayList<>();
        for (FSHFLight l : List.copyOf(project.lights)) {
            if (l.type != FSHFLightType.SUN) continue;
            FSHFOpening window = findOpening(l.windowId == null ? "" : l.windowId);
            if (window != null && window.type == FSHFOpeningType.WINDOW) continue;
            FSHFOpening first = firstWindow();
            if (first == null) {
                warnings.add(label(l) + " se eliminó porque ya no hay ventanas.");
                project.lights.remove(l);
            } else {
                l.windowId = first.id;
            }
        }
        return warnings;
    }

    private List<String> fitLightsToRoom() {
        FSHFRoomDims r = project.room;
        for (FSHFLight l : project.lights) {
            if (l.type == FSHFLightType.SUN) continue;
            l.x = clampInside(l.x, r.width());
            l.y = clampInside(l.y, r.height());
            l.z = clampInside(l.z, r.length());
            if (l.type == FSHFLightType.SPOT) {
                l.targetX = clampInside(l.targetX, r.width());
                l.targetZ = clampInside(l.targetZ, r.length());
            }
            roundLight(l);
        }
        return refreshSunLights();
    }

    private static double clampInside(double v, double max) {
        return FSHFUnits.clamp(v, 0.05, max - 0.05);
    }

    private static void roundLight(FSHFLight l) {
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

    public FSHFOutcome<FSHFFurniture> addFurniture(String type) {
        FSHFCatalogEntry e = catalog.get(type);
        FSHFCatalogEntry.Size s = e.defaults();
        FSHFFurniture f = new FSHFFurniture(ids.next(type), type, 0, 0, 0, s.width(), s.depth(), s.height(), e.defaultColor());
        fitHeight(f, e);
        return placeNew(f, e.name(), null);
    }

    public FSHFOutcome<FSHFFurniture> addFurnitureAt(String type, double x, double z) {
        FSHFCatalogEntry e = catalog.get(type);
        if (e.kind() == FSHFCatalogEntry.Kind.MISSING) return FSHFOutcome.fail("No existe \"" + type + "\" en el catálogo.");
        FSHFCatalogEntry.Size s = e.defaults();
        FSHFRoomDims r = project.room;
        FSHFFurniture f = new FSHFFurniture(ids.next(type), type, round(FSHFUnits.clamp(x, 0, r.width())), round(FSHFUnits.clamp(z, 0, r.length())),
                0, s.width(), s.depth(), s.height(), e.defaultColor());
        fitHeight(f, e);
        return placeNew(f, e.name(), f);
    }

    private void fitHeight(FSHFFurniture f, FSHFCatalogEntry e) {
        double h = project.room.height();
        if (FSHFCatalog.isStair(f.type) || f.height > h) {
            f.height = round(Math.max(e.heightRange().min(), Math.min(e.heightRange().max(), h)));
        }
    }

    public FSHFOutcome<FSHFFurniture> duplicate(FSHFFurniture source) {
        FSHFFurniture f = source.copy();
        f.id = ids.next(source.type);
        f.locked = false;
        return placeNew(f, catalog.get(source.type).name(), source);
    }

    private FSHFOutcome<FSHFFurniture> placeNew(FSHFFurniture f, String name, FSHFFurniture near) {
        double[] spot = findFreeSpot(f, near);
        if (spot == null) {
            FSHFFurniture turned = f.copy();
            turned.rotation = (f.rotation + 90) % 360;
            spot = findFreeSpot(turned, near);
            if (spot != null) f.rotation = turned.rotation;
        }
        if (spot == null) {
            return FSHFOutcome.fail("No hay espacio libre para " + name.toLowerCase() + " ("
                    + FSHFUnits.number(f.width) + " x " + FSHFUnits.number(f.depth) + " m).");
        }
        f.x = spot[0];
        f.z = spot[1];
        project.furniture.add(f);
        fire(Change.FURNITURE);
        return FSHFOutcome.ok(f);
    }

    public String checkFurniture(FSHFFurniture self, FSHFFurniture candidate) {
        return check(candidate, self == null ? Set.of() : Collections.singleton(self));
    }

    private String check(FSHFFurniture candidate, Set<FSHFFurniture> ignore) {
        FSHFRoomDims r = project.room;
        if (candidate.height > r.height() + 1e-9) {
            return "Es más alto (" + FSHFUnits.number(candidate.height) + " m) que la habitación ("
                    + FSHFUnits.number(r.height()) + " m).";
        }
        FSHFFootprint fp = FSHFFootprint.of(candidate);
        if (!fp.insideRoom(r.width(), r.length())) return "No cabe dentro de la habitación en esa posición.";
        for (FSHFFurniture other : project.furniture) {
            if (!ignore.contains(other) && fp.overlaps(FSHFFootprint.of(other))) return "Choca con " + label(other) + ".";
        }
        for (FSHFColumn c : project.columns) {
            if (fp.overlaps(c.footprint())) return "Choca con " + label(c).toLowerCase() + ".";
        }
        for (FSHFOpening o : project.openings) {
            if (o.type.isDoor() && fp.overlaps(doorZone(o))) return "Bloquea el paso de " + label(o) + ".";
        }
        return null;
    }

    public boolean moveFurniture(FSHFFurniture f, double x, double z) {
        if (f.locked) return false;
        FSHFRoomDims r = project.room;
        FSHFFootprint target = FSHFFootprint.of(f).withCenter(round(x), round(z)).clampToRoom(r.width(), r.length());
        double[][] tries = {{target.x(), target.z()}, {target.x(), f.z}, {f.x, target.z()}};
        FSHFFurniture candidate = f.copy();
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

    String changeFurniture(FSHFFurniture f, FSHFFurniture candidate, boolean positionEdited) {
        if (f.locked) return label(f) + LOCKED;
        candidate.rotation = ((candidate.rotation % 360) + 360) % 360;
        candidate.x = round(candidate.x);
        candidate.z = round(candidate.z);
        candidate.width = round(candidate.width);
        candidate.depth = round(candidate.depth);
        candidate.height = round(candidate.height);

        boolean sameGeometry = candidate.x == f.x && candidate.z == f.z && candidate.rotation == f.rotation
                && candidate.width == f.width && candidate.depth == f.depth && candidate.height == f.height;
        if (!sameGeometry) {
            if (!positionEdited) {
                FSHFRoomDims r = project.room;
                FSHFFootprint fp = FSHFFootprint.of(candidate).clampToRoom(r.width(), r.length());
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

    public String replaceType(FSHFFurniture f, String type) {
        if (f.locked) return label(f) + LOCKED;
        FSHFCatalogEntry e = catalog.get(type);
        if (e.kind() == FSHFCatalogEntry.Kind.MISSING) return "No existe \"" + type + "\" en el catálogo.";
        FSHFFurniture c = f.copy();
        c.type = type;
        c.width = e.defaults().width();
        c.depth = e.defaults().depth();
        c.height = e.defaults().height();
        c.color = e.defaultColor();
        c.color2 = null;
        c.finish = null;
        FSHFRoomDims r = project.room;
        FSHFFootprint fp = FSHFFootprint.of(c).clampToRoom(r.width(), r.length());
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

        if (f.id.startsWith(oldPrefix + "-")) f.id = ids.next(type);
        fire(Change.FURNITURE);
        return null;
    }

    public String reshape(FSHFFurniture f, FSHFFurniture candidate, boolean keepPosition) {
        return changeFurniture(f, candidate, keepPosition);
    }

    public String rotateFurniture(FSHFFurniture f, double degrees) {
        FSHFFurniture candidate = f.copy();
        candidate.rotation = f.rotation + degrees;
        return changeFurniture(f, candidate, false);
    }

    private List<String> fitFurnitureToRoom() {
        FSHFRoomDims r = project.room;
        List<String> warnings = new ArrayList<>();
        for (FSHFFurniture f : project.furniture) {
            FSHFFootprint fp = FSHFFootprint.of(f).clampToRoom(r.width(), r.length());
            f.x = round(fp.x());
            f.z = round(fp.z());
            String error = checkFurniture(f, f);
            if (error != null) warnings.add(label(f) + ": " + error);
        }
        return warnings;
    }

    private double[] findFreeSpot(FSHFFurniture f, FSHFFurniture near) {
        FSHFRoomDims r = project.room;
        double ox = near != null ? near.x : r.width() / 2;
        double oz = near != null ? near.z : r.length() / 2;
        List<double[]> candidates = new ArrayList<>();
        int nx = (int) Math.floor(r.width() * 10 + 1e-9), nz = (int) Math.floor(r.length() * 10 + 1e-9);
        for (int i = 0; i <= nx; i++) {
            for (int j = 0; j <= nz; j++) candidates.add(new double[] {round(i * 0.1), round(j * 0.1)});
        }
        candidates.sort(Comparator.comparingDouble(p -> Math.hypot(p[0] - ox, p[1] - oz)));
        FSHFFurniture test = f.copy();
        for (double[] p : candidates) {
            test.x = p[0];
            test.z = p[1];
            if (checkFurniture(null, test) == null) return p;
        }
        return null;
    }

    private static final String LOCKED = " está bloqueado: desbloquéalo para cambiarlo.";

    public void setLocked(Collection<FSHFFurniture> group, boolean locked) {
        for (FSHFFurniture f : group) f.locked = locked;
        fire(Change.FURNITURE);
    }

    public FSHFFurniture firstLocked(Collection<FSHFFurniture> group) {
        for (FSHFFurniture f : group) if (f.locked) return f;
        return null;
    }

    private static Set<FSHFFurniture> identitySet(Collection<FSHFFurniture> items) {
        Set<FSHFFurniture> set = Collections.newSetFromMap(new IdentityHashMap<>());
        set.addAll(items);
        return set;
    }

    private static FSHFSnapper.Box boxOf(Collection<FSHFFurniture> group) {
        FSHFSnapper.Box box = null;
        for (FSHFFurniture f : group) {
            FSHFSnapper.Box b = FSHFSnapper.Box.of(FSHFFootprint.of(f));
            box = box == null ? b : box.union(b);
        }
        return box;
    }

    private String checkGroup(List<FSHFFurniture> group, List<FSHFFurniture> candidates) {
        Set<FSHFFurniture> ignore = identitySet(group);
        for (int i = 0; i < candidates.size(); i++) {
            String err = check(candidates.get(i), ignore);
            if (err != null) return group.size() == 1 ? err : label(group.get(i)) + ": " + err;
        }
        for (int i = 0; i < candidates.size(); i++) {
            FSHFFootprint a = FSHFFootprint.of(candidates.get(i));
            for (int j = i + 1; j < candidates.size(); j++) {
                if (a.overlaps(FSHFFootprint.of(candidates.get(j)))) {
                    return label(group.get(i)) + " chocaría con " + label(group.get(j)) + ".";
                }
            }
        }
        return null;
    }

    private static List<FSHFFurniture> shifted(List<FSHFFurniture> group, double dx, double dz) {
        List<FSHFFurniture> result = new ArrayList<>();
        for (FSHFFurniture f : group) {
            FSHFFurniture c = f.copy();
            c.x = round(f.x + dx);
            c.z = round(f.z + dz);
            result.add(c);
        }
        return result;
    }

    private void applyGroup(List<FSHFFurniture> group, List<FSHFFurniture> candidates) {
        for (int i = 0; i < group.size(); i++) {
            FSHFFurniture f = group.get(i), c = candidates.get(i);
            f.x = c.x;
            f.z = c.z;
            f.rotation = c.rotation;
        }
        fire(Change.FURNITURE);
    }

    public String moveGroup(List<FSHFFurniture> group, double dx, double dz) {
        FSHFFurniture locked = firstLocked(group);
        if (locked != null) return label(locked) + LOCKED;
        List<FSHFFurniture> candidates = shifted(group, round(dx), round(dz));
        String err = checkGroup(group, candidates);
        if (err == null) applyGroup(group, candidates);
        return err;
    }

    public List<FSHFSnapper.Guide> drag(List<FSHFFurniture> group, FSHFFurniture anchor, double x, double z, boolean magnet) {
        if (firstLocked(group) != null) return null;
        FSHFRoomDims r = project.room;
        FSHFSnapper.Box box = boxOf(group);
        double dx = x - anchor.x, dz = z - anchor.z;

        dx = FSHFUnits.clamp(dx, -box.minX(), r.width() - box.maxX());
        dz = FSHFUnits.clamp(dz, -box.minZ(), r.length() - box.maxZ());

        record Try(double dx, double dz) {}
        List<Try> tries = new ArrayList<>();
        List<FSHFSnapper.Box> others = new ArrayList<>();
        if (magnet) {
            Set<FSHFFurniture> members = identitySet(group);
            for (FSHFFurniture f : project.furniture) if (!members.contains(f)) others.add(FSHFSnapper.Box.of(FSHFFootprint.of(f)));
            for (FSHFColumn c : project.columns) others.add(FSHFSnapper.Box.of(c.footprint()));
            FSHFSnapper.Result snap = FSHFSnapper.snap(box.shift(dx, dz), r.width(), r.length(), others);

            if (snap.guideX() != null && snap.guideZ() != null) tries.add(new Try(dx + snap.dx(), dz + snap.dz()));
            if (snap.guideX() != null) tries.add(new Try(dx + snap.dx(), dz));
            if (snap.guideZ() != null) tries.add(new Try(dx, dz + snap.dz()));
        }
        tries.add(new Try(dx, dz));
        tries.add(new Try(dx, 0));
        tries.add(new Try(0, dz));
        for (Try t : tries) {
            List<FSHFFurniture> candidates = shifted(group, round(t.dx), round(t.dz));
            if (checkGroup(group, candidates) != null) continue;
            boolean moved = false;
            for (int i = 0; i < group.size(); i++) {
                moved |= candidates.get(i).x != group.get(i).x || candidates.get(i).z != group.get(i).z;
            }
            if (moved) applyGroup(group, candidates);
            if (!magnet) return List.of();

            FSHFSnapper.Result now = FSHFSnapper.snap(boxOf(group), r.width(), r.length(), others);
            List<FSHFSnapper.Guide> guides = new ArrayList<>();
            if (now.guideX() != null && Math.abs(now.dx()) < 0.006) guides.add(now.guideX());
            if (now.guideZ() != null && Math.abs(now.dz()) < 0.006) guides.add(now.guideZ());
            return guides;
        }
        return null;
    }

    public String rotateGroup(List<FSHFFurniture> group, double degrees) {
        if (group.size() == 1) return rotateFurniture(group.get(0), degrees);
        FSHFFurniture locked = firstLocked(group);
        if (locked != null) return label(locked) + LOCKED;
        FSHFSnapper.Box box = boxOf(group);
        double cx = box.centerX(), cz = box.centerZ();
        double a = Math.toRadians(degrees), c = Math.cos(a), s = Math.sin(a);
        List<FSHFFurniture> candidates = new ArrayList<>();
        for (FSHFFurniture f : group) {
            FSHFFurniture k = f.copy();
            double dx = f.x - cx, dz = f.z - cz;

            k.x = cx + dx * c + dz * s;
            k.z = cz - dx * s + dz * c;
            k.rotation = (((f.rotation + degrees) % 360) + 360) % 360;
            candidates.add(k);
        }
        FSHFRoomDims r = project.room;
        FSHFSnapper.Box turned = boxOf(candidates);
        double sx = FSHFUnits.clamp(0, -turned.minX(), r.width() - turned.maxX());
        double sz = FSHFUnits.clamp(0, -turned.minZ(), r.length() - turned.maxZ());
        for (FSHFFurniture k : candidates) {
            k.x = round(k.x + sx);
            k.z = round(k.z + sz);
        }
        String err = checkGroup(group, candidates);
        if (err == null) applyGroup(group, candidates);
        return err;
    }

    public FSHFOutcome<List<FSHFFurniture>> duplicateGroup(List<FSHFFurniture> source) {
        return placeGroup(source, false);
    }

    public FSHFOutcome<List<FSHFFurniture>> paste(List<FSHFFurniture> items) {
        return placeGroup(items, true);
    }

    private FSHFOutcome<List<FSHFFurniture>> placeGroup(List<FSHFFurniture> items, boolean samePlaceAllowed) {
        if (items.isEmpty()) return FSHFOutcome.fail("No hay muebles para agregar.");
        List<FSHFFurniture> copies = new ArrayList<>();
        for (FSHFFurniture item : items) {
            FSHFFurniture c = item.copy();
            c.id = ids.next(item.type);
            c.locked = false;
            c.x = round(c.x);
            c.z = round(c.z);
            copies.add(c);
        }
        FSHFRoomDims r = project.room;

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
            List<FSHFFurniture> candidates = shifted(copies, o[0], o[1]);
            if (checkGroup(candidates, candidates) == null) {
                project.furniture.addAll(candidates);
                fire(Change.FURNITURE);
                return FSHFOutcome.ok(candidates);
            }
        }

        List<FSHFFurniture> placed = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        for (FSHFFurniture c : copies) {
            FSHFOutcome<FSHFFurniture> out = placeNew(c, catalog.get(c.type).name(), c);
            if (out.failed()) failed.add(catalog.get(c.type).name());
            else placed.add(out.value());
        }
        if (placed.isEmpty()) return FSHFOutcome.fail("No hay espacio libre para " + String.join(", ", failed).toLowerCase() + ".");
        if (!failed.isEmpty()) onWarning.accept("No hubo espacio para: " + String.join(", ", failed) + ".");
        return FSHFOutcome.ok(placed);
    }

    private static final double TOUCH = 0.005;
    public static final double MIN_COLUMN = 0.05;
    public static final double MIN_LEFT = 0.5;

    public FSHFOutcome<FSHFColumn> addColumn(FSHFColumn.Kind kind) {
        if (kind == FSHFColumn.Kind.NOTCH) throw new IllegalArgumentException("Los recortes se agregan con addNotch.");
        FSHFColumn c = new FSHFColumn(ids.next(kind.id()), kind, 0, 0, kind.defaultWidth(), kind.defaultDepth());
        FSHFSurface wall = project.walls.get(FSHFWallSide.NORTE);
        c.material = wall.material;
        c.color = wall.color;
        FSHFRoomDims r = project.room;
        List<double[]> candidates = new ArrayList<>();
        int nx = (int) Math.floor(r.width() * 10 + 1e-9), nz = (int) Math.floor(r.length() * 10 + 1e-9);
        for (int i = 0; i <= nx; i++) {
            for (int j = 0; j <= nz; j++) candidates.add(new double[] {round(i * 0.1), round(j * 0.1)});
        }
        candidates.sort(Comparator.comparingDouble(p -> Math.hypot(p[0] - r.width() / 2, p[1] - r.length() / 2)));
        FSHFColumn test = c.copy();
        for (double[] p : candidates) {
            test.x = p[0];
            test.z = p[1];
            if (checkColumn(test, null) == null) {
                c.x = p[0];
                c.z = p[1];
                project.columns.add(c);
                fire(Change.ROOM);
                return FSHFOutcome.ok(c);
            }
        }
        return FSHFOutcome.fail("No hay espacio libre para " + (kind == FSHFColumn.Kind.WALL ? "un muro interno" : "una columna") + " ("
                + FSHFUnits.number(c.width) + " x " + FSHFUnits.number(c.depth) + " m).");
    }

    public FSHFOutcome<FSHFColumn> addNotch(FSHFColumn.Corner corner, double width, double depth) {
        for (FSHFColumn other : project.columns) {
            if (other.kind == FSHFColumn.Kind.NOTCH && other.corner == corner) {
                return FSHFOutcome.fail("La esquina " + corner.id() + " ya tiene un recorte: cambia sus medidas en el panel.");
            }
        }
        FSHFColumn c = new FSHFColumn(ids.next("notch"), FSHFColumn.Kind.NOTCH, 0, 0, round(width), round(depth));
        c.corner = corner;
        FSHFSurface wall = project.walls.get(corner.south ? FSHFWallSide.SUR : FSHFWallSide.NORTE);
        c.material = wall.material;
        c.color = wall.color;
        c.placeInCorner(project.room);
        String err = checkColumn(c, null);
        if (err != null) return FSHFOutcome.fail(err);
        project.columns.add(c);
        fire(Change.ROOM);
        return FSHFOutcome.ok(c);
    }

    public String checkColumn(FSHFColumn c, FSHFColumn self) {
        FSHFRoomDims r = project.room;
        String err = checkRange("El ancho", c.width, MIN_COLUMN, r.width());
        if (err == null) err = checkRange("La profundidad", c.depth, MIN_COLUMN, r.length());
        if (err != null) return err;
        if (c.kind == FSHFColumn.Kind.NOTCH && (c.width > r.width() - MIN_LEFT + 1e-9 || c.depth > r.length() - MIN_LEFT + 1e-9)) {
            return "El recorte debe dejar al menos " + FSHFUnits.number(MIN_LEFT) + " m de habitación a cada lado.";
        }
        if (c.minX() < -1e-9 || c.minZ() < -1e-9 || c.maxX() > r.width() + 1e-9 || c.maxZ() > r.length() + 1e-9) {
            return "No cabe dentro de la habitación en esa posición.";
        }
        FSHFFootprint fp = c.footprint();
        for (FSHFColumn other : project.columns) {
            if (other != self && fp.overlaps(other.footprint())) return "Se superpone con " + label(other).toLowerCase() + ".";
        }
        for (FSHFFurniture f : project.furniture) {
            if (fp.overlaps(FSHFFootprint.of(f))) return "Choca con " + label(f) + ".";
        }
        for (FSHFLight l : project.lights) {
            if (l.type != FSHFLightType.SUN && inside(c, l.x, l.z)) return "Quedaría encima de " + label(l).toLowerCase() + ".";
        }
        for (FSHFOpening o : project.openings) {
            if (covers(c, o)) return "Taparía " + label(o) + ".";
            if (o.type.isDoor() && fp.overlaps(doorZone(o))) return "Bloquearía el paso de " + label(o) + ".";
        }
        return null;
    }

    private static boolean inside(FSHFColumn c, double x, double z) {
        return x > c.minX() - 0.05 && x < c.maxX() + 0.05 && z > c.minZ() - 0.05 && z < c.maxZ() + 0.05;
    }

    private boolean covers(FSHFColumn c, FSHFOpening o) {
        FSHFRoomDims r = project.room;
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

    String changeColumn(FSHFColumn c, FSHFColumn candidate) {
        candidate.x = round(candidate.x);
        candidate.z = round(candidate.z);
        candidate.width = round(candidate.width);
        candidate.depth = round(candidate.depth);
        if (candidate.kind == FSHFColumn.Kind.NOTCH) candidate.placeInCorner(project.room);
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

    String changeColumnFinish(FSHFColumn c, FSHFSurfaceMaterial material, String color) {
        if (!material.allowedOnWalls()) return material.label() + " no se puede usar en muros.";
        c.material = material;
        c.color = color;
        fire(Change.ROOM);
        return null;
    }

    private List<String> fitColumnsToRoom() {
        FSHFRoomDims r = project.room;
        List<String> warnings = new ArrayList<>();
        for (FSHFColumn c : List.copyOf(project.columns)) {
            if (c.kind == FSHFColumn.Kind.NOTCH) {
                c.width = round(Math.min(c.width, r.width() - MIN_LEFT));
                c.depth = round(Math.min(c.depth, r.length() - MIN_LEFT));
                c.placeInCorner(r);
            } else {
                c.x = round(FSHFUnits.clamp(c.x, c.width / 2, r.width() - c.width / 2));
                c.z = round(FSHFUnits.clamp(c.z, c.depth / 2, r.length() - c.depth / 2));
            }
            if (c.width < MIN_COLUMN || c.depth < MIN_COLUMN || c.width > r.width() + 1e-9 || c.depth > r.length() + 1e-9) {
                project.columns.remove(c);
                warnings.add(label(c) + " ya no cabía y se eliminó.");
            }
        }
        return warnings;
    }
}
