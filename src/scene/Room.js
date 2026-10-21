// Room: genera el piso, las 4 paredes (con huecos para puertas y ventanas) y las cotas.
//
// Sistema de coordenadas (en metros):
//   - El interior de la habitación va de x = 0 a ancho, z = 0 a largo, y = 0 a alto.
//   - El origen (0,0,0) es la esquina interior noroeste, a nivel del piso.
//   - Las paredes se construyen HACIA AFUERA, así las medidas ingresadas son
//     las medidas libres interiores (lo que importa para amoblar).
import * as THREE from 'three';
import { createDimension, disposeDimensions } from './Dimensions.js';
import { Opening, validateOpening } from './Openings.js';
import { disposeObject } from './common.js';

export const DEFAULT_ROOM = { width: 4, length: 3.5, height: 2.5, wallThickness: 0.15 };

const wallMaterial = new THREE.MeshStandardMaterial({ color: 0xf2efe9, roughness: 0.9 });
const floorMaterial = new THREE.MeshStandardMaterial({ color: 0xc49a6c, roughness: 0.8 });
const edgeMaterial = new THREE.LineBasicMaterial({ color: 0x555555 });
const gridMaterial = new THREE.LineBasicMaterial({ color: 0x000000, transparent: true, opacity: 0.12 });

// Material "fantasma": no se dibuja (ni color ni profundidad), pero el objeto
// SÍ sigue proyectando sombras. Lo usamos en el techo y en las paredes que se
// ocultan en 3D: así la luz del sol solo entra por las ventanas.
const ghostMaterial = new THREE.MeshBasicMaterial({ colorWrite: false, depthWrite: false, side: THREE.DoubleSide });

export class Room {
  constructor(scene) {
    this.scene = scene;
    this.group = new THREE.Group(); // piso + techo
    this.wallGroup = new THREE.Group(); // paredes (se reconstruyen al cambiar aberturas)
    this.dimensions = new THREE.Group(); // cotas
    this.walls = [];
    this.openings = [];
    this.listeners = [];
    scene.add(this.group, this.wallGroup, this.dimensions);
    this.build(DEFAULT_ROOM);
  }

  /** Registra una función que se llama cuando la habitación o sus aberturas cambian. */
  onChange(fn) {
    this.listeners.push(fn);
  }

  /**
   * (Re)construye la habitación con nuevas medidas.
   * @returns {string[]} avisos (por ejemplo, aberturas que ya no caben y se eliminaron)
   */
  build({ width, length, height, wallThickness }) {
    this.dims = { width, length, height, wallThickness };
    const warnings = this.#fitOpenings();

    disposeObject(this.group);
    this.group.clear();
    this.#createFloor(width, length);
    this.#createCeiling();

    disposeDimensions(this.dimensions);
    this.dimensions.clear();
    this.#createDimensions();

    this.rebuildWalls();
    return warnings;
  }

  /**
   * Carga medidas y aberturas guardadas en un proyecto.
   * @returns {string[]} avisos
   */
  load(dims, openings = []) {
    this.openings = openings.map((data) => new Opening(this, { ...data }));
    return this.build(dims);
  }

  /** Largo interior de una pared. */
  wallLength(wall) {
    return wall === 'norte' || wall === 'sur' ? this.dims.width : this.dims.length;
  }

  /** Vector unitario que apunta desde la pared hacia el interior. */
  interiorNormal(wall) {
    return { norte: new THREE.Vector3(0, 0, 1), sur: new THREE.Vector3(0, 0, -1), oeste: new THREE.Vector3(1, 0, 0), este: new THREE.Vector3(-1, 0, 0) }[wall];
  }

  /** Centro de una abertura en coordenadas del mundo (en medio del grosor del muro). */
  openingCenter(opening) {
    const { wall, offset, width, height, sill } = opening.data;
    const { width: W, length: L, wallThickness: t } = this.dims;
    const along = offset + width / 2;
    const y = sill + height / 2;
    return {
      norte: new THREE.Vector3(along, y, -t / 2),
      sur: new THREE.Vector3(along, y, L + t / 2),
      oeste: new THREE.Vector3(-t / 2, y, along),
      este: new THREE.Vector3(W + t / 2, y, along),
    }[wall];
  }

  /** Agrega una ventana o puerta. Devuelve la abertura o un mensaje de error. */
  addOpening(type) {
    const result = Opening.create(this, type);
    if (typeof result === 'string') return result;
    this.openings.push(result);
    this.rebuildWalls();
    return result;
  }

  removeOpening(opening) {
    this.openings = this.openings.filter((o) => o !== opening);
    this.rebuildWalls();
  }

