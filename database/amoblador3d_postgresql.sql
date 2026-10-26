-- =============================================================================
-- Amoblador3D: base de datos en PostgreSQL
--
-- Versión en PostgreSQL de la base SQLite que usa la aplicación (esquema v2, ver
-- database/amoblador3d_sqlite.sql). Mismas tablas y columnas, con los tipos y
-- reglas propios de PostgreSQL:
--
--   SQLite                              PostgreSQL
--   INTEGER PRIMARY KEY AUTOINCREMENT   BIGINT GENERATED ALWAYS AS IDENTITY
--   REAL                                DOUBLE PRECISION
--   TEXT con fecha ISO                  TIMESTAMPTZ
--   BLOB                                BYTEA
--   INTEGER 0/1                         BOOLEAN
--   UNIQUE COLLATE NOCASE               índice único sobre lower(name)
--
-- Además agrega: un dominio para colores, la tabla surface_material (catálogo de
-- acabados), un trigger que impide poner un material donde no corresponde (p. ej.
-- alfombra en una pared), updated_at automático y vistas de consulta.
--
-- Ejecutar (crea la base "amoblador3d" si no existe y todo dentro del esquema
-- "amoblador3d"; se puede volver a ejecutar, porque borra y recrea el esquema):
--
--   psql -U postgres -f database/amoblador3d_postgresql.sql
-- =============================================================================

\set ON_ERROR_STOP on

SELECT 'CREATE DATABASE amoblador3d ENCODING ''UTF8'''
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'amoblador3d') \gexec

\connect amoblador3d

BEGIN;

DROP SCHEMA IF EXISTS amoblador3d CASCADE;
CREATE SCHEMA amoblador3d;
SET search_path TO amoblador3d;

-- Color en formato #rrggbb, como lo guarda la aplicación.
CREATE DOMAIN color_hex AS VARCHAR(7)
    CHECK (VALUE ~ '^#[0-9A-Fa-f]{6}$');

-- -----------------------------------------------------------------------------
-- Proyectos
-- -----------------------------------------------------------------------------
CREATE TABLE project (
    id             BIGINT           GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name           VARCHAR(100)     NOT NULL CHECK (btrim(name) <> ''),
    width          DOUBLE PRECISION NOT NULL CHECK (width > 0),     -- medidas interiores, en metros
    length         DOUBLE PRECISION NOT NULL CHECK (length > 0),
    height         DOUBLE PRECISION NOT NULL CHECK (height > 0),
    wall_thickness DOUBLE PRECISION NOT NULL CHECK (wall_thickness > 0),
    ambient        DOUBLE PRECISION NOT NULL CHECK (ambient BETWEEN 0 AND 2),
    created_at     TIMESTAMPTZ      NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ      NOT NULL DEFAULT now(),
    thumbnail      BYTEA                                             -- miniatura PNG
);

-- El nombre no distingue mayúsculas ("Sala" y "sala" son el mismo proyecto).
CREATE UNIQUE INDEX project_name_key ON project (lower(name));

-- -----------------------------------------------------------------------------
-- Paredes y piso
-- -----------------------------------------------------------------------------
CREATE TABLE surface_material (
    id            VARCHAR(20)  PRIMARY KEY,
    label         VARCHAR(40)  NOT NULL,
    default_color color_hex    NOT NULL,
    on_wall       BOOLEAN      NOT NULL,
    on_floor      BOOLEAN      NOT NULL,
    CHECK (on_wall OR on_floor)
);

-- Mismos valores que el enum model/SurfaceMaterial.java.
INSERT INTO surface_material (id, label, default_color, on_wall, on_floor) VALUES
    ('pintura',     'Pintura mate',        '#f2efe9', TRUE,  FALSE),
    ('satinado',    'Pintura satinada',    '#e9eef2', TRUE,  FALSE),
    ('papel-tapiz', 'Papel tapiz (rayas)', '#d8c7a8', TRUE,  FALSE),
    ('ladrillo',    'Ladrillo',            '#a4553a', TRUE,  FALSE),
    ('piedra',      'Piedra',              '#9d9488', TRUE,  TRUE),
    ('concreto',    'Concreto',            '#a7a7a2', TRUE,  TRUE),
    ('madera',      'Madera',              '#b07d4f', TRUE,  TRUE),
    ('ceramico',    'Cerámico',            '#e4e0d8', TRUE,  TRUE),
    ('marmol',      'Mármol',              '#ece9e4', TRUE,  TRUE),
    ('porcelanato', 'Porcelanato',         '#c9c3b9', FALSE, TRUE),
    ('alfombra',    'Alfombra',            '#7d8a96', FALSE, TRUE);

