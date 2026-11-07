package pe.edu.unaj.amoblador3d.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Una casa (o departamento): varios ambientes, cada uno con su habitación, sus aberturas, luces
 * y muebles ({@link FSHFProject}), ubicados en el plano de la casa, en uno o varios pisos.
 *
 * <p>Coordenadas del plano (en metros): X crece hacia el este y Z hacia el sur, igual que dentro
 * de cada ambiente. Cada ambiente se ubica por la esquina interior noroeste de su planta ya girada
 * ({@link Space#x}, {@link Space#z}); su giro ({@link Space#rotation}) es horario visto desde arriba:
 * con 90° su pared norte queda mirando al este.
 */
public final class FSHFHome {
    public static final int MAX_SPACES = 30, MAX_NAME = 40, MAX_LEVELS = 4;
    /** Grosor de la losa entre un piso y el de arriba (m). */
    public static final double SLAB = 0.2;
    /** Distancia máxima (m) entre las caras de dos muros para considerar que están pegados. */
    private static final double TOUCH = 0.02;

    /** Un ambiente: sala, dormitorio, cocina... */
    public static final class Space {
        public String id;
        public String name;
        public double x, z;
        public FSHFProject project;
        /** Giro horario visto desde arriba: 0, 90, 180 o 270. */
        public int rotation;
        /** Piso: 0 es el primero. */
        public int level;

        public Space(String id, String name, double x, double z, FSHFProject project) {
            this(id, name, x, z, project, 0, 0);
        }

        public Space(String id, String name, double x, double z, FSHFProject project, int rotation, int level) {
            this.id = id;
            this.name = name;
            this.x = x;
            this.z = z;
            this.project = project;
            this.rotation = rotation;
            this.level = level;
        }

        public Space copy() {
            return new Space(id, name, x, z, project.copy(), rotation, level);
        }

        /** Ancho de la planta ya girada (a lo largo de X del plano). */
        public double planWidth() {
            return FSHFHome.planWidth(project.room, rotation);
        }

        /** Largo de la planta ya girada (a lo largo de Z del plano). */
        public double planLength() {
            return FSHFHome.planLength(project.room, rotation);
        }

        /** Planta exterior (incluye el grosor de los muros): {minX, minZ, maxX, maxZ} en el plano de la casa. */
        public double[] outer() {
            return box(x, z, rotation, project.room);
        }

        /**
         * Matriz que lleva un punto del ambiente (lx, lz) al plano de la casa:
         * {@code {mxx, mxz, tx, mzx, mzz, tz}}, con X = mxx·lx + mxz·lz + tx y Z = mzx·lx + mzz·lz + tz.
         */
        public double[] matrix() {
            double w = project.room.width(), l = project.room.length();
            return switch (rotation) {
                case 90 -> new double[] {0, -1, x + l, 1, 0, z};
                case 180 -> new double[] {-1, 0, x + w, 0, -1, z + l};
                case 270 -> new double[] {0, 1, x, -1, 0, z + w};
                default -> new double[] {1, 0, x, 0, 1, z};
            };
        }

        /** Punto del ambiente → plano de la casa. */
        public double[] toPlan(double lx, double lz) {
            double[] m = matrix();
            return new double[] {m[0] * lx + m[1] * lz + m[2], m[3] * lx + m[4] * lz + m[5]};
        }

        /** Punto del plano de la casa → ambiente. */
        public double[] toLocal(double px, double pz) {
            double[] m = matrix();
            double dx = px - m[2], dz = pz - m[5];
            // La parte de giro es ortogonal: su inversa es su transpuesta.
            return new double[] {m[0] * dx + m[3] * dz, m[1] * dx + m[4] * dz};
        }

        /** Hacia dónde mira, en el plano de la casa, una pared del ambiente. */
        public FSHFWallSide planSide(FSHFWallSide wall) {
            FSHFWallSide s = wall;
            for (int i = 0; i < rotation / 90; i++) s = clockwise(s);
            return s;
        }
    }

    /**
     * Una puerta vista desde la casa: si da a otro ambiente ({@code to} no es null), dónde cae en
     * la pared de ese otro ambiente; si no, es una puerta al exterior.
     */
    public record Link(Space from, FSHFOpening door, Space to, FSHFWallSide toWall, double toOffset, double toWidth) {
        public boolean exterior() {
            return to == null;
        }
    }

    public final List<Space> spaces = new ArrayList<>();

    /** Una casa con un solo ambiente (así se abren los proyectos de una sola habitación). */
    public static FSHFHome of(FSHFProject p, String name) {
        FSHFHome h = new FSHFHome();
        h.spaces.add(new Space("ambiente-1", name, 0, 0, p));
        return h;
    }

    public FSHFHome copy() {
        FSHFHome h = new FSHFHome();
        for (Space s : spaces) h.spaces.add(s.copy());
        return h;
    }

    public static double planWidth(FSHFRoomDims r, int rotation) {
        return rotation % 180 == 0 ? r.width() : r.length();
    }

    public static double planLength(FSHFRoomDims r, int rotation) {
        return rotation % 180 == 0 ? r.length() : r.width();
    }

    /** Planta exterior de una habitación con esas medidas, ubicada en (x, z) y girada. */
    public static double[] box(double x, double z, int rotation, FSHFRoomDims r) {
        double t = r.wallThickness();
        return new double[] {x - t, z - t, x + planWidth(r, rotation) + t, z + planLength(r, rotation) + t};
    }

    /** Giro de 90° en sentido horario: norte → este → sur → oeste. */
    public static FSHFWallSide clockwise(FSHFWallSide s) {
        return switch (s) {
            case NORTE -> FSHFWallSide.ESTE;
            case ESTE -> FSHFWallSide.SUR;
            case SUR -> FSHFWallSide.OESTE;
            case OESTE -> FSHFWallSide.NORTE;
        };
    }

    /** Lleva cualquier ángulo a 0, 90, 180 o 270 (el más cercano). */
    public static int normalizeRotation(double degrees) {
        if (!Double.isFinite(degrees)) return 0;
        long r = Math.round(degrees / 90) * 90;
        return (int) (((r % 360) + 360) % 360);
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

    // ---------------------------------------------------------------- Pisos

    /** Cuántos pisos tiene la casa (al menos 1). */
    public int levels() {
        int n = 0;
        for (Space s : spaces) n = Math.max(n, s.level);
        return n + 1;
    }

    /** Alto del piso: el del ambiente más alto de ese piso (2,5 m si no tiene ambientes). */
    public double levelHeight(int level) {
        double h = 0;
        for (Space s : spaces) if (s.level == level) h = Math.max(h, s.project.room.height());
        return h > 0 ? h : 2.5;
    }

    /** Altura del piso terminado de un nivel: los pisos de abajo más sus losas. */
    public double elevation(int level) {
        double y = 0;
        for (int k = 0; k < level; k++) y += levelHeight(k) + SLAB;
        return y;
    }

    // ---------------------------------------------------------------- Ubicación

    /** true si los dos ambientes se encimarían (tocarse muro con muro está bien). */
    public static boolean overlaps(double[] a, double[] b) {
        double eps = 1e-6;
        return a[0] < b[2] - eps && b[0] < a[2] - eps && a[1] < b[3] - eps && b[1] < a[3] - eps;
    }

    /** El primer ambiente con el que se enciman {@code s} si estuviera en (x, z), o null. */
    public Space collision(Space s, double x, double z) {
        return collision(s, x, z, s.rotation, s.level, s.project.room);
    }

    /** Igual, con otro giro, otro piso y otras medidas. Solo chocan los ambientes del mismo piso. */
    public Space collision(Space s, double x, double z, int rotation, int level, FSHFRoomDims room) {
        double[] b = box(x, z, rotation, room);
        for (Space o : spaces) if (o != s && o.level == level && overlaps(b, o.outer())) return o;
        return null;
    }

    /** Lugar libre en el primer piso (ver {@link #freeSpot(FSHFProject, int)}). */
    public double[] freeSpot(FSHFProject p) {
        return freeSpot(p, 0);
    }

    /**
     * Lugar libre para un ambiente nuevo en ese piso: a la derecha de lo que ya hay, pegado muro con
     * muro (alineado arriba con el ambiente que queda más al este). En un piso vacío, encima de la
     * esquina noroeste del piso de abajo.
     */
    public double[] freeSpot(FSHFProject p, int level) {
        double maxX = -Double.MAX_VALUE, z = 0;
        boolean any = false;
        for (Space s : spaces) {
            if (s.level != level) continue;
            any = true;
            double[] o = s.outer();
            if (o[2] > maxX) {
                maxX = o[2];
                z = s.z;
            }
        }
        if (!any) {
            double minX = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
            for (Space s : spaces) {
                if (s.level != level - 1) continue;
                minX = Math.min(minX, s.x);
                minZ = Math.min(minZ, s.z);
            }
            return minX == Double.MAX_VALUE ? new double[] {0, 0} : new double[] {minX, minZ};
        }
        double t = p.room.wallThickness();
        return new double[] {Math.round((maxX + t) * 100) / 100.0, z};
    }

    // ---------------------------------------------------------------- Puertas entre ambientes

    /**
     * Todas las puertas de la casa: a qué ambiente dan (cuando su muro está pegado al de otro
     * ambiente del mismo piso) o si son al exterior.
     */
    public List<Link> links() {
        List<Link> result = new ArrayList<>();
        for (Space a : spaces) {
            for (FSHFOpening o : a.project.openings) {
                if (!o.type.isDoor()) continue;
                Link link = null;
                for (Space b : spaces) {
                    if (b == a || b.level != a.level) continue;
                    link = linkTo(a, o, b);
                    if (link != null) break;
                }
                result.add(link != null ? link : new Link(a, o, null, null, 0, 0));
            }
        }
        return result;
    }

    /** Las puertas que salen al exterior. */
    public List<Link> exteriorDoors() {
        return links().stream().filter(Link::exterior).toList();
    }

    /**
     * Si la puerta {@code o} del ambiente {@code a} da contra un muro de {@code b}, dónde cae en ese
     * muro (recortada a lo que mide la pared de {@code b}). Null si no lo toca.
     */
    static Link linkTo(Space a, FSHFOpening o, Space b) {
        // Los dos extremos de la puerta sobre la cara exterior del muro de a, en el plano de la casa.
        double ta = a.project.room.wallThickness();
        double[][] ends = new double[2][];
        for (int i = 0; i < 2; i++) {
            double along = o.offset + i * o.width;
            double[] local = switch (o.wall) {
                case NORTE -> new double[] {along, -ta};
                case SUR -> new double[] {along, a.project.room.length() + ta};
                case OESTE -> new double[] {-ta, along};
                case ESTE -> new double[] {a.project.room.width() + ta, along};
            };
            double[] plan = a.toPlan(local[0], local[1]);
            ends[i] = b.toLocal(plan[0], plan[1]);
        }
        FSHFRoomDims r = b.project.room;
        double tb = r.wallThickness();
        for (FSHFWallSide side : FSHFWallSide.values()) {
            // La cara exterior de ese muro de b, en las coordenadas de b.
            double fixed = switch (side) {
                case NORTE -> -tb;
                case SUR -> r.length() + tb;
                case OESTE -> -tb;
                case ESTE -> r.width() + tb;
            };
            int across = side.alongX() ? 1 : 0, along = side.alongX() ? 0 : 1;
            if (Math.abs(ends[0][across] - fixed) > TOUCH || Math.abs(ends[1][across] - fixed) > TOUCH) continue;
            double lo = Math.max(0, Math.min(ends[0][along], ends[1][along]));
            double hi = Math.min(r.wallLength(side), Math.max(ends[0][along], ends[1][along]));
            if (hi - lo < 0.3) continue; // casi no se tocan: no hay paso
            return new Link(a, o, b, side, Math.round(lo * 100) / 100.0, Math.round((hi - lo) * 100) / 100.0);
        }
        return null;
    }

    /**
     * Los vanos que hay que abrir en los muros de {@code b} para que las puertas de los ambientes
     * vecinos no den contra una pared (solo para dibujar la casa; no se guardan).
     */
    public List<FSHFOpening> passagesInto(Space b) {
        List<FSHFOpening> result = new ArrayList<>();
        for (Link l : links()) {
            if (l.to() != b) continue;
            boolean taken = false;
            for (FSHFOpening mine : b.project.openings) {
                taken |= mine.wall == l.toWall() && mine.offset < l.toOffset() + l.toWidth() && l.toOffset() < mine.offset + mine.width;
            }
            if (taken) continue; // ya hay una abertura ahí (p. ej. la misma puerta puesta de los dos lados)
            result.add(new FSHFOpening("vano-" + l.from().id + "-" + l.door().id, FSHFOpeningType.PASSAGE, l.toWall(), l.toOffset(),
                    l.toWidth(), l.door().height, 0));
        }
        return result;
    }

    /**
     * Paredes del ambiente que dan al exterior (ningún otro ambiente del mismo piso está pegado a
     * ellas), en el orden sur, oeste, este, norte.
     */
    public List<FSHFWallSide> exteriorWalls(Space s) {
        List<FSHFWallSide> result = new ArrayList<>();
        double[] a = s.outer();
        for (FSHFWallSide wall : List.of(FSHFWallSide.SUR, FSHFWallSide.OESTE, FSHFWallSide.ESTE, FSHFWallSide.NORTE)) {
            FSHFWallSide side = s.planSide(wall);
            boolean touched = false;
            for (Space o : spaces) {
                if (o == s || o.level != s.level) continue;
                double[] b = o.outer();
                boolean spanX = a[0] < b[2] - TOUCH && b[0] < a[2] - TOUCH, spanZ = a[1] < b[3] - TOUCH && b[1] < a[3] - TOUCH;
                touched |= switch (side) {
                    case NORTE -> spanX && Math.abs(a[1] - b[3]) <= TOUCH;
                    case SUR -> spanX && Math.abs(a[3] - b[1]) <= TOUCH;
                    case OESTE -> spanZ && Math.abs(a[0] - b[2]) <= TOUCH;
                    case ESTE -> spanZ && Math.abs(a[2] - b[0]) <= TOUCH;
                };
            }
            if (!touched) result.add(wall);
        }
        return result;
    }
}
