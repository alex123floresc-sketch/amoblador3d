package pe.edu.unaj.amoblador3d.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import pe.edu.unaj.amoblador3d.model.CameraView;
import pe.edu.unaj.amoblador3d.model.Column;
import pe.edu.unaj.amoblador3d.model.Furniture;
import pe.edu.unaj.amoblador3d.model.Home;
import pe.edu.unaj.amoblador3d.model.Light;
import pe.edu.unaj.amoblador3d.model.LightType;
import pe.edu.unaj.amoblador3d.model.Opening;
import pe.edu.unaj.amoblador3d.model.OpeningType;
import pe.edu.unaj.amoblador3d.model.Project;
import pe.edu.unaj.amoblador3d.model.RoomDims;
import pe.edu.unaj.amoblador3d.model.Surface;
import pe.edu.unaj.amoblador3d.model.SurfaceMaterial;
import pe.edu.unaj.amoblador3d.model.WallSide;

/** Guarda y lee proyectos en las tablas project, surface, opening, light y furniture. */
public final class ProjectRepository {
    /** Fila de la lista de proyectos (sin el contenido completo). */
    public record Summary(long id, String name, Instant updatedAt, byte[] thumbnail) {}

    private final Database db;

    public ProjectRepository(Database db) {
        this.db = db;
    }

