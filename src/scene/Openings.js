// Aberturas (ventanas y puertas) sobre las paredes.
//
// Datos de una abertura (todo en metros):
//   wall   'norte' | 'sur' | 'este' | 'oeste'
//   offset distancia desde la esquina interior hasta el borde de la abertura.
//          En norte/sur se mide a lo largo de X (desde el oeste); en este/oeste a lo largo de Z (desde el norte).
//   width, height  tamaño del vano
//   sill   altura del alféizar (desde el piso hasta el borde inferior). En puertas es 0.
//
// El hueco real en la pared lo hace Room (agujeros en el Shape). Aquí solo
// validamos los datos y creamos el marco, vidrio u hoja de puerta.
import * as THREE from 'three';
import { createOutline, checkRange, nextId, registerId } from './common.js';

export const WALLS = [
  ['norte', 'Norte'],
  ['sur', 'Sur'],
  ['este', 'Este'],
  ['oeste', 'Oeste'],
];

const DEFAULTS = {
  window: { width: 1.2, height: 1.0, sill: 0.9 },
  door: { width: 0.9, height: 2.1, sill: 0 },
};

const MARGIN = 0.05; // separación mínima con esquinas, techo y otras aberturas

const frameMaterial = new THREE.MeshStandardMaterial({ color: 0xfafafa, roughness: 0.5 });
const glassMaterial = new THREE.MeshStandardMaterial({
  color: 0xbfe3ff,
  transparent: true,
  opacity: 0.25,
  roughness: 0.05,
  depthWrite: false,
});
const doorMaterial = new THREE.MeshStandardMaterial({ color: 0x8b5a2b, roughness: 0.6 });
const handleMaterial = new THREE.MeshStandardMaterial({ color: 0xc0c0c0, metalness: 0.8, roughness: 0.3 });
const planWindowMaterial = new THREE.MeshBasicMaterial({ color: 0x9fd3ff });
const planDoorMaterial = new THREE.MeshBasicMaterial({ color: 0xffffff });
const planLineMaterial = new THREE.LineBasicMaterial({ color: 0x333333 });

export class Opening {
  kind = 'opening';
  selected = false;

  constructor(room, data) {
    this.room = room;
    this.data = data;
    registerId(data.id);
  }

  /** Crea una abertura nueva del tipo dado en el primer lugar libre. Devuelve la abertura o un texto de error. */
  static create(room, type) {
    const data = { id: nextId(type), type, wall: 'norte', offset: 0, ...DEFAULTS[type] };
    const opening = new Opening(room, data);
    // Probamos cada pared hasta encontrar un hueco donde quepa
    // (las puertas empiezan por el sur y las ventanas por el norte).
    const order = type === 'door' ? ['sur', 'oeste', 'este', 'norte'] : ['norte', 'este', 'oeste', 'sur'];
    for (const wall of order) {
      const offset = findFreeOffset({ ...data, wall }, room, opening);
      if (offset !== null) {
        Object.assign(data, { wall, offset });
        return opening;
      }
    }
    return `No hay espacio libre en las paredes para otra ${type === 'door' ? 'puerta' : 'ventana'}.`;
  }

  get label() {
    const name = this.data.type === 'door' ? 'Puerta' : 'Ventana';
    return `${name} ${this.data.id.split('-').pop()} (${this.data.wall})`;
  }

  /** Campos que muestra el panel de propiedades. */
  fields() {
    const len = this.room.wallLength(this.data.wall);
    const fields = [
      { key: 'wall', label: 'Pared', type: 'select', options: WALLS },
      { key: 'offset', label: 'Posición desde esquina', type: 'number', unit: 'm', step: 0.01, min: 0, max: len },
      { key: 'width', label: 'Ancho', type: 'number', unit: 'm', step: 0.01, min: 0.3, max: len },
      { key: 'height', label: 'Alto', type: 'number', unit: 'm', step: 0.01, min: 0.3, max: this.room.dims.height },
    ];
    if (this.data.type === 'window') {
      fields.push({ key: 'sill', label: 'Altura alféizar', type: 'number', unit: 'm', step: 0.01, min: 0, max: this.room.dims.height });
    }
    return fields;
  }

