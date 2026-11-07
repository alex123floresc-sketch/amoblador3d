package pe.edu.unaj.amoblador3d.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class FSHFDatabase implements AutoCloseable {
    static final int SCHEMA_VERSION = 9;

    private static final String[] SCHEMA_V1 = {
        """
        CREATE TABLE project (
            id             INTEGER PRIMARY KEY AUTOINCREMENT,
            name           TEXT    NOT NULL UNIQUE COLLATE NOCASE,
            width          REAL    NOT NULL,
            length         REAL    NOT NULL,
            height         REAL    NOT NULL,
            wall_thickness REAL    NOT NULL,
            ambient        REAL    NOT NULL,
            created_at     TEXT    NOT NULL,
            updated_at     TEXT    NOT NULL,
            thumbnail      BLOB
        )""",
        """
        CREATE TABLE opening (
            project_id  INTEGER NOT NULL REFERENCES project(id) ON DELETE CASCADE,
            position    INTEGER NOT NULL,
            id          TEXT    NOT NULL,
            type        TEXT    NOT NULL CHECK (type IN ('window', 'door')),
            wall        TEXT    NOT NULL CHECK (wall IN ('norte', 'sur', 'este', 'oeste')),
            wall_offset REAL    NOT NULL,
            width       REAL    NOT NULL,
            height      REAL    NOT NULL,
            sill        REAL    NOT NULL,
            PRIMARY KEY (project_id, id)
        )""",
        """
        CREATE TABLE light (
            project_id  INTEGER NOT NULL REFERENCES project(id) ON DELETE CASCADE,
            position    INTEGER NOT NULL,
            id          TEXT    NOT NULL,
            type        TEXT    NOT NULL CHECK (type IN ('point', 'spot', 'sun')),
            color       TEXT    NOT NULL,
            intensity   REAL    NOT NULL,
            cast_shadow INTEGER NOT NULL,
            x REAL, y REAL, z REAL,
            target_x REAL, target_z REAL, angle REAL, penumbra REAL,
            window_id TEXT, elevation REAL, deviation REAL,
            PRIMARY KEY (project_id, id)
        )""",
        """
        CREATE TABLE furniture (
            project_id INTEGER NOT NULL REFERENCES project(id) ON DELETE CASCADE,
            position   INTEGER NOT NULL,
            id         TEXT    NOT NULL,
            type       TEXT    NOT NULL,
            x          REAL    NOT NULL,
            z          REAL    NOT NULL,
            rotation   REAL    NOT NULL,
            width      REAL    NOT NULL,
            depth      REAL    NOT NULL,
            height     REAL    NOT NULL,
            color      TEXT,
            PRIMARY KEY (project_id, id)
        )""",
        """
        CREATE TABLE catalog_model (
            id         TEXT PRIMARY KEY,
            name       TEXT NOT NULL,
            file       TEXT NOT NULL,
            width      REAL NOT NULL CHECK (width > 0),
            depth      REAL NOT NULL CHECK (depth > 0),
            height     REAL NOT NULL CHECK (height > 0),
            rotation   REAL NOT NULL DEFAULT 0,
            created_at TEXT NOT NULL
        )""",
    };

    private static final String[] SCHEMA_V2 = {
        """
        CREATE TABLE surface (
            project_id INTEGER NOT NULL REFERENCES project(id) ON DELETE CASCADE,
            id         TEXT    NOT NULL,
            material   TEXT    NOT NULL,
            color      TEXT    NOT NULL,
            PRIMARY KEY (project_id, id)
        )""",
    };

    private static final String[] SCHEMA_V3 = {
        """
        CREATE TABLE price (
            kind  TEXT NOT NULL CHECK (kind IN ('material', 'furniture', 'extra')),
            item  TEXT NOT NULL,
            price REAL NOT NULL CHECK (price >= 0),
            PRIMARY KEY (kind, item)
        )""",
    };

    private static final String[] SCHEMA_V4 = {
        "ALTER TABLE furniture ADD COLUMN locked INTEGER NOT NULL DEFAULT 0 CHECK (locked IN (0, 1))",
    };

    private static final String[] SCHEMA_V5 = {
        """
        CREATE TABLE camera_view (
            project_id INTEGER NOT NULL REFERENCES project(id) ON DELETE CASCADE,
            position   INTEGER NOT NULL,
            name       TEXT    NOT NULL,
            mode       TEXT    NOT NULL CHECK (mode IN ('3d', 'plan', 'walk')),
            x REAL NOT NULL, y REAL NOT NULL, z REAL NOT NULL,
            yaw REAL NOT NULL, pitch REAL NOT NULL, distance REAL NOT NULL,
            PRIMARY KEY (project_id, position)
        )""",
    };

    private static final String[] SCHEMA_V6 = {
        "ALTER TABLE project ADD COLUMN baseboard_height REAL NOT NULL DEFAULT 0.08",
        "ALTER TABLE project ADD COLUMN baseboard_color TEXT NOT NULL DEFAULT '#f7f5f0'",
        """
        CREATE TABLE room_column (
            project_id INTEGER NOT NULL REFERENCES project(id) ON DELETE CASCADE,
            position   INTEGER NOT NULL,
            id         TEXT    NOT NULL,
            kind       TEXT    NOT NULL CHECK (kind IN ('column', 'wall', 'notch')),
            corner     TEXT    CHECK (corner IN ('noroeste', 'noreste', 'suroeste', 'sureste')),
            x          REAL    NOT NULL,
            z          REAL    NOT NULL,
            width      REAL    NOT NULL CHECK (width > 0),
            depth      REAL    NOT NULL CHECK (depth > 0),
            material   TEXT    NOT NULL,
            color      TEXT    NOT NULL,
            PRIMARY KEY (project_id, id)
        )""",
    };

    private static final String[] SCHEMA_V7 = {
        "ALTER TABLE furniture ADD COLUMN color2 TEXT",
        "ALTER TABLE furniture ADD COLUMN finish TEXT",
        """
        CREATE TABLE custom_design (
            id         TEXT PRIMARY KEY,
            name       TEXT NOT NULL,
            data       TEXT NOT NULL,
            created_at TEXT NOT NULL,
            updated_at TEXT NOT NULL
        )""",
    };

    private static final String[] SCHEMA_V8 = {
        """
        CREATE TABLE home_space (
            project_id INTEGER NOT NULL REFERENCES project(id) ON DELETE CASCADE,
            position   INTEGER NOT NULL,
            id         TEXT    NOT NULL,
            name       TEXT    NOT NULL,
            x          REAL    NOT NULL,
            z          REAL    NOT NULL,
            data       TEXT,
            PRIMARY KEY (project_id, position)
        )""",
    };

    private static final String[] SCHEMA_V9 = {
        "ALTER TABLE home_space ADD COLUMN rotation INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE home_space ADD COLUMN level INTEGER NOT NULL DEFAULT 0",
        """
        CREATE TABLE opening_v9 (
            project_id  INTEGER NOT NULL REFERENCES project(id) ON DELETE CASCADE,
            position    INTEGER NOT NULL,
            id          TEXT    NOT NULL,
            type        TEXT    NOT NULL CHECK (type IN ('window', 'door', 'entry', 'double', 'sliding', 'garage', 'passage')),
            wall        TEXT    NOT NULL CHECK (wall IN ('norte', 'sur', 'este', 'oeste')),
            wall_offset REAL    NOT NULL,
            width       REAL    NOT NULL,
            height      REAL    NOT NULL,
            sill        REAL    NOT NULL,
            PRIMARY KEY (project_id, id)
        )""",
        "INSERT INTO opening_v9 SELECT project_id, position, id, type, wall, wall_offset, width, height, sill FROM opening",
        "DROP TABLE opening",
        "ALTER TABLE opening_v9 RENAME TO opening",
    };

    private static final String[][] STEPS = {SCHEMA_V1, SCHEMA_V2, SCHEMA_V3, SCHEMA_V4, SCHEMA_V5, SCHEMA_V6, SCHEMA_V7,
        SCHEMA_V8, SCHEMA_V9};

    private final Connection connection;

    public FSHFDatabase(Path file) throws SQLException {
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
        } catch (IOException e) {
            throw new SQLException("No se pudo crear la carpeta de datos: " + e.getMessage(), e);
        }
        connection = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        migrate();
    }

    public Connection connection() {
        return connection;
    }

    @FunctionalInterface
    public interface SqlWork<T> {
        T run(Connection c) throws SQLException;
    }

    public <T> T transaction(SqlWork<T> work) throws SQLException {
        connection.setAutoCommit(false);
        try {
            T result = work.run(connection);
            connection.commit();
            return result;
        } catch (SQLException | RuntimeException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private void migrate() throws SQLException {
        int version;
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery("PRAGMA user_version")) {
            version = rs.next() ? rs.getInt(1) : 0;
        }
        if (version > SCHEMA_VERSION) {
            throw new SQLException("La base de datos fue creada con una versión más nueva de Amoblador3D.");
        }
        for (int v = version; v < SCHEMA_VERSION; v++) {
            String[] step = STEPS[v];
            int next = v + 1;
            transaction(c -> {
                try (Statement st = c.createStatement()) {
                    for (String sql : step) st.execute(sql);
                    st.execute("PRAGMA user_version = " + next);
                }
                return null;
            });
        }
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}
