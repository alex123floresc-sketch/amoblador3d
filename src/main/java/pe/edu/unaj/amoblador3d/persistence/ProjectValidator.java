package pe.edu.unaj.amoblador3d.persistence;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import pe.edu.unaj.amoblador3d.model.Furniture;
import pe.edu.unaj.amoblador3d.model.Light;
import pe.edu.unaj.amoblador3d.model.Opening;
import pe.edu.unaj.amoblador3d.model.Project;
import pe.edu.unaj.amoblador3d.model.Surface;
import pe.edu.unaj.amoblador3d.model.SurfaceMaterial;
import pe.edu.unaj.amoblador3d.util.Units;

/**
 * Revisa un proyecto leído de un archivo o de la base de datos ANTES de mostrarlo.
 *
 * <p>Corrección: la versión anterior empezaba a cargar el proyecto y fallaba a la mitad si un
 * mueble o una luz tenía datos incompletos, dejando la escena vacía o rota. Ahora se revisa
 * todo primero; lo que no se puede arreglar produce un error claro y la escena actual no se toca.
 * Las aberturas que no caben las descarta después {@code Design.load} con un aviso.
 */
public final class ProjectValidator {
    private static final Pattern COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");

    private ProjectValidator() {}

    public static void validate(Project p) throws ProjectFormatException {
        if (p.room == null || p.room.validate() != null) {
            String detail = p.room == null ? "faltan" : p.room.validate();
            throw new ProjectFormatException("Las medidas de la habitación del proyecto no son válidas: " + detail);
        }
        p.ambient = Double.isFinite(p.ambient) ? Units.clamp(p.ambient, 0, 2) : Project.DEFAULT_AMBIENT;
        fixSurface(p.floor, Surface.DEFAULT_FLOOR);
        for (Surface s : p.walls.values()) fixSurface(s, Surface.DEFAULT_WALL);

        Set<String> ids = new HashSet<>();
        for (Opening o : p.openings) {
            checkId(o.id, ids);
            finite("una abertura", o.offset, o.width, o.height, o.sill);
        }
        for (Light l : p.lights) {
            checkId(l.id, ids);
            finite(l.id, l.intensity, l.x, l.y, l.z, l.targetX, l.targetZ, l.angle, l.penumbra, l.elevation, l.deviation);
            if (l.color == null || !COLOR.matcher(l.color).matches()) l.color = new Light(l.id, l.type).color;
            l.intensity = Units.clamp(l.intensity, 0, l.type.maxIntensity());
        }
        for (Furniture f : p.furniture) {
            checkId(f.id, ids);
            if (f.type == null || f.type.isBlank()) throw new ProjectFormatException("El mueble " + f.id + " no tiene tipo.");
            finite(f.id, f.x, f.z, f.rotation, f.width, f.depth, f.height);
            if (f.width <= 0 || f.depth <= 0 || f.height <= 0) {
                throw new ProjectFormatException("El mueble " + f.id + " tiene medidas no válidas.");
            }
            f.rotation = ((f.rotation % 360) + 360) % 360;
            if (f.color != null && !COLOR.matcher(f.color).matches()) f.color = null;
        }
    }

    /** Un material que no corresponde (p. ej. alfombra en una pared) o un color dañado vuelven al valor por defecto. */
    private static void fixSurface(Surface s, SurfaceMaterial fallback) {
        if (s.material == null || !s.material.allowedOn(s.isFloor())) s.material = fallback;
        if (s.color == null || !COLOR.matcher(s.color).matches()) s.color = s.material.defaultColor();
    }

    private static void checkId(String id, Set<String> seen) throws ProjectFormatException {
        if (id == null || id.isBlank()) throw new ProjectFormatException("Hay un elemento sin id.");
        if (!seen.add(id)) throw new ProjectFormatException("El id \"" + id + "\" está repetido.");
    }

    private static void finite(String what, double... values) throws ProjectFormatException {
        for (double v : values) {
            if (!Double.isFinite(v)) throw new ProjectFormatException("Hay un número no válido en " + what + ".");
        }
    }
}
