-- =============================================================================
-- Amoblador3D: base de datos SQLite (esquema v4)
--
-- Es el mismo esquema que la aplicación crea sola al arrancar
-- (src/main/java/pe/edu/unaj/amoblador3d/persistence/Database.java), más un
-- proyecto de ejemplo. Sirve para revisar o preparar la base sin abrir la app:
--
--   sqlite3 amoblador3d.db < database/amoblador3d_sqlite.sql
--
-- o en DB Browser for SQLite: Archivo > Importar > Base de datos desde archivo SQL.
-- Para que la aplicación la use, copia el .db resultante a
-- %APPDATA%\Amoblador3D\amoblador3d.db (con la aplicación cerrada).
-- =============================================================================

PRAGMA foreign_keys = ON;

BEGIN;

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
);

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
);

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
);

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
    locked     INTEGER NOT NULL DEFAULT 0 CHECK (locked IN (0, 1)),  -- v4: bloqueado
    PRIMARY KEY (project_id, id)
);

CREATE TABLE catalog_model (
    id         TEXT PRIMARY KEY,
    name       TEXT NOT NULL,
    file       TEXT NOT NULL,
    width      REAL NOT NULL CHECK (width > 0),
    depth      REAL NOT NULL CHECK (depth > 0),
    height     REAL NOT NULL CHECK (height > 0),
    rotation   REAL NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL
);

-- v2: material y color de las paredes y el piso.
CREATE TABLE surface (
    project_id INTEGER NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    id         TEXT    NOT NULL,
    material   TEXT    NOT NULL,
    color      TEXT    NOT NULL,
    PRIMARY KEY (project_id, id)
);

-- v3: precios que el usuario cambió en el presupuesto. Solo se guardan los que difieren de
-- los referenciales (los referenciales están en el código: estimate/PriceList.java).
CREATE TABLE price (
    kind  TEXT NOT NULL CHECK (kind IN ('material', 'furniture', 'extra')),
    item  TEXT NOT NULL,
    price REAL NOT NULL CHECK (price >= 0),
    PRIMARY KEY (kind, item)
);

-- La aplicación lee este número para saber que las tablas ya están creadas.
PRAGMA user_version = 4;

-- -----------------------------------------------------------------------------
-- Proyecto de ejemplo: dormitorio de 4 x 3.5 m
-- -----------------------------------------------------------------------------
INSERT INTO project (id, name, width, length, height, wall_thickness, ambient, created_at, updated_at)
VALUES (1, 'Dormitorio de ejemplo', 4.0, 3.5, 2.5, 0.15, 0.6, '2026-10-25T12:00:00Z', '2026-10-25T12:00:00Z');

INSERT INTO surface (project_id, id, material, color) VALUES
    (1, 'piso',        'madera',   '#b07d4f'),
    (1, 'pared-norte', 'ladrillo', '#a4553a'),
    (1, 'pared-sur',   'pintura',  '#f2efe9'),
    (1, 'pared-este',  'pintura',  '#f2efe9'),
    (1, 'pared-oeste', 'pintura',  '#f2efe9');

INSERT INTO opening (project_id, position, id, type, wall, wall_offset, width, height, sill) VALUES
    (1, 0, 'window-1', 'window', 'norte', 1.40, 1.2, 1.0, 0.9),
    (1, 1, 'door-1',   'door',   'sur',   1.55, 0.9, 2.1, 0.0);

INSERT INTO light (project_id, position, id, type, color, intensity, cast_shadow, x, y, z, window_id, elevation, deviation) VALUES
    (1, 0, 'point-1', 'point', '#ffe2b8', 6.0, 1, 2.0, 2.25, 1.75, NULL, NULL, NULL),
    (1, 1, 'sun-1',   'sun',   '#fff1d6', 3.0, 1, NULL, NULL, NULL, 'window-1', 35.0, 20.0);

INSERT INTO furniture (project_id, position, id, type, x, z, rotation, width, depth, height, color, locked) VALUES
    (1, 0, 'cama-1',       'cama',       2.0, 1.6, 0, 1.40, 1.9, 0.90, '#8b6b4a', 1),
    (1, 1, 'escritorio-1', 'escritorio', 3.3, 1.7, 0, 1.20, 0.6, 0.75, '#d9c6a5', 0),
    (1, 2, 'silla-1',      'silla',      1.0, 1.7, 0, 0.45, 0.5, 0.90, '#6d4c33', 0),
    (1, 3, 'closet-1',     'closet',     3.3, 2.3, 0, 1.20, 0.6, 2.00, '#e8e2d6', 0);

-- Precio cambiado por el usuario: la pintura mate a S/ 70 el galón.
INSERT INTO price (kind, item, price) VALUES ('material', 'pintura', 70.0);

COMMIT;
