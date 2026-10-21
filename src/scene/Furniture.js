// Muebles dentro de la habitación: colocación, movimiento, rotación,
// cambio de medidas y detección de colisiones (con paredes y entre muebles).
//
// Datos de un mueble (metros y grados):
//   type      id del catálogo ('cama', 'mesa', o el id de un modelo 3D)
//   x, z      centro de la base en el piso
//   rotation  giro alrededor del eje vertical, en grados
//   width, depth, height, color
import * as THREE from 'three';
import { getEntry } from '../catalog/catalog.js';
import { loadModel, fitModel } from '../catalog/models.js';
import { rectsOverlap, rectInsideRoom, clampToRoom } from './collision.js';
import { createOutline, nextId, registerId, validateFields } from './common.js';
import { createDimension, disposeDimensions } from './Dimensions.js';

const round = (v) => Math.round(v * 100) / 100; // precisión de 1 cm

export class FurnitureManager {
  constructor(scene, room) {
    this.room = room;
    this.items = [];
    this.mode = '3d';
    this.group = new THREE.Group();
    // La interfaz reemplaza estas funciones para mostrar avisos y duplicar.
    this.onWarning = () => {};
    this.onDuplicate = () => {};
    scene.add(this.group);
  }

  /** Agrega un mueble del catálogo en el primer lugar libre. Devuelve el mueble o un error. */
  add(type) {
    const entry = getEntry(type);
    const data = { id: nextId(type), type, x: 0, z: 0, rotation: 0, ...entry.defaults };
    return this.#placeNew(data, entry.name);
  }

  /** Crea una copia del mueble en el lugar libre más cercano al original. */
  duplicate(item) {
    const data = { ...item.data, id: nextId(item.data.type) };
    return this.#placeNew(data, item.entry.name, item.data);
  }

  /** Recrea un mueble guardado en un proyecto (sin buscarle lugar). */
  addFromData(data) {
    const item = new FurnitureItem(this, { ...data });
    this.items.push(item);
    return item;
  }

  remove(item) {
    this.items = this.items.filter((i) => i !== item);
    item.dispose();
  }

  clear() {
    for (const item of this.items) item.dispose();
    this.items = [];
  }

  #placeNew(data, name, near) {
    const item = new FurnitureItem(this, data);
    const spot = this.#findFreeSpot(item, near);
    if (!spot) {
      item.dispose();
      return `No hay espacio libre para ${name.toLowerCase()} (${data.width} x ${data.depth} m).`;
    }
    Object.assign(item.data, spot);
    item.place();
    this.items.push(item);
    return item;
  }

  /** Huella del mueble en planta (ver collision.js). */
  footprint(d) {
    return { x: d.x, z: d.z, hw: d.width / 2, hd: d.depth / 2, angle: THREE.MathUtils.degToRad(d.rotation) };
  }

  /** Revisa si el mueble puede estar en la posición/medidas `d`. Devuelve un error o null. */
  check(item, d) {
    const { width: W, length: L, height: H } = this.room.dims;
    if (d.height > H) return `Es más alto (${d.height} m) que la habitación (${H} m).`;
    const fp = this.footprint(d);
    if (!rectInsideRoom(fp, W, L)) return 'No cabe dentro de la habitación en esa posición.';
    for (const other of this.items) {
      if (other !== item && rectsOverlap(fp, this.footprint(other.data))) return `Choca con ${other.label}.`;
    }
    for (const { door, zone } of this.#doorZones()) {
      if (rectsOverlap(fp, zone)) return `Bloquea el paso de ${door.label}.`;
    }
    return null;
  }

  // Zona de giro de cada puerta: un cuadrado de lado = ancho de la puerta,
  // pegado a la pared por el lado interior. Ningún mueble puede invadirla.
  #doorZones() {
    const { width: W, length: L } = this.room.dims;
    return this.room.openings
      .filter((o) => o.data.type === 'door')
      .map((door) => {
        const { wall, offset, width: w } = door.data;
        const along = offset + w / 2;
        const center = { norte: [along, w / 2], sur: [along, L - w / 2], oeste: [w / 2, along], este: [W - w / 2, along] }[wall];
        return { door, zone: { x: center[0], z: center[1], hw: w / 2, hd: w / 2, angle: 0 } };
      });
  }

  /**
   * Mueve el mueble lo más cerca posible de (x, z), usado al arrastrar.
   * Si choca, intenta deslizarlo solo en X o solo en Z (así "resbala" por
   * las paredes y otros muebles en lugar de quedarse pegado).
   */
  moveTo(item, x, z) {
    const { width: W, length: L } = this.room.dims;
    const target = clampToRoom(this.footprint({ ...item.data, x: round(x), z: round(z) }), W, L);
    const tries = [
      { x: target.x, z: target.z },
      { x: target.x, z: item.data.z },
      { x: item.data.x, z: target.z },
    ];
    for (const pos of tries) {
      const candidate = { ...item.data, x: round(pos.x), z: round(pos.z) };
      if (!this.check(item, candidate)) {
        Object.assign(item.data, { x: candidate.x, z: candidate.z });
        item.place();
        return true;
      }
    }
    return false;
  }

  /** Tras cambiar las medidas de la habitación, mete los muebles dentro. Devuelve avisos. */
  fitToRoom() {
    const { width: W, length: L } = this.room.dims;
    const warnings = [];
    for (const item of this.items) {
      const fp = clampToRoom(this.footprint(item.data), W, L);
      Object.assign(item.data, { x: round(fp.x), z: round(fp.z) });
      item.place();
      const error = this.check(item, item.data);
      if (error) warnings.push(`${item.label}: ${error}`);
    }
    return warnings;
  }

  setMode(mode) {
    this.mode = mode;
    for (const item of this.items) item.updateDimensionsVisibility();
  }

  // Prueba posiciones en una cuadrícula de 10 cm, empezando por las más
  // cercanas a `near` (por defecto, el centro de la habitación).
  #findFreeSpot(item, near) {
    const { width: W, length: L } = this.room.dims;
    const origin = near ?? { x: W / 2, z: L / 2 };
    const candidates = [];
    for (let x = 0; x <= W; x += 0.1) for (let z = 0; z <= L; z += 0.1) candidates.push({ x: round(x), z: round(z) });
    candidates.sort((a, b) => Math.hypot(a.x - origin.x, a.z - origin.z) - Math.hypot(b.x - origin.x, b.z - origin.z));
    return candidates.find((pos) => !this.check(item, { ...item.data, ...pos })) ?? null;
  }
}

