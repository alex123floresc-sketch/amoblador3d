// Utilidades compartidas por aberturas, luces y muebles.
import * as THREE from 'three';

export const SELECT_COLOR = 0x4f9cf9;

const outlineMaterial = new THREE.LineBasicMaterial({ color: SELECT_COLOR, depthTest: false, transparent: true });

/**
 * Contorno de una caja (w x h x d) para resaltar lo seleccionado.
 * Se dibuja encima de todo (depthTest: false) para que siempre se vea.
 */
export function createOutline(w, h, d) {
  const geometry = new THREE.EdgesGeometry(new THREE.BoxGeometry(w, h, d));
  const outline = new THREE.LineSegments(geometry, outlineMaterial);
  outline.renderOrder = 1000;
  return outline;
}

/** Genera ids únicos cortos: "mueble-1", "mueble-2"... */
const counters = {};
export function nextId(prefix) {
  counters[prefix] = (counters[prefix] ?? 0) + 1;
  return `${prefix}-${counters[prefix]}`;
}

/** Al cargar un proyecto, avisa qué ids ya existen para no repetirlos después. */
export function registerId(id) {
  const match = /^(.*)-(\d+)$/.exec(id);
  if (match) counters[match[1]] = Math.max(counters[match[1]] ?? 0, Number(match[2]));
}

/** Libera geometrías de un objeto y sus hijos (los materiales son compartidos). */
export function disposeObject(obj) {
  obj.traverse((o) => {
    o.geometry?.dispose();
    if (o.isCSS2DObject) o.element.remove();
  });
}

/** Convierte un valor a número y verifica que esté en [min, max]. Devuelve un error o null. */
export function checkRange(label, value, min, max) {
  if (!Number.isFinite(value) || value < min - 1e-9 || value > max + 1e-9) {
    return `${label} debe estar entre ${fmt(min)} y ${fmt(max)}.`;
  }
  return null;
}

const fmt = (v) => v.toLocaleString('es-PE', { maximumFractionDigits: 2 });

/** Valida los campos numéricos de `patch` usando los min/max declarados en `fields`. */
export function validateFields(fields, patch) {
  for (const f of fields) {
    if (f.type === 'number' && f.key in patch) {
      const error = checkRange(f.label, patch[f.key], f.min, f.max);
      if (error) return error;
    }
  }
  return null;
}