  values() {
    return this.data;
  }

  /** Aplica cambios si son válidos. Devuelve null o un mensaje de error. */
  update(patch) {
    const candidate = { ...this.data, ...patch };
    // Al cambiar de pared, centramos la abertura en la nueva pared.
    if (patch.wall && patch.wall !== this.data.wall) {
      const offset = findFreeOffset(candidate, this.room, this);
      if (offset === null) return `No hay espacio libre en la pared ${patch.wall}.`;
      candidate.offset = offset;
    }
    const error = validateOpening(candidate, this.room, this);
    if (error) return error;
    this.data = candidate;
    this.room.rebuildWalls();
    return null;
  }

  setSelected(selected) {
    this.selected = selected;
    if (this.outline) this.outline.visible = selected;
  }

  remove() {
    this.room.removeOpening(this);
  }

  toJSON() {
    return { ...this.data };
  }

  /**
   * Construye el marco/vidrio/hoja en coordenadas LOCALES de la pared:
   *   x a lo largo de la pared, y hacia arriba, z atraviesa el grosor (0..t).
   * @param {object} wall { thickness, height, localStart, interiorZ }
   * @returns {{ group: THREE.Group, plan: THREE.Group }}
   */
  buildVisual(wall) {
    const { type, offset, width, height, sill } = this.data;
    const t = wall.thickness;
    const x0 = wall.localStart + offset;
    const cx = x0 + width / 2;
    const cz = t / 2;
    // s = +1 o -1: hacia qué lado (en z local) queda el interior de la habitación.
    const s = wall.interiorZ === 0 ? -1 : 1;
    const fw = 0.05; // ancho de los perfiles del marco

    const group = new THREE.Group();
    const add = (w, h, d, x, y, z, material, castShadow = true) => {
      const mesh = new THREE.Mesh(new THREE.BoxGeometry(w, h, d), material);
      mesh.position.set(x, y, z);
      mesh.castShadow = castShadow;
      mesh.receiveShadow = true;
      group.add(mesh);
    };

    if (type === 'window') {
      const cy = sill + height / 2;
      add(width, fw, 0.07, cx, sill + height - fw / 2, cz, frameMaterial); // dintel
      add(width, fw, 0.07, cx, sill + fw / 2, cz, frameMaterial); // base
      add(fw, height - 2 * fw, 0.07, x0 + fw / 2, cy, cz, frameMaterial); // lateral izq.
      add(fw, height - 2 * fw, 0.07, x0 + width - fw / 2, cy, cz, frameMaterial); // lateral der.
      if (width > 1) add(0.04, height - 2 * fw, 0.07, cx, cy, cz, frameMaterial); // parteluz central
      add(width - 2 * fw, height - 2 * fw, 0.01, cx, cy, cz, glassMaterial, false); // vidrio (deja pasar la luz)
      add(width + 0.1, 0.03, 0.12, cx, sill - 0.015, wall.interiorZ + s * 0.04, frameMaterial); // repisa interior
    } else {
      const leafZ = wall.interiorZ - s * 0.03; // la hoja queda cerca de la cara interior
      add(fw, height, t + 0.02, x0 + fw / 2, height / 2, cz, frameMaterial); // jamba izq.
      add(fw, height, t + 0.02, x0 + width - fw / 2, height / 2, cz, frameMaterial); // jamba der.
      add(width, fw, t + 0.02, cx, height - fw / 2, cz, frameMaterial); // cabezal
      add(width - 2 * fw, height - fw - 0.01, 0.04, cx, (height - fw + 0.01) / 2, leafZ, doorMaterial); // hoja
      add(0.12, 0.02, 0.02, x0 + width - fw - 0.1, 1.0, leafZ + s * 0.035, handleMaterial); // manija interior
      add(0.12, 0.02, 0.02, x0 + width - fw - 0.1, 1.0, leafZ - s * 0.035, handleMaterial); // manija exterior
      add(width, 0.01, t, cx, 0.005, cz, frameMaterial); // umbral (cubre el piso dentro del muro)
    }

    this.outline = createOutline(width + 0.04, height + 0.04, t + 0.06);
    this.outline.position.set(cx, sill + height / 2, cz);
    this.outline.visible = this.selected;
    group.add(this.outline);

    // Símbolo para la vista de planta: se dibuja justo encima del muro.
    const plan = new THREE.Group();
    plan.userData.planOnly = true;
    const y = wall.height + 0.01;
    const fill = new THREE.Mesh(new THREE.PlaneGeometry(width, t), type === 'door' ? planDoorMaterial : planWindowMaterial);
    fill.rotation.x = -Math.PI / 2; // acostar el plano
    fill.position.set(cx, y, cz);
    plan.add(fill);
    if (type === 'door') {
      // Hoja abierta 90° y arco de giro, como en los planos de arquitectura.
      // Bisagra -> punta de la hoja abierta -> arco hasta la posición cerrada.
      const points = [new THREE.Vector3(x0, y, wall.interiorZ)];
      for (let i = 0; i <= 24; i++) {
        const a = (Math.PI / 2) * (i / 24);
        points.push(new THREE.Vector3(x0 + width * Math.sin(a), y, wall.interiorZ + s * width * Math.cos(a)));
      }
      plan.add(new THREE.Line(new THREE.BufferGeometry().setFromPoints(points), planLineMaterial));
    } else {
      const line = [new THREE.Vector3(x0, y + 0.001, cz), new THREE.Vector3(x0 + width, y + 0.001, cz)];
      plan.add(new THREE.Line(new THREE.BufferGeometry().setFromPoints(line), planLineMaterial));
    }

    // Para que al hacer clic sobre cualquier pieza sepamos a qué abertura pertenece.
    group.userData.entity = this;
    plan.userData.entity = this;
    return { group, plan };
  }
}

