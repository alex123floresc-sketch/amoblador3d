package pe.edu.unaj.amoblador3d.model;

import java.util.ArrayList;
import java.util.List;

public final class FSHFHome {
    public static final int MAX_SPACES = 30, MAX_NAME = 40, MAX_LEVELS = 4;
    public static final double SLAB = 0.2;
    private static final double TOUCH = 0.02;

    public static final class Space {
        public String id;
        public String name;
        public double x, z;
        public FSHFProject project;
        public int rotation;
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

        public double planWidth() {
            return FSHFHome.planWidth(project.room, rotation);
        }

        public double planLength() {
            return FSHFHome.planLength(project.room, rotation);
        }

        public double[] outer() {
            return box(x, z, rotation, project.room);
        }

        public double[] matrix() {
            double w = project.room.width(), l = project.room.length();
            return switch (rotation) {
                case 90 -> new double[] {0, -1, x + l, 1, 0, z};
                case 180 -> new double[] {-1, 0, x + w, 0, -1, z + l};
                case 270 -> new double[] {0, 1, x, -1, 0, z + w};
                default -> new double[] {1, 0, x, 0, 1, z};
            };
        }

        public double[] toPlan(double lx, double lz) {
            double[] m = matrix();
            return new double[] {m[0] * lx + m[1] * lz + m[2], m[3] * lx + m[4] * lz + m[5]};
        }

        public double[] toLocal(double px, double pz) {
            double[] m = matrix();
            double dx = px - m[2], dz = pz - m[5];

            return new double[] {m[0] * dx + m[3] * dz, m[1] * dx + m[4] * dz};
        }

        public FSHFWallSide planSide(FSHFWallSide wall) {
            FSHFWallSide s = wall;
            for (int i = 0; i < rotation / 90; i++) s = clockwise(s);
            return s;
        }
    }

    public record Link(Space from, FSHFOpening door, Space to, FSHFWallSide toWall, double toOffset, double toWidth) {
        public boolean exterior() {
            return to == null;
        }
    }

    public final List<Space> spaces = new ArrayList<>();

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

    public static double[] box(double x, double z, int rotation, FSHFRoomDims r) {
        double t = r.wallThickness();
        return new double[] {x - t, z - t, x + planWidth(r, rotation) + t, z + planLength(r, rotation) + t};
    }

    public static FSHFWallSide clockwise(FSHFWallSide s) {
        return switch (s) {
            case NORTE -> FSHFWallSide.ESTE;
            case ESTE -> FSHFWallSide.SUR;
            case SUR -> FSHFWallSide.OESTE;
            case OESTE -> FSHFWallSide.NORTE;
        };
    }

    public static int normalizeRotation(double degrees) {
        if (!Double.isFinite(degrees)) return 0;
        long r = Math.round(degrees / 90) * 90;
        return (int) (((r % 360) + 360) % 360);
    }

    public String nextId() {
        int n = 1;
        while (true) {
            String id = "ambiente-" + n;
            if (spaces.stream().noneMatch(s -> s.id.equals(id))) return id;
            n++;
        }
    }

    public String uniqueName(String base) {
        String trimmed = base == null || base.isBlank() ? "Ambiente" : base.trim();
        String b = trimmed.length() > MAX_NAME - 3 ? trimmed.substring(0, MAX_NAME - 3) : trimmed;
        if (spaces.stream().noneMatch(s -> s.name.equalsIgnoreCase(b))) return b;
        for (int i = 2; ; i++) {
            String name = b + " " + i;
            if (spaces.stream().noneMatch(s -> s.name.equalsIgnoreCase(name))) return name;
        }
    }

    public int levels() {
        int n = 0;
        for (Space s : spaces) n = Math.max(n, s.level);
        return n + 1;
    }

    public double levelHeight(int level) {
        double h = 0;
        for (Space s : spaces) if (s.level == level) h = Math.max(h, s.project.room.height());
        return h > 0 ? h : 2.5;
    }

    public double elevation(int level) {
        double y = 0;
        for (int k = 0; k < level; k++) y += levelHeight(k) + SLAB;
        return y;
    }

    public static boolean overlaps(double[] a, double[] b) {
        double eps = 1e-6;
        return a[0] < b[2] - eps && b[0] < a[2] - eps && a[1] < b[3] - eps && b[1] < a[3] - eps;
    }

    public Space collision(Space s, double x, double z) {
        return collision(s, x, z, s.rotation, s.level, s.project.room);
    }

    public Space collision(Space s, double x, double z, int rotation, int level, FSHFRoomDims room) {
        double[] b = box(x, z, rotation, room);
        for (Space o : spaces) if (o != s && o.level == level && overlaps(b, o.outer())) return o;
        return null;
    }

    public double[] freeSpot(FSHFProject p) {
        return freeSpot(p, 0);
    }

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

    public List<Link> exteriorDoors() {
        return links().stream().filter(Link::exterior).toList();
    }

    static Link linkTo(Space a, FSHFOpening o, Space b) {
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
            if (hi - lo < 0.3) continue;
            return new Link(a, o, b, side, Math.round(lo * 100) / 100.0, Math.round((hi - lo) * 100) / 100.0);
        }
        return null;
    }

    public List<FSHFOpening> passagesInto(Space b) {
        List<FSHFOpening> result = new ArrayList<>();
        for (Link l : links()) {
            if (l.to() != b) continue;
            boolean taken = false;
            for (FSHFOpening mine : b.project.openings) {
                taken |= mine.wall == l.toWall() && mine.offset < l.toOffset() + l.toWidth() && l.toOffset() < mine.offset + mine.width;
            }
            if (taken) continue;
            result.add(new FSHFOpening("vano-" + l.from().id + "-" + l.door().id, FSHFOpeningType.PASSAGE, l.toWall(), l.toOffset(),
                    l.toWidth(), l.door().height, 0));
        }
        return result;
    }

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