class FurnitureItem {
  kind = 'furniture';
  selected = false;
  buildToken = 0; // evita que un modelo que termina de cargar tarde pise una versión más nueva

  constructor(manager, data) {
    this.manager = manager;
    this.data = data;
    registerId(data.id);
    this.object = new THREE.Group();
    this.object.userData.entity = this;
    manager.group.add(this.object);
    this.#rebuild();
  }

  get entry() {
    return getEntry(this.data.type);
  }

  get label() {
    return `${this.entry.name} ${this.data.id.split('-').pop()}`;
  }

  fields() {
    const { width: W, length: L } = this.manager.room.dims;
    const lim = this.entry.limits;
    const num = (key, label, min, max, step = 0.01, unit = 'm') => ({ key, label, type: 'number', min, max, step, unit });
    const fields = [
      num('x', 'Posición X', 0, W),
      num('z', 'Posición Z', 0, L),
      num('rotation', 'Rotación', -360, 360, 1, '°'),
      num('width', 'Ancho', ...lim.width),
      num('depth', 'Profundidad', ...lim.depth),
      num('height', 'Alto', ...lim.height),
    ];
    // Los modelos 3D traen sus propios colores/texturas.
    if (this.entry.kind === 'parametric') fields.push({ key: 'color', label: 'Color', type: 'color' });
    return fields;
  }

  /** Botones extra en el panel de propiedades. */
  actions() {
    return [
      { label: '⟲ 90°', title: 'Girar a la izquierda (Shift+R)', run: () => this.rotateBy(90) },
      { label: '⟳ 90°', title: 'Girar a la derecha (R)', run: () => this.rotateBy(-90) },
      { label: 'Duplicar', title: 'Ctrl+D', run: () => this.manager.onDuplicate(this) },
    ];
  }

  values() {
    return this.data;
  }

  /** Aplica cambios si son válidos. Devuelve null o un mensaje de error. */
  update(patch) {
    const error = validateFields(this.fields(), patch);
    if (error) return error;

    const candidate = { ...this.data, ...patch };
    if ('rotation' in patch) candidate.rotation = ((candidate.rotation % 360) + 360) % 360; // 0..359
    for (const k of ['x', 'z', 'width', 'depth', 'height']) candidate[k] = round(candidate[k]);

    // Si al girar o agrandar el mueble se sale por una pared, lo empujamos hacia adentro.
    if (!('x' in patch) && !('z' in patch)) {
      const { width: W, length: L } = this.manager.room.dims;
      const fp = clampToRoom(this.manager.footprint(candidate), W, L);
      candidate.x = round(fp.x);
      candidate.z = round(fp.z);
    }

    const conflict = this.manager.check(this, candidate);
    if (conflict) return conflict;

    const needsRebuild = ['width', 'depth', 'height', 'color'].some((k) => candidate[k] !== this.data[k]);
    this.data = candidate;
    if (needsRebuild) this.#rebuild();
    else this.place();
    return null;
  }

