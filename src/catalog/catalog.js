// Registro central del catálogo: une los muebles paramétricos (creados por
// código) con los modelos 3D (.glb/.gltf) declarados en los catalog.json.
//
// Cada entrada tiene:
//   id, name, kind: 'parametric' | 'model' | 'missing'
//   defaults: { width, depth, height, color? }   medidas iniciales en metros
//   limits:   { width: [min, max], ... }
//   build(data)  (paramétricos)   o   model: URL amob://...  (modelos 3D)
import * as THREE from 'three';
import { PARAMETRIC } from './parametric.js';

// Los modelos importados pueden tener cualquier tamaño razonable.
const MODEL_LIMITS = { width: [0.05, 6], depth: [0.05, 6], height: [0.05, 4] };

const entries = new Map();
for (const [id, entry] of Object.entries(PARAMETRIC)) entries.set(id, { ...entry, id, kind: 'parametric' });

/** Devuelve la entrada del catálogo; si ya no existe, una caja roja de reemplazo. */
export function getEntry(type) {
  return entries.get(type) ?? missingEntry(type);
}

export function listEntries(kind) {
  return [...entries.values()].filter((e) => e.kind === kind);
}

/** Reemplaza los modelos 3D por los que vienen de los catalog.json. */
export function setModelEntries(models) {
  for (const [id, e] of entries) if (e.kind === 'model') entries.delete(id);
  for (const m of models) {
    entries.set(m.id, {
      id: m.id,
      kind: 'model',
      name: m.name,
      source: m.source, // 'builtin' o 'user'
      model: m.url,
      rotation: m.rotation ?? 0, // giro para que el frente del modelo mire a +Z
      defaults: { width: m.width, depth: m.depth, height: m.height },
      limits: MODEL_LIMITS,
    });
  }
}

const missingMaterial = new THREE.MeshStandardMaterial({ color: 0xd9534f, transparent: true, opacity: 0.5 });

// Si un proyecto usa un modelo que ya no está en el catálogo, lo mostramos
// como una caja roja con sus medidas para no perder la distribución.
function missingEntry(type) {
  return {
    id: type,
    kind: 'missing',
    name: `Falta "${type}"`,
    defaults: { width: 1, depth: 1, height: 1 },
    limits: MODEL_LIMITS,
    build: ({ width, depth, height }) => {
      const g = new THREE.Group();
      const mesh = new THREE.Mesh(new THREE.BoxGeometry(width, height, depth), missingMaterial);
      mesh.position.y = height / 2;
      g.add(mesh);
      g.userData.shared = true; // el material es compartido: no liberarlo
      return g;
    },
  };
}