CREATE TABLE surface (
    project_id BIGINT      NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    id         VARCHAR(20) NOT NULL
               CHECK (id IN ('piso', 'pared-norte', 'pared-sur', 'pared-este', 'pared-oeste')),
    material   VARCHAR(20) NOT NULL REFERENCES surface_material(id),
    color      color_hex   NOT NULL,
    PRIMARY KEY (project_id, id)
);

-- -----------------------------------------------------------------------------
-- Ventanas y puertas
-- -----------------------------------------------------------------------------
CREATE TABLE opening (
    project_id  BIGINT           NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    position    INTEGER          NOT NULL CHECK (position >= 0),    -- orden en la lista
    id          VARCHAR(40)      NOT NULL,
    type        VARCHAR(10)      NOT NULL CHECK (type IN ('window', 'door')),
    wall        VARCHAR(10)      NOT NULL CHECK (wall IN ('norte', 'sur', 'este', 'oeste')),
    wall_offset DOUBLE PRECISION NOT NULL CHECK (wall_offset >= 0), -- desde la esquina izquierda de la pared
    width       DOUBLE PRECISION NOT NULL CHECK (width > 0),
    height      DOUBLE PRECISION NOT NULL CHECK (height > 0),
    sill        DOUBLE PRECISION NOT NULL CHECK (sill >= 0),        -- altura del alféizar (0 en puertas)
    PRIMARY KEY (project_id, id)
);

-- -----------------------------------------------------------------------------
-- Luces
-- -----------------------------------------------------------------------------
CREATE TABLE light (
    project_id  BIGINT           NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    position    INTEGER          NOT NULL CHECK (position >= 0),
    id          VARCHAR(40)      NOT NULL,
    type        VARCHAR(10)      NOT NULL CHECK (type IN ('point', 'spot', 'sun')),
    color       color_hex        NOT NULL,
    intensity   DOUBLE PRECISION NOT NULL CHECK (intensity >= 0),
    cast_shadow BOOLEAN          NOT NULL DEFAULT TRUE,
    -- bombilla y foco
    x DOUBLE PRECISION, y DOUBLE PRECISION, z DOUBLE PRECISION,
    -- solo foco
    target_x DOUBLE PRECISION, target_z DOUBLE PRECISION,
    angle    DOUBLE PRECISION, penumbra DOUBLE PRECISION,
    -- solo sol: entra por una ventana del mismo proyecto
    window_id VARCHAR(40), elevation DOUBLE PRECISION, deviation DOUBLE PRECISION,
    PRIMARY KEY (project_id, id),
    CHECK (type <> 'sun' OR window_id IS NOT NULL),
    FOREIGN KEY (project_id, window_id) REFERENCES opening(project_id, id) ON DELETE CASCADE
);

-- -----------------------------------------------------------------------------
-- Muebles
-- -----------------------------------------------------------------------------
CREATE TABLE furniture (
    project_id BIGINT           NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    position   INTEGER          NOT NULL CHECK (position >= 0),
    id         VARCHAR(60)      NOT NULL,
    type       VARCHAR(60)      NOT NULL,                    -- cama, mesa... o el id de un modelo
    x          DOUBLE PRECISION NOT NULL,                    -- centro del mueble
    z          DOUBLE PRECISION NOT NULL,
    rotation   DOUBLE PRECISION NOT NULL DEFAULT 0,          -- grados
    width      DOUBLE PRECISION NOT NULL CHECK (width > 0),
    depth      DOUBLE PRECISION NOT NULL CHECK (depth > 0),
    height     DOUBLE PRECISION NOT NULL CHECK (height > 0),
    color      color_hex,                                    -- NULL: colores propios del modelo
    locked     BOOLEAN          NOT NULL DEFAULT FALSE,      -- bloqueado: no se mueve
    PRIMARY KEY (project_id, id)
);