/** Revisa que la abertura quepa en su pared y no choque con otras. Devuelve un error o null. */
export function validateOpening(o, room, self) {
  const len = room.wallLength(o.wall);
  const H = room.dims.height;
  const name = o.type === 'door' ? 'La puerta' : 'La ventana';

  const err =
    checkRange('El ancho', o.width, 0.3, len - 2 * MARGIN) ||
    checkRange('La posición', o.offset, MARGIN, len - o.width - MARGIN) ||
    (o.type === 'window'
      ? checkRange('El alféizar', o.sill, MARGIN, H - o.height - MARGIN) || checkRange('El alto', o.height, 0.3, H - o.sill - MARGIN)
      : checkRange('El alto', o.height, 1.5, H - MARGIN));
  if (err) return err;
  if (o.type === 'door' && o.sill !== 0) return 'Las puertas siempre empiezan en el piso.';

  for (const other of room.openings) {
    if (other === self || other.data.wall !== o.wall) continue;
    const p = other.data;
    const separated = o.offset >= p.offset + p.width + MARGIN || o.offset + o.width <= p.offset - MARGIN;
    if (!separated) return `${name} se superpone con ${other.label}.`;
  }
  return null;
}

/** Busca una posición libre en la pared (primero el centro). Devuelve offset o null. */
function findFreeOffset(o, room, self) {
  const len = room.wallLength(o.wall);
  const centered = Math.round(((len - o.width) / 2) * 100) / 100;
  const candidates = [centered];
  for (let off = MARGIN; off <= len - o.width - MARGIN + 1e-9; off += 0.05) candidates.push(Math.round(off * 100) / 100);
  return candidates.find((offset) => !validateOpening({ ...o, offset }, room, self)) ?? null;
}