  rotateBy(degrees) {
    const rotation = (((this.data.rotation + degrees) % 360) + 360) % 360;
    return this.update({ rotation });
  }

  setSelected(selected) {
    this.selected = selected;
    this.#rebuildSelection();
  }

  remove() {
    this.manager.remove(this);
  }

  toJSON() {
    return { ...this.data };
  }

  /** Ubica el grupo según x, z y rotación. */
  place() {
    this.object.position.set(this.data.x, 0, this.data.z);
    this.object.rotation.y = THREE.MathUtils.degToRad(this.data.rotation);
  }

  updateDimensionsVisibility() {
    if (this.heightDim) this.heightDim.visible = this.manager.mode === '3d';
  }

  dispose() {
    this.#disposeChildren();
    this.manager.group.remove(this.object);
  }

  // Vuelve a crear la geometría (cuando cambian medidas o color).
  #rebuild() {
    this.#disposeChildren();
    const entry = this.entry;
    const token = ++this.buildToken;

    if (entry.kind === 'model') {
      // Mientras el modelo carga, mostramos una caja translúcida con sus medidas.
      this.#setModel(placeholder(this.data));
      loadModel(entry.model)
        .then((scene) => {
          if (token === this.buildToken) this.#setModel(fitModel(scene, this.data, entry.rotation));
        })
        .catch((err) => this.manager.onWarning(`No se pudo cargar el modelo "${entry.name}": ${err.message ?? err}`));
    } else {
      this.#setModel(entry.build(this.data));
    }
    this.place();
    this.#rebuildSelection();
  }

  #setModel(model) {
    if (this.model) {
      disposeModel(this.model);
      this.object.remove(this.model);
    }
    this.model = model;
    this.object.add(model);
  }

  // Contorno + cotas del mueble, visibles solo cuando está seleccionado.
  #rebuildSelection() {
    if (this.selection) {
      disposeDimensions(this.selection);
      this.object.remove(this.selection);
      this.selection = null;
      this.heightDim = null;
    }
    if (!this.selected) return;

    const { width: w, depth: d, height: h } = this.data;
    const V = (x, y, z) => new THREE.Vector3(x, y, z);
    this.selection = new THREE.Group();
    const outline = createOutline(w, h, d);
    outline.position.y = h / 2;
    this.selection.add(
      outline,
      createDimension(V(-w / 2, 0.01, d / 2), V(w / 2, 0.01, d / 2), V(0, 0, 1), 0.15), // ancho
      createDimension(V(w / 2, 0.01, -d / 2), V(w / 2, 0.01, d / 2), V(1, 0, 0), 0.15), // profundidad
    );
    this.heightDim = createDimension(V(-w / 2, 0, d / 2), V(-w / 2, h, d / 2), V(-1, 0, 0), 0.15);
    this.selection.add(this.heightDim);
    this.updateDimensionsVisibility();
    this.object.add(this.selection);
  }

  #disposeChildren() {
    this.buildToken++; // si había un modelo cargando, ya no debe aparecer
    if (this.model) {
      disposeModel(this.model);
      this.object.remove(this.model);
      this.model = null;
    }
    if (this.selection) {
      disposeDimensions(this.selection);
      this.object.remove(this.selection);
      this.selection = null;
    }
  }
}

const placeholderMaterial = new THREE.MeshStandardMaterial({ color: 0x9aa3ad, transparent: true, opacity: 0.35 });

function placeholder({ width, depth, height }) {
  const g = new THREE.Group();
  const mesh = new THREE.Mesh(new THREE.BoxGeometry(width, height, depth), placeholderMaterial);
  mesh.position.y = height / 2;
  g.add(mesh);
  return g;
}

// Los paramétricos crean sus propios materiales (por el color) y hay que liberarlos.
// Los modelos 3D comparten geometría con la caché (userData.shared): esos no se liberan.
function disposeModel(model) {
  if (model.userData.shared) return;
  model.traverse((o) => {
    o.geometry?.dispose();
    if (o.material !== placeholderMaterial) o.material?.dispose();
  });
}
