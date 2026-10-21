// Iluminación: luz ambiental + luces que el usuario agrega.
//
// Tipos de luz:
//   point -> Bombilla (PointLight): ilumina en todas direcciones desde un punto.
//   spot  -> Foco (SpotLight): cono de luz que apunta a un punto del piso.
//   sun   -> Sol por ventana (DirectionalLight): rayos paralelos que entran
//            por una ventana. Como las paredes y el techo proyectan sombra,
//            la luz solo pasa por el hueco de la ventana.
//
// Las intensidades usan las unidades físicas de Three.js:
// candelas (cd) para bombillas/focos y lux (lx) para el sol.
import * as THREE from 'three';
import { createOutline, disposeObject, nextId, registerId, validateFields } from './common.js';

const TYPES = {
  point: { name: 'Bombilla', color: '#ffe2b8', intensity: 6 },
  spot: { name: 'Foco', color: '#fff4e0', intensity: 25 },
  sun: { name: 'Sol', color: '#fff1d6', intensity: 3 },
};

const deg = THREE.MathUtils.degToRad;

export class LightManager {
  constructor(scene, room) {
    this.room = room;
    this.items = [];
    this.group = new THREE.Group();
    // Luz ambiental: cielo blanco arriba y un tono cálido que "rebota" del piso.
    this.ambient = new THREE.HemisphereLight(0xffffff, 0x8a7a66, 0.6);
    scene.add(this.ambient, this.group);
  }

  /** Agrega una luz. Devuelve la luz creada o un mensaje de error. */
  add(type) {
    const { width: W, length: L, height: H } = this.room.dims;
    const data = { id: nextId(type), type, color: TYPES[type].color, intensity: TYPES[type].intensity, castShadow: true };

    if (type === 'point') Object.assign(data, { x: W / 2, y: H - 0.25, z: L / 2 });
    if (type === 'spot') Object.assign(data, { x: W * 0.25, y: H - 0.05, z: L * 0.3, targetX: W * 0.25, targetZ: L * 0.3, angle: 30, penumbra: 0.4 });
    if (type === 'sun') {
      const win = this.room.openings.find((o) => o.data.type === 'window');
      if (!win) return 'Agrega primero una ventana: la luz del sol entra por ella.';
      Object.assign(data, { windowId: win.data.id, elevation: 35, deviation: 20 });
    }
    round(data);

    const item = new LightItem(this, data);
    this.items.push(item);
    return item;
  }

  /** Recrea una luz guardada en un proyecto. */
  addFromData(data) {
    const item = new LightItem(this, { ...data });
    this.items.push(item);
    return item;
  }

  remove(item) {
    this.items = this.items.filter((i) => i !== item);
    disposeObject(item.object);
    this.group.remove(item.object);
  }

  clear() {
    for (const item of [...this.items]) this.remove(item);
  }

  /** Recalcula las luces cuando cambian las ventanas. Devuelve avisos. */
  refresh() {
    const warnings = [];
    for (const item of [...this.items]) {
      if (item.data.type !== 'sun') continue;
      const windows = this.room.openings.filter((o) => o.data.type === 'window');
      if (!windows.some((w) => w.data.id === item.data.windowId)) {
        if (windows.length === 0) {
          warnings.push(`${item.label} se eliminó porque ya no hay ventanas.`);
          this.remove(item);
          continue;
        }
        item.data.windowId = windows[0].data.id;
      }
      item.apply();
    }
    return warnings;
  }

  /** Tras cambiar las medidas de la habitación, mete las luces dentro. */
  fitToRoom() {
    const { width: W, length: L, height: H } = this.room.dims;
    const clamp = (v, max) => Math.min(Math.max(v, 0.05), max - 0.05);
    for (const item of this.items) {
      const d = item.data;
      if (d.type === 'sun') continue;
      Object.assign(d, { x: clamp(d.x, W), y: clamp(d.y, H), z: clamp(d.z, L) });
      if (d.type === 'spot') Object.assign(d, { targetX: clamp(d.targetX, W), targetZ: clamp(d.targetZ, L) });
      round(d);
    }
    return this.refresh();
  }

