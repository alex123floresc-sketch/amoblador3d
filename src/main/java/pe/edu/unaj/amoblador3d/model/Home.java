package pe.edu.unaj.amoblador3d.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Una casa (o departamento): varios ambientes, cada uno con su habitación, sus aberturas, luces
 * y muebles ({@link Project}), ubicados en el plano de la casa.
 *
 * <p>Coordenadas del plano (en metros): cada ambiente se ubica por la esquina interior noroeste
 * de su habitación ({@link Space#x}, {@link Space#z}); X crece hacia el este y Z hacia el sur,
 * igual que dentro de cada ambiente.
 */
public final class Home {
    public static final int MAX_SPACES = 30, MAX_NAME = 40;

    /** Un ambiente: sala, dormitorio, cocina... */
    public static final class Space {
        public String id;
        public String name;
        public double x, z;
        public Project project;

        public Space(String id, String name, double x, double z, Project project) {
            this.id = id;
            this.name = name;
            this.x = x;
            this.z = z;
            this.project = project;
        }

        public Space copy() {
            return new Space(id, name, x, z, project.copy());
        }

        /** Planta exterior (incluye el grosor de los muros): {minX, minZ, maxX, maxZ} en el plano de la casa. */
        public double[] outer() {
            double t = project.room.wallThickness();
            return new double[] {x - t, z - t, x + project.room.width() + t, z + project.room.length() + t};
        }
    }

    public final List<Space> spaces = new ArrayList<>();

    /** Una casa con un solo ambiente (así se abren los proyectos de una sola habitación). */
    public static Home of(Project p, String name) {
        Home h = new Home();
        h.spaces.add(new Space("ambiente-1", name, 0, 0, p));
        return h;
    }

    public Home copy() {
        Home h = new Home();
        for (Space s : spaces) h.spaces.add(s.copy());
        return h;
    }

    /** Id que todavía no usa ningún ambiente: "ambiente-3". */
    public String nextId() {
        int n = 1;
        while (true) {
            String id = "ambiente-" + n;
            if (spaces.stream().noneMatch(s -> s.id.equals(id))) return id;
            n++;
        }
    }

    /** Nombre que no se repite: "Dormitorio", "Dormitorio 2"... */
    public String uniqueName(String base) {
        String trimmed = base == null || base.isBlank() ? "Ambiente" : base.trim();
        String b = trimmed.length() > MAX_NAME - 3 ? trimmed.substring(0, MAX_NAME - 3) : trimmed;
        if (spaces.stream().noneMatch(s -> s.name.equalsIgnoreCase(b))) return b;
        for (int i = 2; ; i++) {
            String name = b + " " + i;
            if (spaces.stream().noneMatch(s -> s.name.equalsIgnoreCase(name))) return name;
        }
    }

    /** true si los dos ambientes se encimarían (tocarse muro con muro está bien). */
    public static boolean overlaps(double[] a, double[] b) {
        double eps = 1e-6;
        return a[0] < b[2] - eps && b[0] < a[2] - eps && a[1] < b[3] - eps && b[1] < a[3] - eps;
    }

    /** El primer ambiente con el que se enciman {@code s} si estuviera en (x, z), o null. */
    public Space collision(Space s, double x, double z) {
        double t = s.project.room.wallThickness();
        double[] box = {x - t, z - t, x + s.project.room.width() + t, z + s.project.room.length() + t};
        for (Space o : spaces) if (o != s && overlaps(box, o.outer())) return o;
        return null;
    }

    /**
     * Lugar libre para un ambiente nuevo: a la derecha de la casa, pegado muro con muro
     * (alineado arriba con el ambiente que queda más al este).
     */
    public double[] freeSpot(Project p) {
        if (spaces.isEmpty()) return new double[] {0, 0};
        double maxX = -Double.MAX_VALUE, z = 0;
        for (Space s : spaces) {
            double[] o = s.outer();
            if (o[2] > maxX) {
                maxX = o[2];
                z = s.z;
            }
        }
        double t = p.room.wallThickness();
        return new double[] {Math.round((maxX + t) * 100) / 100.0, z};
    }
}
