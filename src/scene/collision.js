// Colisiones en planta (2D, plano XZ). Cada mueble se representa por su "huella":
// un rectángulo orientado { x, z, hw, hd, angle }
//   x, z   = centro en metros
//   hw, hd = mitad del ancho y mitad de la profundidad
//   angle  = rotación alrededor del eje Y en radianes
//
// Para saber si dos rectángulos rotados se tocan usamos el Teorema del Eje
// Separador (SAT): si existe un eje donde sus proyecciones no se solapan, no chocan.
// En rectángulos basta con probar los 2 ejes de cada uno (4 en total).

const EPS = 1e-6;

/** Ejes locales del rectángulo en coordenadas del mundo (igual que rotation.y de Three.js). */
function axes(r) {
  const c = Math.cos(r.angle);
  const s = Math.sin(r.angle);
  return [
    { x: c, z: -s }, // eje X local (ancho)
    { x: s, z: c }, // eje Z local (profundidad)
  ];
}

/** "Radio" del rectángulo proyectado sobre un eje n. */
function projectedRadius(r, n) {
  const [ax, az] = axes(r);
  return r.hw * Math.abs(ax.x * n.x + ax.z * n.z) + r.hd * Math.abs(az.x * n.x + az.z * n.z);
}

/** true si los rectángulos se superponen (tocarse justo en el borde NO cuenta). */
export function rectsOverlap(a, b) {
  const d = { x: b.x - a.x, z: b.z - a.z };
  for (const n of [...axes(a), ...axes(b)]) {
    const distance = Math.abs(d.x * n.x + d.z * n.z);
    if (distance >= projectedRadius(a, n) + projectedRadius(b, n) - EPS) return false; // eje separador
  }
  return true;
}

/** Mitad del ancho/largo de la caja alineada a los ejes que envuelve el rectángulo rotado. */
export function halfExtents(r) {
  const c = Math.abs(Math.cos(r.angle));
  const s = Math.abs(Math.sin(r.angle));
  return { hx: r.hw * c + r.hd * s, hz: r.hw * s + r.hd * c };
}

/** true si el rectángulo cabe completo dentro de la habitación [0,width] x [0,length]. */
export function rectInsideRoom(r, width, length) {
  const { hx, hz } = halfExtents(r);
  return r.x - hx >= -EPS && r.x + hx <= width + EPS && r.z - hz >= -EPS && r.z + hz <= length + EPS;
}

/** Ajusta el centro para que el rectángulo quede dentro de la habitación (si es posible). */
export function clampToRoom(r, width, length) {
  const { hx, hz } = halfExtents(r);
  const clamp = (v, min, max) => (min > max ? (min + max) / 2 : Math.min(Math.max(v, min), max));
  return { ...r, x: clamp(r.x, hx, width - hx), z: clamp(r.z, hz, length - hz) };
}