  setAmbient(intensity) {
    this.ambient.intensity = intensity;
  }
}

class LightItem {
  kind = 'light';
  selected = false;

  constructor(manager, data) {
    this.manager = manager;
    this.room = manager.room;
    this.data = data;
    registerId(data.id);
    this.object = new THREE.Group();
    this.object.userData.entity = this; // para saber qué luz se clicó
    manager.group.add(this.object);
    this.#build();
    this.apply();
  }

  get label() {
    return `${TYPES[this.data.type].name} ${this.data.id.split('-').pop()}`;
  }

  fields() {
    const { width: W, length: L, height: H } = this.room.dims;
    const { type } = this.data;
    const num = (key, label, min, max, step = 0.01, unit = 'm') => ({ key, label, type: 'number', min, max, step, unit });
    const fields = [
      { key: 'color', label: 'Color', type: 'color' },
      num('intensity', 'Intensidad', 0, type === 'sun' ? 20 : 500, 0.5, type === 'sun' ? 'lx' : 'cd'),
      { key: 'castShadow', label: 'Proyectar sombras', type: 'checkbox' },
    ];
    if (type !== 'sun') fields.push(num('x', 'Posición X', 0.05, W - 0.05), num('y', 'Altura (Y)', 0.05, H - 0.05), num('z', 'Posición Z', 0.05, L - 0.05));
    if (type === 'spot') {
      fields.push(
        num('targetX', 'Apunta a X', 0, W),
        num('targetZ', 'Apunta a Z', 0, L),
        num('angle', 'Apertura', 5, 80, 1, '°'),
        num('penumbra', 'Suavidad del borde', 0, 1, 0.05, ''),
      );
    }
    if (type === 'sun') {
      const windows = this.room.openings.filter((o) => o.data.type === 'window');
      fields.push(
        { key: 'windowId', label: 'Entra por', type: 'select', options: windows.map((w) => [w.data.id, w.label]) },
        num('elevation', 'Altura del sol', 5, 85, 1, '°'),
        num('deviation', 'Desvío lateral', -60, 60, 1, '°'),
      );
    }
    return fields;
  }

  values() {
    return this.data;
  }

  update(patch) {
    const error = validateFields(this.fields(), patch);
    if (error) return error;
    Object.assign(this.data, patch);
    this.apply();
    return null;
  }

  setSelected(selected) {
    this.selected = selected;
    this.outline.visible = selected;
    if (this.aimLine) this.aimLine.visible = selected;
  }

  remove() {
    this.manager.remove(this);
  }

  toJSON() {
    return { ...this.data };
  }

  // Crea la luz de Three.js y un pequeño objeto visible para poder seleccionarla.
  #build() {
    const { type } = this.data;
    this.helperMaterial = new THREE.MeshBasicMaterial({ color: this.data.color });

    if (type === 'point') {
      this.light = new THREE.PointLight(0xffffff, 1, 0, 2); // distancia 0 = sin límite; decay 2 = físico
      this.light.shadow.camera.near = 0.05;
      this.helper = new THREE.Mesh(new THREE.SphereGeometry(0.06, 16, 12), this.helperMaterial);
    }

    if (type === 'spot') {
      this.light = new THREE.SpotLight(0xffffff, 1, 0, deg(30), 0.4, 2);
      this.light.shadow.camera.near = 0.05;
      const cone = new THREE.ConeGeometry(0.07, 0.14, 20, 1, true);
      cone.rotateX(-Math.PI / 2); // la boca del cono mira hacia +Z (lookAt apunta +Z al objetivo)
      this.helper = new THREE.Mesh(cone, new THREE.MeshBasicMaterial({ color: this.data.color, side: THREE.DoubleSide }));
      this.helperMaterial = this.helper.material;
      this.aimLine = new THREE.Line(new THREE.BufferGeometry(), new THREE.LineDashedMaterial({ color: 0xffd166, dashSize: 0.08, gapSize: 0.05 }));
      this.aimLine.visible = false;
      this.object.add(this.aimLine);
    }

