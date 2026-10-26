package pe.edu.unaj.amoblador3d.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Conexión a la base de datos SQLite y creación/actualización de las tablas.
 *
 * <p>La versión del esquema se guarda en {@code PRAGMA user_version}: para cambiar las
 * tablas en el futuro, se agrega un paso en {@link #migrate()} y se sube {@link #SCHEMA_VERSION}.
 */
public final class Database implements AutoCloseable {
    static final int SCHEMA_VERSION = 3;

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

    /** v2: material y color de las paredes y el piso. */
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

    /** v3: precios que el usuario cambió en el presupuesto (los demás son los referenciales). */
    private static final String[] SCHEMA_V3 = {
        """
        CREATE TABLE price (
            kind  TEXT NOT NULL CHECK (kind IN ('material', 'furniture', 'extra')),
            item  TEXT NOT NULL,
            price REAL NOT NULL CHECK (price >= 0),
            PRIMARY KEY (kind, item)
        )""",
    };

    /** Pasos de migración: el paso i lleva la base de la versión i a la i + 1. */
    private static final String[][] STEPS = {SCHEMA_V1, SCHEMA_V2, SCHEMA_V3};

    private final Connection connection;

    public Database(Path file) throws SQLException {
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
        } catch (IOException e) {
            throw new SQLException("No se pudo crear la carpeta de datos: " + e.getMessage(), e);
        }
        connection = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON"); // SQLite no revisa las claves foráneas si no se activa
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

    /**
     * Ejecuta todo o nada: si algo falla a la mitad, se deshacen los cambios.
     * (Antes, un error al guardar podía dejar el archivo del proyecto a medio escribir).
     */
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