  /** Caja que envuelve la habitación completa (incluye el grosor de muros). */
  getBounds() {
    const { width, length, height, wallThickness: t } = this.dims;
    return new THREE.Box3(new THREE.Vector3(-t, 0, -t), new THREE.Vector3(width + t, height, length + t));
  }

  /**
   * Se llama en cada frame. En 3D vuelve "fantasma" las paredes que están entre
   * la cámara y el interior (efecto casa de muñecas) para poder ver dentro.
   */
  update(camera, mode) {
    for (const wall of this.walls) {
      const { pointInside, normal } = wall.userData;
      // Si la cámara está del lado exterior de la pared, el producto punto es negativo.
      const show = mode === 'plan' || camera.position.clone().sub(pointInside).dot(normal) > 0;
      wall.material = show ? wallMaterial : ghostMaterial;
      for (const child of wall.children) {
        child.visible = child.userData.planOnly ? mode === 'plan' : show;
      }
    }
    // En planta la cota de altura no tiene sentido.
    if (this.heightDim) this.heightDim.visible = mode === '3d';
  }

  setDimensionsVisible(visible) {
    this.dimensions.visible = visible;
  }

  /** Reconstruye las 4 paredes con los huecos de las aberturas actuales. */
  rebuildWalls() {
    disposeObject(this.wallGroup);
    this.wallGroup.clear();
    this.walls = [];
    for (const def of this.#wallDefinitions()) this.#createWall(def);
    for (const fn of this.listeners) fn();
  }

  // Datos de cada pared. Norte y sur cubren también las esquinas (ancho + 2 grosores);
  // este y oeste van entre ellas, así no quedan huecos ni solapes.
  //   localStart: dónde empieza el interior en la coordenada x LOCAL de la pared
  //   interiorZ:  qué cara (z local 0 o t) mira hacia el interior
  #wallDefinitions() {
    const { width: W, length: L, wallThickness: t } = this.dims;
    const V = (x, y, z) => new THREE.Vector3(x, y, z);
    return [
      { name: 'norte', length: W + 2 * t, position: V(-t, 0, -t), rotationY: 0, localStart: t, interiorZ: t },
      { name: 'sur', length: W + 2 * t, position: V(-t, 0, L), rotationY: 0, localStart: t, interiorZ: 0 },
      // Girar -90° en Y hace que el eje x local de la pared apunte a +Z del mundo.
      { name: 'oeste', length: L, position: V(0, 0, 0), rotationY: -Math.PI / 2, localStart: 0, interiorZ: 0 },
      { name: 'este', length: L, position: V(W + t, 0, 0), rotationY: -Math.PI / 2, localStart: 0, interiorZ: t },
    ];
  }

  /**
   * Crea una pared extruyendo su silueta (largo x alto) con el grosor del muro.
   * - Las ventanas son agujeros (holes) dentro de la silueta.
   * - Las puertas tocan el piso, así que no pueden ser agujeros cerrados: las
   *   recortamos como "muescas" en el borde inferior de la silueta.
   */
  #createWall(def) {
    const { height, wallThickness: t } = this.dims;
    const mine = this.openings.filter((o) => o.data.wall === def.name);
    const doors = mine.filter((o) => o.data.type === 'door').sort((a, b) => a.data.offset - b.data.offset);
    const windows = mine.filter((o) => o.data.type === 'window');

    const shape = new THREE.Shape();
    shape.moveTo(0, 0);
    for (const door of doors) {
      const x0 = def.localStart + door.data.offset;
      const x1 = x0 + door.data.width;
      shape.lineTo(x0, 0);
      shape.lineTo(x0, door.data.height);
      shape.lineTo(x1, door.data.height);
      shape.lineTo(x1, 0);
    }
    shape.lineTo(def.length, 0);
    shape.lineTo(def.length, height);
    shape.lineTo(0, height);
    shape.closePath();

    for (const win of windows) {
      const { offset, width, height: h, sill } = win.data;
      const x0 = def.localStart + offset;
      const hole = new THREE.Path();
      hole.moveTo(x0, sill);
      hole.lineTo(x0 + width, sill);
      hole.lineTo(x0 + width, sill + h);
      hole.lineTo(x0, sill + h);
      hole.closePath();
      shape.holes.push(hole);
    }

    const geometry = new THREE.ExtrudeGeometry(shape, { depth: t, bevelEnabled: false });
    const wall = new THREE.Mesh(geometry, wallMaterial);
    wall.name = `pared-${def.name}`;
    wall.position.copy(def.position);
    wall.rotation.y = def.rotationY;
    wall.castShadow = true;
    wall.receiveShadow = true;
    wall.add(new THREE.LineSegments(new THREE.EdgesGeometry(geometry), edgeMaterial));

