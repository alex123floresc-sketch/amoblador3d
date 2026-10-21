// Convertir el proyecto completo a JSON (para guardar) y reconstruirlo desde JSON (al abrir).
//
// Formato del archivo .amob3d (es JSON normal, se puede abrir con un editor de texto):
// {
//   "app": "Amoblador3D", "version": 1,
//   "room":      { "width": 4, "length": 3.5, "height": 2.5, "wallThickness": 0.15 },
//   "openings":  [ { "id", "type": "window"|"door", "wall", "offset", "width", "height", "sill" } ],
//   "ambient":   0.6,
//   "lights":    [ { "id", "type": "point"|"spot"|"sun", "color", "intensity", ... } ],
//   "furniture": [ { "id", "type", "x", "z", "rotation", "width", "depth", "height", "color" } ]
// }

export const PROJECT_VERSION = 1;

/** Toma una "foto" del estado actual como objeto plano (sin objetos de Three.js). */
export function serializeProject({ room, lights, furniture }) {
  return {
    app: 'Amoblador3D',
    version: PROJECT_VERSION,
    room: { ...room.dims },
    openings: room.openings.map((o) => o.toJSON()),
    ambient: lights.ambient.intensity,
    lights: lights.items.map((l) => l.toJSON()),
    furniture: furniture.items.map((f) => f.toJSON()),
  };
}

/** Revisa que el archivo tenga la forma esperada. Lanza un Error con un mensaje claro si no. */
export function validateProject(data) {
  if (!data || data.app !== 'Amoblador3D') throw new Error('El archivo no es un proyecto de Amoblador3D.');
  if (data.version > PROJECT_VERSION) throw new Error('El proyecto fue creado con una versión más nueva de la aplicación.');
  const r = data.room ?? {};
  const ok = (v, min, max) => typeof v === 'number' && v >= min && v <= max;
  if (!ok(r.width, 1, 30) || !ok(r.length, 1, 30) || !ok(r.height, 2, 6) || !ok(r.wallThickness, 0.05, 0.5)) {
    throw new Error('Las medidas de la habitación del proyecto no son válidas.');
  }
  for (const key of ['openings', 'lights', 'furniture']) {
    if (data[key] !== undefined && !Array.isArray(data[key])) throw new Error(`"${key}" debe ser una lista.`);
  }
}

/**
 * Reemplaza todo el contenido de la escena por el del proyecto.
 * @returns {string[]} avisos (por ejemplo, ventanas que no cabían)
 */
export function applyProject(data, { room, lights, furniture }) {
  validateProject(data);
  furniture.clear();
  lights.clear();
  const warnings = room.load(data.room, data.openings ?? []);
  lights.setAmbient(data.ambient ?? 0.6);
  for (const light of data.lights ?? []) lights.addFromData(light);
  warnings.push(...lights.refresh());
  for (const item of data.furniture ?? []) furniture.addFromData(item);
  return warnings;
}