    /** Proyectos guardados, el más reciente primero. */
    public List<Summary> list() throws SQLException {
        List<Summary> result = new ArrayList<>();
        String sql = "SELECT id, name, updated_at, thumbnail FROM project ORDER BY updated_at DESC";
        try (Statement st = db.connection().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                result.add(new Summary(rs.getLong(1), rs.getString(2), Instant.parse(rs.getString(3)), rs.getBytes(4)));
            }
        }
        return result;
    }

    /** Id del proyecto con ese nombre (sin distinguir mayúsculas), o null. */
    public Long findByName(String name) throws SQLException {
        try (PreparedStatement ps = db.connection().prepareStatement("SELECT id FROM project WHERE name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }

    /**
     * Guarda el proyecto completo en una transacción.
     *
     * @param id null para crear un proyecto nuevo
     * @return el id del proyecto
     */
    public long save(Long id, String name, Project p, byte[] thumbnail) throws SQLException {
        return saveHome(id, name, Home.of(p, "Ambiente 1"), thumbnail);
    }

    /**
     * Guarda la casa completa en una transacción: el primer ambiente en las tablas de siempre y
     * los demás en home_space.
     *
     * @param id null para crear un proyecto nuevo
     * @return el id del proyecto
     */
    public long saveHome(Long id, String name, Home home, byte[] thumbnail) throws SQLException {
        if (home.spaces.isEmpty()) throw new SQLException("La casa no tiene ambientes.");
        return db.transaction(c -> {
            long projectId = saveIn(c, id, name, home.spaces.get(0).project, thumbnail);
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM home_space WHERE project_id = ?")) {
                ps.setLong(1, projectId);
                ps.executeUpdate();
            }
            String sql = "INSERT INTO home_space (project_id, position, id, name, x, z, data) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 0; i < home.spaces.size(); i++) {
                    Home.Space s = home.spaces.get(i);
                    ps.setLong(1, projectId);
                    ps.setInt(2, i);
                    ps.setString(3, s.id);
                    ps.setString(4, s.name);
                    ps.setDouble(5, s.x);
                    ps.setDouble(6, s.z);
                    if (i == 0) ps.setNull(7, Types.VARCHAR);
                    else ps.setString(7, ProjectJson.write(s.project));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            return projectId;
        });
    }

    /** Lee la casa completa. Un proyecto de una sola habitación es una casa con un ambiente. Null si no existe. */
    public Home loadHome(long id) throws SQLException, ProjectFormatException {
        Project first = load(id);
        if (first == null) return null;
        Home h = new Home();
        String sql = "SELECT id, name, x, z, data FROM home_space WHERE project_id = ? ORDER BY position";
        try (PreparedStatement ps = db.connection().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String data = rs.getString(5);
                    Project p = h.spaces.isEmpty() ? first : data == null ? null : ProjectJson.read(data);
                    if (p == null) throw new ProjectFormatException("Un ambiente de la casa está dañado.");
                    h.spaces.add(new Home.Space(rs.getString(1), rs.getString(2), rs.getDouble(3), rs.getDouble(4), p));
                }
            }
        }
        if (h.spaces.isEmpty()) return Home.of(first, "Ambiente 1");
        HomeJson.fix(h);
        return h;
    }

    private long saveIn(Connection c, Long id, String name, Project p, byte[] thumbnail) throws SQLException {
        String now = Instant.now().toString();
        long projectId;
        if (id == null) {
            String sql = "INSERT INTO project (name, width, length, height, wall_thickness, ambient, baseboard_height,"
                    + " baseboard_color, created_at, updated_at, thumbnail) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bindProject(ps, name, p);
                ps.setString(9, now);
                ps.setString(10, now);
                ps.setBytes(11, thumbnail);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    projectId = keys.getLong(1);
                }
            }
        } else {
            projectId = id;
            String sql = "UPDATE project SET name = ?, width = ?, length = ?, height = ?, wall_thickness = ?, ambient = ?,"
                    + " baseboard_height = ?, baseboard_color = ?, updated_at = ?, thumbnail = ? WHERE id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                bindProject(ps, name, p);
                ps.setString(9, now);
                ps.setBytes(10, thumbnail);
                ps.setLong(11, projectId);
                if (ps.executeUpdate() == 0) throw new SQLException("El proyecto ya no existe en la base de datos.");
            }
            for (String table : List.of("surface", "opening", "light", "furniture", "camera_view", "room_column")) {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM " + table + " WHERE project_id = ?")) {
                    ps.setLong(1, projectId);
                    ps.executeUpdate();
                }
            }
        }
        insertChildren(c, projectId, p);
        return projectId;
    }

    private static void bindProject(PreparedStatement ps, String name, Project p) throws SQLException {
        ps.setString(1, name);
        ps.setDouble(2, p.room.width());
        ps.setDouble(3, p.room.length());
        ps.setDouble(4, p.room.height());
        ps.setDouble(5, p.room.wallThickness());
        ps.setDouble(6, p.ambient);
        ps.setDouble(7, p.baseboard.height);
        ps.setString(8, p.baseboard.color);
    }

    private static void insertChildren(Connection c, long projectId, Project p) throws SQLException {
        String sql = "INSERT INTO surface (project_id, id, material, color) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (Surface s : p.surfaces()) {
                ps.setLong(1, projectId);
                ps.setString(2, s.id);
                ps.setString(3, s.material.id());
                ps.setString(4, s.color);
                ps.addBatch();
            }
            ps.executeBatch();
        }

        sql = "INSERT INTO opening (project_id, position, id, type, wall, wall_offset, width, height, sill)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 0;
            for (Opening o : p.openings) {
                ps.setLong(1, projectId);
                ps.setInt(2, i++);
                ps.setString(3, o.id);
                ps.setString(4, o.type.id());
                ps.setString(5, o.wall.id());
                ps.setDouble(6, o.offset);
                ps.setDouble(7, o.width);
                ps.setDouble(8, o.height);
                ps.setDouble(9, o.sill);
                ps.addBatch();
            }
            ps.executeBatch();
        }

        sql = "INSERT INTO light (project_id, position, id, type, color, intensity, cast_shadow, x, y, z,"
                + " target_x, target_z, angle, penumbra, window_id, elevation, deviation)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 0;
            for (Light l : p.lights) {
                ps.setLong(1, projectId);
                ps.setInt(2, i++);
                ps.setString(3, l.id);
                ps.setString(4, l.type.id());
                ps.setString(5, l.color);
                ps.setDouble(6, l.intensity);
                ps.setInt(7, l.castShadow ? 1 : 0);
                ps.setDouble(8, l.x);
                ps.setDouble(9, l.y);
                ps.setDouble(10, l.z);
                ps.setDouble(11, l.targetX);
                ps.setDouble(12, l.targetZ);
                ps.setDouble(13, l.angle);
                ps.setDouble(14, l.penumbra);
                if (l.windowId != null) ps.setString(15, l.windowId);
                else ps.setNull(15, Types.VARCHAR);
                ps.setDouble(16, l.elevation);
                ps.setDouble(17, l.deviation);
                ps.addBatch();
            }
            ps.executeBatch();
        }

        sql = "INSERT INTO furniture (project_id, position, id, type, x, z, rotation, width, depth, height, color, locked,"
                + " color2, finish) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 0;
            for (Furniture f : p.furniture) {
                ps.setLong(1, projectId);
                ps.setInt(2, i++);
                ps.setString(3, f.id);
                ps.setString(4, f.type);
                ps.setDouble(5, f.x);
                ps.setDouble(6, f.z);
                ps.setDouble(7, f.rotation);
                ps.setDouble(8, f.width);
                ps.setDouble(9, f.depth);
                ps.setDouble(10, f.height);
                if (f.color != null) ps.setString(11, f.color);
                else ps.setNull(11, Types.VARCHAR);
                ps.setInt(12, f.locked ? 1 : 0);
                if (f.color2 != null) ps.setString(13, f.color2);
                else ps.setNull(13, Types.VARCHAR);
                if (f.finish != null) ps.setString(14, f.finish.id());
                else ps.setNull(14, Types.VARCHAR);
                ps.addBatch();
            }
            ps.executeBatch();
        }

        sql = "INSERT INTO room_column (project_id, position, id, kind, corner, x, z, width, depth, material, color)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 0;
            for (Column col : p.columns) {
                ps.setLong(1, projectId);
                ps.setInt(2, i++);
                ps.setString(3, col.id);
                ps.setString(4, col.kind.id());
                if (col.corner != null) ps.setString(5, col.corner.id());
                else ps.setNull(5, Types.VARCHAR);
                ps.setDouble(6, col.x);
                ps.setDouble(7, col.z);
                ps.setDouble(8, col.width);
                ps.setDouble(9, col.depth);
                ps.setString(10, col.material.id());
                ps.setString(11, col.color);
                ps.addBatch();
            }
            ps.executeBatch();
        }

        sql = "INSERT INTO camera_view (project_id, position, name, mode, x, y, z, yaw, pitch, distance)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 0;
            for (CameraView v : p.views) {
                ps.setLong(1, projectId);
                ps.setInt(2, i++);
                ps.setString(3, v.name());
                ps.setString(4, v.kind().id());
                ps.setDouble(5, v.x());
                ps.setDouble(6, v.y());
                ps.setDouble(7, v.z());
                ps.setDouble(8, v.yaw());
                ps.setDouble(9, v.pitch());
                ps.setDouble(10, v.distance());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /** Lee un proyecto completo y lo valida. Devuelve null si no existe. */
    public Project load(long id) throws SQLException, ProjectFormatException {
        Connection c = db.connection();
        Project p = new Project();
        String sql = "SELECT width, length, height, wall_thickness, ambient, baseboard_height, baseboard_color"
                + " FROM project WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                p.room = new RoomDims(rs.getDouble(1), rs.getDouble(2), rs.getDouble(3), rs.getDouble(4));
                p.ambient = rs.getDouble(5);
                p.baseboard.height = rs.getDouble(6);
                p.baseboard.color = rs.getString(7);
            }
        }
        try {
            // Los proyectos guardados antes de la v2 no tienen filas aquí: quedan los acabados por defecto.
            sql = "SELECT id, material, color FROM surface WHERE project_id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String sid = rs.getString(1);
                        Surface s = null;
                        for (Surface any : p.surfaces()) if (any.id.equals(sid)) s = any;
                        if (s == null) continue;
                        s.material = SurfaceMaterial.fromId(rs.getString(2));
                        s.color = rs.getString(3);
                    }
                }
            }
            sql = "SELECT id, type, wall, wall_offset, width, height, sill FROM opening WHERE project_id = ? ORDER BY position";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        p.openings.add(new Opening(rs.getString(1), OpeningType.fromId(rs.getString(2)),
                                WallSide.fromId(rs.getString(3)), rs.getDouble(4), rs.getDouble(5), rs.getDouble(6), rs.getDouble(7)));
                    }
                }
            }
            sql = "SELECT id, type, color, intensity, cast_shadow, x, y, z, target_x, target_z, angle, penumbra,"
                    + " window_id, elevation, deviation FROM light WHERE project_id = ? ORDER BY position";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Light l = new Light(rs.getString(1), LightType.fromId(rs.getString(2)));
                        l.color = rs.getString(3);
                        l.intensity = rs.getDouble(4);
                        l.castShadow = rs.getInt(5) != 0;
                        l.x = rs.getDouble(6);
                        l.y = rs.getDouble(7);
                        l.z = rs.getDouble(8);
                        l.targetX = rs.getDouble(9);
                        l.targetZ = rs.getDouble(10);
                        l.angle = rs.getDouble(11);
                        l.penumbra = rs.getDouble(12);
                        l.windowId = rs.getString(13);
                        l.elevation = rs.getDouble(14);
                        l.deviation = rs.getDouble(15);
                        p.lights.add(l);
                    }
                }
            }
            sql = "SELECT id, kind, corner, x, z, width, depth, material, color FROM room_column"
                    + " WHERE project_id = ? ORDER BY position";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Column col = new Column(rs.getString(1), Column.Kind.fromId(rs.getString(2)), rs.getDouble(4),
                                rs.getDouble(5), rs.getDouble(6), rs.getDouble(7));
                        if (rs.getString(3) != null) col.corner = Column.Corner.fromId(rs.getString(3));
                        col.material = SurfaceMaterial.fromId(rs.getString(8));
                        col.color = rs.getString(9);
                        p.columns.add(col);
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            throw new ProjectFormatException("El proyecto guardado tiene datos no válidos: " + e.getMessage());
        }
        sql = "SELECT id, type, x, z, rotation, width, depth, height, color, locked, color2, finish FROM furniture"
                + " WHERE project_id = ? ORDER BY position";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Furniture f = new Furniture(rs.getString(1), rs.getString(2), rs.getDouble(3), rs.getDouble(4),
                            rs.getDouble(5), rs.getDouble(6), rs.getDouble(7), rs.getDouble(8), rs.getString(9));
                    f.locked = rs.getInt(10) != 0;
                    f.color2 = rs.getString(11);
                    f.finish = pe.edu.unaj.amoblador3d.model.Finish.fromId(rs.getString(12));
                    p.furniture.add(f);
                }
            }
        }
        sql = "SELECT name, mode, x, y, z, yaw, pitch, distance FROM camera_view WHERE project_id = ? ORDER BY position";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        p.views.add(new CameraView(rs.getString(1), CameraView.Kind.fromId(rs.getString(2)), rs.getDouble(3),
                                rs.getDouble(4), rs.getDouble(5), rs.getDouble(6), rs.getDouble(7), rs.getDouble(8)));
                    } catch (IllegalArgumentException ignored) {
                        // tipo de vista desconocido: se descarta
                    }
                }
            }
        }
        ProjectValidator.validate(p);
        return p;
    }

    public void rename(long id, String name) throws SQLException {
        try (PreparedStatement ps = db.connection().prepareStatement("UPDATE project SET name = ? WHERE id = ?")) {
            ps.setString(1, name);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    /** Elimina el proyecto; sus aberturas, luces y muebles se borran en cascada. */
    public void delete(long id) throws SQLException {
        try (PreparedStatement ps = db.connection().prepareStatement("DELETE FROM project WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }
}