    // Marcos, vidrios y hojas de puerta: hijos de la pared, en sus coordenadas locales.
    const info = { thickness: t, height, localStart: def.localStart, interiorZ: def.interiorZ };
    for (const opening of mine) {
      const { group, plan } = opening.buildVisual(info);
      wall.add(group, plan);
    }

    // Un punto sobre la cara interior: el centro de la habitación proyectado sobre la pared.
    const normal = this.interiorNormal(def.name);
    const { width: W, length: L } = this.dims;
    const pointInside = new THREE.Vector3(W / 2, 0, L / 2);
    if (def.name === 'norte') pointInside.z = 0;
    if (def.name === 'sur') pointInside.z = L;
    if (def.name === 'oeste') pointInside.x = 0;
    if (def.name === 'este') pointInside.x = W;

    wall.userData = { side: def.name, pointInside, normal };
    this.walls.push(wall);
    this.wallGroup.add(wall);
  }

  #createFloor(width, length) {
    const floor = new THREE.Mesh(new THREE.PlaneGeometry(width, length), floorMaterial);
    floor.rotation.x = -Math.PI / 2; // el plano nace vertical; lo acostamos
    floor.position.set(width / 2, 0, length / 2);
    floor.receiveShadow = true;
    floor.name = 'piso';
    this.group.add(floor);

    // Cuadrícula de 1 m sobre el piso para percibir la escala.
    const points = [];
    for (let x = 1; x < width; x++) points.push(new THREE.Vector3(x, 0, 0), new THREE.Vector3(x, 0, length));
    for (let z = 1; z < length; z++) points.push(new THREE.Vector3(0, 0, z), new THREE.Vector3(width, 0, z));
    const grid = new THREE.LineSegments(new THREE.BufferGeometry().setFromPoints(points), gridMaterial);
    grid.position.y = 0.002; // apenas encima del piso para evitar parpadeo (z-fighting)
    this.group.add(grid);
  }

  // Techo invisible: no se ve, pero bloquea la luz del sol que entraría desde arriba.
  #createCeiling() {
    const { width, length, height, wallThickness: t } = this.dims;
    const ceiling = new THREE.Mesh(new THREE.PlaneGeometry(width + 2 * t, length + 2 * t), ghostMaterial);
    ceiling.rotation.x = Math.PI / 2;
    ceiling.position.set(width / 2, height, length / 2);
    ceiling.castShadow = true;
    ceiling.userData.noPick = true;
    this.group.add(ceiling);
  }

  #createDimensions() {
    const { width, length, height, wallThickness: t } = this.dims;
    const V = (x, y, z) => new THREE.Vector3(x, y, z);
    const gap = t + 0.4; // separación de las cotas respecto al interior

    // Ancho (eje X), por fuera de la pared sur.
    this.dimensions.add(createDimension(V(0, 0.01, length), V(width, 0.01, length), V(0, 0, 1), gap));
    // Largo (eje Z), por fuera de la pared este.
    this.dimensions.add(createDimension(V(width, 0.01, 0), V(width, 0.01, length), V(1, 0, 0), gap));
    // Alto, en la esquina suroeste.
    this.heightDim = createDimension(V(0, 0, length), V(0, height, length), V(-1, 0, 0), gap);
    this.dimensions.add(this.heightDim);
  }

  /**
   * Tras cambiar las medidas, intenta que cada abertura siga cabiendo
   * (achicándola o moviéndola). Si no es posible, la elimina.
   */
  #fitOpenings() {
    const warnings = [];
    const H = this.dims.height;
    const round = (v) => Math.round(v * 100) / 100;
    const all = this.openings;
    this.openings = []; // se van agregando las que sí caben
    for (const opening of all) {
      const d = opening.data;
      const len = this.wallLength(d.wall);
      const fitted = { ...d, width: round(Math.min(d.width, len - 0.1)) };
      if (d.type === 'door') {
        fitted.height = round(Math.min(d.height, H - 0.05));
      } else {
        fitted.height = round(Math.min(d.height, H - 0.15));
        fitted.sill = round(Math.max(0.05, Math.min(d.sill, H - fitted.height - 0.05)));
      }
      fitted.offset = round(Math.max(0.05, Math.min(d.offset, len - fitted.width - 0.05)));

      if (validateOpening(fitted, this, opening)) {
        warnings.push(`${opening.label} ya no cabía y se eliminó.`);
      } else {
        opening.data = fitted;
        this.openings.push(opening);
      }
    }
    return warnings;
  }
}
