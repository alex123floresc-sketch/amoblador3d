package pe.edu.unaj.amoblador3d.persistence;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import pe.edu.unaj.amoblador3d.model.FSHFBaseboard;
import pe.edu.unaj.amoblador3d.model.FSHFColumn;
import pe.edu.unaj.amoblador3d.model.FSHFFurniture;
import pe.edu.unaj.amoblador3d.model.FSHFLight;
import pe.edu.unaj.amoblador3d.model.FSHFOpening;
import pe.edu.unaj.amoblador3d.model.FSHFProject;
import pe.edu.unaj.amoblador3d.model.FSHFSurface;
import pe.edu.unaj.amoblador3d.model.FSHFSurfaceMaterial;
import pe.edu.unaj.amoblador3d.util.FSHFUnits;

public final class FSHFProjectValidator {
    private static final Pattern COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");

    private FSHFProjectValidator() {}

    public static void validate(FSHFProject p) throws FSHFProjectFormatException {
        if (p.room == null || p.room.validate() != null) {
            String detail = p.room == null ? "faltan" : p.room.validate();
            throw new FSHFProjectFormatException("Las medidas de la habitación del proyecto no son válidas: " + detail);
        }
        p.ambient = Double.isFinite(p.ambient) ? FSHFUnits.clamp(p.ambient, 0, 2) : FSHFProject.DEFAULT_AMBIENT;
        fixSurface(p.floor, FSHFSurface.DEFAULT_FLOOR);
        for (FSHFSurface s : p.walls.values()) fixSurface(s, FSHFSurface.DEFAULT_WALL);
        fixSurface(p.ceiling, FSHFSurface.DEFAULT_CEILING);
        if (p.baseboard == null) p.baseboard = new FSHFBaseboard();
        if (!Double.isFinite(p.baseboard.height)) p.baseboard.height = FSHFBaseboard.DEFAULT_HEIGHT;
        p.baseboard.height = FSHFUnits.clamp(p.baseboard.height, 0, FSHFBaseboard.MAX_HEIGHT);
        if (p.baseboard.color == null || !COLOR.matcher(p.baseboard.color).matches()) p.baseboard.color = FSHFBaseboard.DEFAULT_COLOR;

        Set<String> ids = new HashSet<>();
        for (FSHFOpening o : p.openings) {
            checkId(o.id, ids);
            finite("una abertura", o.offset, o.width, o.height, o.sill);
        }
        for (FSHFLight l : p.lights) {
            checkId(l.id, ids);
            finite(l.id, l.intensity, l.x, l.y, l.z, l.targetX, l.targetZ, l.angle, l.penumbra, l.elevation, l.deviation);
            if (l.color == null || !COLOR.matcher(l.color).matches()) l.color = new FSHFLight(l.id, l.type).color;
            l.intensity = FSHFUnits.clamp(l.intensity, 0, l.type.maxIntensity());
        }
        for (FSHFColumn c : p.columns) {
            checkId(c.id, ids);
            if (c.kind == null) throw new FSHFProjectFormatException("La columna " + c.id + " no tiene tipo.");
            finite(c.id, c.x, c.z, c.width, c.depth);
            if (c.width <= 0 || c.depth <= 0) throw new FSHFProjectFormatException("La columna " + c.id + " tiene medidas no válidas.");
            if (c.kind == FSHFColumn.Kind.NOTCH && c.corner == null) {
                throw new FSHFProjectFormatException("El recorte " + c.id + " no dice en qué esquina está.");
            }
            if (c.material == null || !c.material.allowedOnWalls()) c.material = FSHFSurface.DEFAULT_WALL;
            if (c.color == null || !COLOR.matcher(c.color).matches()) c.color = c.material.defaultColor();
        }

        Set<String> names = new HashSet<>();
        p.views.removeIf(v -> !v.isValid() || !names.add(v.name().trim().toLowerCase()));
        for (FSHFFurniture f : p.furniture) {
            checkId(f.id, ids);
            if (f.type == null || f.type.isBlank()) throw new FSHFProjectFormatException("El mueble " + f.id + " no tiene tipo.");
            finite(f.id, f.x, f.z, f.rotation, f.width, f.depth, f.height);
            if (f.width <= 0 || f.depth <= 0 || f.height <= 0) {
                throw new FSHFProjectFormatException("El mueble " + f.id + " tiene medidas no válidas.");
            }
            f.rotation = ((f.rotation % 360) + 360) % 360;
            if (f.color != null && !COLOR.matcher(f.color).matches()) f.color = null;
            if (f.color2 != null && !COLOR.matcher(f.color2).matches()) f.color2 = null;
        }
    }

    private static void fixSurface(FSHFSurface s, FSHFSurfaceMaterial fallback) {
        if (s.material == null || !s.material.allowedOn(s)) s.material = fallback;
        if (s.color == null || !COLOR.matcher(s.color).matches()) s.color = s.material.defaultColor();
    }

    private static void checkId(String id, Set<String> seen) throws FSHFProjectFormatException {
        if (id == null || id.isBlank()) throw new FSHFProjectFormatException("Hay un elemento sin id.");
        if (!seen.add(id)) throw new FSHFProjectFormatException("El id \"" + id + "\" está repetido.");
    }

    private static void finite(String what, double... values) throws FSHFProjectFormatException {
        for (double v : values) {
            if (!Double.isFinite(v)) throw new FSHFProjectFormatException("Hay un número no válido en " + what + ".");
        }
    }
}