-- Modelos 3D importados por el usuario.
CREATE TABLE catalog_model (
    id         VARCHAR(60)      PRIMARY KEY,
    name       VARCHAR(100)     NOT NULL,
    file       VARCHAR(255)     NOT NULL,
    width      DOUBLE PRECISION NOT NULL CHECK (width > 0),
    depth      DOUBLE PRECISION NOT NULL CHECK (depth > 0),
    height     DOUBLE PRECISION NOT NULL CHECK (height > 0),
    rotation   DOUBLE PRECISION NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ      NOT NULL DEFAULT now()
);

-- Vistas de cámara guardadas (ver el script SQLite para el significado de cada número).
CREATE TABLE camera_view (
    project_id BIGINT           NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    position   INTEGER          NOT NULL,
    name       VARCHAR(60)      NOT NULL,
    mode       VARCHAR(4)       NOT NULL CHECK (mode IN ('3d', 'plan', 'walk')),
    x DOUBLE PRECISION NOT NULL, y DOUBLE PRECISION NOT NULL, z DOUBLE PRECISION NOT NULL,
    yaw DOUBLE PRECISION NOT NULL, pitch DOUBLE PRECISION NOT NULL, distance DOUBLE PRECISION NOT NULL,
    PRIMARY KEY (project_id, position)
);

-- Precios que el usuario cambió en el presupuesto (soles). Los referenciales están en
-- el código (estimate/PriceList.java); aquí solo van los cambiados.
CREATE TABLE price (
    kind  VARCHAR(10)   NOT NULL CHECK (kind IN ('material', 'furniture', 'extra')),
    item  VARCHAR(60)   NOT NULL,
    price NUMERIC(10,2) NOT NULL CHECK (price >= 0),
    PRIMARY KEY (kind, item)
);

-- -----------------------------------------------------------------------------
-- Reglas en triggers
-- -----------------------------------------------------------------------------

-- Cada material solo se puede usar donde corresponde (la alfombra no va en una pared).
CREATE FUNCTION check_surface_material() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    allowed BOOLEAN;
BEGIN
    SELECT CASE WHEN NEW.id = 'piso' THEN on_floor ELSE on_wall END
      INTO allowed
      FROM surface_material
     WHERE id = NEW.material;
    IF NOT allowed THEN
        RAISE EXCEPTION 'El material "%" no se puede usar en "%"', NEW.material, NEW.id;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER surface_material_allowed
    BEFORE INSERT OR UPDATE ON surface
    FOR EACH ROW EXECUTE FUNCTION check_surface_material();

-- updated_at se actualiza solo al modificar el proyecto.
CREATE FUNCTION touch_updated_at() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$;

CREATE TRIGGER project_touch
    BEFORE UPDATE ON project
    FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

-- -----------------------------------------------------------------------------
-- Vistas
-- -----------------------------------------------------------------------------

-- Lista de proyectos con su tamaño y cuántas cosas tiene.
CREATE VIEW project_summary AS
SELECT p.id,
       p.name,
       round((p.width * p.length)::numeric, 2) AS area_m2,
       (SELECT count(*) FROM opening   o WHERE o.project_id = p.id AND o.type = 'window') AS windows,
       (SELECT count(*) FROM opening   o WHERE o.project_id = p.id AND o.type = 'door')   AS doors,
       (SELECT count(*) FROM light     l WHERE l.project_id = p.id)                      AS lights,
       (SELECT count(*) FROM furniture f WHERE f.project_id = p.id)                      AS furniture,
       p.updated_at
  FROM project p;

-- Cuánto del piso ocupan los muebles en cada proyecto.
CREATE VIEW project_occupancy AS
SELECT p.id,
       p.name,
       round((p.width * p.length)::numeric, 2)                                  AS area_m2,
       round(coalesce(sum(f.width * f.depth), 0)::numeric, 2)                   AS furniture_m2,
       round((100 * coalesce(sum(f.width * f.depth), 0) / (p.width * p.length))::numeric, 1) AS occupied_pct
  FROM project p
  LEFT JOIN furniture f ON f.project_id = p.id
 GROUP BY p.id;