    if (type === 'sun') {
      this.light = new THREE.DirectionalLight(0xffffff, 1);
      // Flecha amarilla fuera de la ventana que indica la dirección de los rayos.
      this.helper = new THREE.ArrowHelper(new THREE.Vector3(0, -1, 0), new THREE.Vector3(), 1, 0xffc845, 0.25, 0.15);
    }

    this.light.shadow.mapSize.set(1024, 1024);
    this.light.shadow.bias = -0.0005;
    this.light.shadow.normalBias = 0.02;
    this.helper.traverse((o) => (o.castShadow = false)); // el ayudante no debe tapar su propia luz

    this.outline = createOutline(0.22, 0.22, 0.22);
    this.outline.visible = false;
    this.object.add(this.light, this.helper, this.outline);
    // Focos y sol apuntan a un "target" que debe estar en la escena; las bombillas no tienen.
    if (this.light.target) this.object.add(this.light.target);
  }

  /** Copia los datos (color, intensidad, posición...) a los objetos de Three.js. */
  apply() {
    const d = this.data;
    this.light.color.set(d.color);
    this.light.intensity = d.intensity;
    this.light.castShadow = d.castShadow;

    if (d.type === 'point' || d.type === 'spot') {
      this.light.position.set(d.x, d.y, d.z);
      this.helper.position.copy(this.light.position);
      this.outline.position.copy(this.light.position);
      this.helperMaterial.color.set(d.color);
    }

    if (d.type === 'spot') {
      this.light.target.position.set(d.targetX, 0, d.targetZ);
      this.light.angle = deg(d.angle);
      this.light.penumbra = d.penumbra;
      this.helper.lookAt(this.light.target.position);
      this.aimLine.geometry.setFromPoints([this.light.position, this.light.target.position]);
      this.aimLine.computeLineDistances(); // necesario para la línea punteada
    }

    if (d.type === 'sun') this.#applySun();
  }

  #applySun() {
    const d = this.data;
    const win = this.room.openings.find((o) => o.data.id === d.windowId);
    if (!win) return;
    const { width: W, length: L, height: H } = this.room.dims;

    // Dirección de los rayos: hacia el interior (normal de la pared), girada
    // "deviation" grados a un lado y bajando según la altura del sol.
    const horizontal = this.room.interiorNormal(win.data.wall).applyAxisAngle(new THREE.Vector3(0, 1, 0), deg(d.deviation));
    const e = deg(d.elevation);
    const dir = horizontal.multiplyScalar(Math.cos(e)).add(new THREE.Vector3(0, -Math.sin(e), 0)).normalize();

    // Una luz direccional solo necesita dirección: la ubicamos lejos, apuntando al centro.
    const center = new THREE.Vector3(W / 2, 0, L / 2);
    this.light.target.position.copy(center);
    this.light.position.copy(center).addScaledVector(dir, -20);

    // La "cámara de sombras" debe cubrir toda la habitación.
    const half = Math.hypot(W, L, H) / 2 + 1;
    Object.assign(this.light.shadow.camera, { left: -half, right: half, top: half, bottom: -half, near: 1, far: 45 });
    this.light.shadow.camera.updateProjectionMatrix();
    this.light.shadow.mapSize.set(2048, 2048);

    // Flecha: termina en el centro de la ventana.
    const windowCenter = this.room.openingCenter(win);
    this.helper.position.copy(windowCenter).addScaledVector(dir, -1.1);
    this.helper.setDirection(dir);
    this.helper.setColor(d.color);
    this.outline.position.copy(this.helper.position);
  }
}

function round(data) {
  for (const [k, v] of Object.entries(data)) if (typeof v === 'number') data[k] = Math.round(v * 100) / 100;
}
