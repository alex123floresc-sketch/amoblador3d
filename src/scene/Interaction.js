// Interacción con el ratón sobre la escena:
//   - Clic sobre un elemento -> lo selecciona (clic en el vacío -> deselecciona).
//   - Arrastrar un mueble -> lo mueve sobre el piso (la cámara se bloquea mientras tanto).
//
// Usamos "raycasting": se lanza un rayo desde la cámara a través del píxel del
// ratón y se ve qué objetos atraviesa, ordenados del más cercano al más lejano.
import * as THREE from 'three';

const CLICK_TOLERANCE = 5; // píxeles: si el ratón se movió más, fue un arrastre de cámara

export class Interaction {
  /**
   * @param {object} opts
   * @param {import('./SceneManager.js').SceneManager} opts.sceneManager
   * @param {() => THREE.Object3D[]} opts.getRoots objetos donde buscar al hacer clic
   * @param {import('./Furniture.js').FurnitureManager} opts.furniture
   * @param {(entity: object|null) => void} opts.onSelect
   * @param {() => void} opts.onDrag se llama mientras se arrastra un mueble
   * @param {() => void} opts.onDragEnd se llama al soltar el mueble
   */
  constructor({ sceneManager, getRoots, furniture, onSelect, onDrag, onDragEnd }) {
    Object.assign(this, { sceneManager, getRoots, furniture, onSelect, onDrag, onDragEnd });
    this.raycaster = new THREE.Raycaster();
    this.floor = new THREE.Plane(new THREE.Vector3(0, 1, 0), 0); // plano y = 0
    this.canvas = sceneManager.renderer.domElement;

    // "capture: true" hace que recibamos el evento ANTES que OrbitControls,
    // así podemos bloquear la cámara si el clic empieza sobre un mueble.
    const container = sceneManager.container;
    container.addEventListener('pointerdown', (e) => this.#onDown(e), { capture: true });
    window.addEventListener('pointermove', (e) => this.#onMove(e));
    window.addEventListener('pointerup', (e) => this.#onUp(e));
  }

  #onDown(e) {
    if (e.button !== 0 || e.target !== this.canvas) return;
    const entity = this.#pick(e);
    this.down = { x: e.clientX, y: e.clientY, entity };

    if (entity?.kind === 'furniture') {
      // Guardamos la diferencia entre el punto del piso clicado y el centro del
      // mueble, para que no "salte" al centro del cursor al empezar a arrastrar.
      const p = this.#floorPoint(e);
      if (!p) return;
      this.drag = { item: entity, dx: entity.data.x - p.x, dz: entity.data.z - p.z };
      this.sceneManager.setControlsLocked(true);
      this.onSelect(entity);
      this.canvas.style.cursor = 'grabbing';
    }
  }

  #onMove(e) {
    if (this.drag) {
      const p = this.#floorPoint(e);
      if (p && this.furniture.moveTo(this.drag.item, p.x + this.drag.dx, p.z + this.drag.dz)) this.onDrag();
      return;
    }
    // Cambiar el cursor al pasar sobre algo seleccionable.
    if (e.target === this.canvas && e.buttons === 0) {
      const entity = this.#pick(e);
      this.canvas.style.cursor = entity?.kind === 'furniture' ? 'grab' : entity ? 'pointer' : '';
    }
  }

  #onUp(e) {
    if (this.drag) {
      this.drag = null;
      this.sceneManager.setControlsLocked(false);
      this.canvas.style.cursor = 'grab';
      this.down = null;
      this.onDragEnd();
      return;
    }
    if (!this.down) return;
    const moved = Math.hypot(e.clientX - this.down.x, e.clientY - this.down.y);
    if (moved < CLICK_TOLERANCE) this.onSelect(this.down.entity);
    this.down = null;
  }

  #setRay(e) {
    const rect = this.canvas.getBoundingClientRect();
    // Coordenadas normalizadas: de -1 a 1 en ambos ejes (y hacia arriba).
    const ndc = new THREE.Vector2(((e.clientX - rect.left) / rect.width) * 2 - 1, -((e.clientY - rect.top) / rect.height) * 2 + 1);
    this.raycaster.setFromCamera(ndc, this.sceneManager.camera);
  }

  /** Devuelve la entidad (abertura, luz o mueble) bajo el ratón, o null. */
  #pick(e) {
    this.#setRay(e);
    const hits = this.raycaster.intersectObjects(this.getRoots(), true);
    for (const { object } of hits) {
      if (object.isLine || object.isPoints) continue; // cuadrícula, bordes, cotas
      if (object.userData.noPick || object.material?.colorWrite === false) continue; // techo/paredes fantasma
      if (!isVisible(object)) continue;
      // El primer objeto sólido manda: si es una pared o el piso, no hay selección.
      return findEntity(object);
    }
    return null;
  }

  #floorPoint(e) {
    this.#setRay(e);
    return this.raycaster.ray.intersectPlane(this.floor, new THREE.Vector3());
  }
}

/** Un objeto es visible solo si él y todos sus padres lo son. */
function isVisible(object) {
  for (let o = object; o; o = o.parent) if (!o.visible) return false;
  return true;
}

/** Sube por los padres hasta encontrar a qué entidad pertenece el objeto. */
function findEntity(object) {
  for (let o = object; o; o = o.parent) if (o.userData.entity) return o.userData.entity;
  return null;
}