-- Acabado de cada pared y del piso con el nombre del material.
CREATE VIEW project_finishes AS
SELECT s.project_id,
       p.name AS project,
       s.id   AS surface,
       m.label AS material,
       s.color
  FROM surface s
  JOIN project p          ON p.id = s.project_id
  JOIN surface_material m ON m.id = s.material;

-- -----------------------------------------------------------------------------
-- Proyecto de ejemplo: dormitorio de 4 x 3.5 m (el mismo del script SQLite)
-- -----------------------------------------------------------------------------
INSERT INTO project (name, width, length, height, wall_thickness, ambient)
VALUES ('Dormitorio de ejemplo', 4.0, 3.5, 2.5, 0.15, 0.6);

INSERT INTO surface (project_id, id, material, color)
SELECT p.id, v.id, v.material, v.color
  FROM project p,
       (VALUES ('piso',        'madera',   '#b07d4f'),
               ('pared-norte', 'ladrillo', '#a4553a'),
               ('pared-sur',   'pintura',  '#f2efe9'),
               ('pared-este',  'pintura',  '#f2efe9'),
               ('pared-oeste', 'pintura',  '#f2efe9')) AS v(id, material, color)
 WHERE p.name = 'Dormitorio de ejemplo';

INSERT INTO opening (project_id, position, id, type, wall, wall_offset, width, height, sill)
SELECT p.id, v.*
  FROM project p,
       (VALUES (0, 'window-1', 'window', 'norte', 1.40, 1.2, 1.0, 0.9),
               (1, 'door-1',   'door',   'sur',   1.55, 0.9, 2.1, 0.0))
           AS v(position, id, type, wall, wall_offset, width, height, sill)
 WHERE p.name = 'Dormitorio de ejemplo';

INSERT INTO light (project_id, position, id, type, color, intensity, cast_shadow, x, y, z, window_id, elevation, deviation)
SELECT p.id, v.*
  FROM project p,
       (VALUES (0, 'point-1', 'point', '#ffe2b8', 6.0, TRUE, 2.0,  2.25, 1.75, NULL,       NULL, NULL),
               (1, 'sun-1',   'sun',   '#fff1d6', 3.0, TRUE, NULL, NULL, NULL, 'window-1', 35.0, 20.0))
           AS v(position, id, type, color, intensity, cast_shadow, x, y, z, window_id, elevation, deviation)
 WHERE p.name = 'Dormitorio de ejemplo';

INSERT INTO furniture (project_id, position, id, type, x, z, rotation, width, depth, height, color, locked)
SELECT p.id, v.*
  FROM project p,
       (VALUES (0, 'cama-1',       'cama',       2.0, 1.6, 0, 1.40, 1.9, 0.90, '#8b6b4a', TRUE),
               (1, 'escritorio-1', 'escritorio', 3.3, 1.7, 0, 1.20, 0.6, 0.75, '#d9c6a5', FALSE),
               (2, 'silla-1',      'silla',      1.0, 1.7, 0, 0.45, 0.5, 0.90, '#6d4c33', FALSE),
               (3, 'closet-1',     'closet',     3.3, 2.3, 0, 1.20, 0.6, 2.00, '#e8e2d6', FALSE))
           AS v(position, id, type, x, z, rotation, width, depth, height, color, locked)
 WHERE p.name = 'Dormitorio de ejemplo';

INSERT INTO camera_view (project_id, position, name, mode, x, y, z, yaw, pitch, distance)
SELECT p.id, 0, 'Desde la puerta', 'walk', 2.0, 1.6, 2.9, 0.0, 0.0, 0.0
  FROM project p
 WHERE p.name = 'Dormitorio de ejemplo';

INSERT INTO price (kind, item, price) VALUES ('material', 'pintura', 70.00);

COMMIT;

-- Consultas de prueba:
--   SET search_path TO amoblador3d;
--   SELECT * FROM project_summary;
--   SELECT * FROM project_occupancy;
--   SELECT * FROM project_finishes;
