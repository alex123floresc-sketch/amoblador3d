// SceneManager: crea el renderizador, las cámaras (3D y planta), los controles
// orbitales y el bucle de render. Convención: 1 unidad de Three.js = 1 metro.
import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import { CSS2DRenderer } from 'three/addons/renderers/CSS2DRenderer.js';

export class SceneManager {
  /** @param {HTMLElement} container elemento donde se dibuja la escena */
  constructor(container) {
    this.container = container;
    this.mode = '3d'; // '3d' | 'plan'
    this.updateCallbacks = []; // funciones que se ejecutan en cada frame

    this.scene = new THREE.Scene();
    this.scene.background = new THREE.Color(0x2b3036);

    // Renderizador WebGL con sombras suaves. El "tone mapping" ACES evita que las
    // zonas muy iluminadas se vean quemadas (blanco puro) cuando sumamos varias luces.
    this.renderer = new THREE.WebGLRenderer({ antialias: true, preserveDrawingBuffer: true });
    this.renderer.setPixelRatio(window.devicePixelRatio);
    this.renderer.shadowMap.enabled = true;
    this.renderer.shadowMap.type = THREE.PCFShadowMap; // en Three.js r18x ya es suave
    this.renderer.toneMapping = THREE.ACESFilmicToneMapping;
    container.appendChild(this.renderer.domElement);

    // Segundo renderizador para textos HTML (etiquetas de cotas) sobre el canvas.
    this.labelRenderer = new CSS2DRenderer();
    Object.assign(this.labelRenderer.domElement.style, {
      position: 'absolute',
      top: '0',
      left: '0',
      pointerEvents: 'none',
    });
    container.appendChild(this.labelRenderer.domElement);

    this.#createCameras();

    // Ajustar tamaño cuando cambia el contenedor (ventana redimensionada, etc.).
    new ResizeObserver(() => this.#onResize()).observe(container);
    this.#onResize();

    this.renderer.setAnimationLoop(() => this.#render());
  }

  #createCameras() {
    const canvas = this.renderer.domElement;

    // Cámara en perspectiva para la vista 3D (fov 50°, ve de 5 cm a 500 m).
    this.perspCamera = new THREE.PerspectiveCamera(50, 1, 0.05, 500);
    this.perspCamera.position.set(8, 7, 10);
    this.perspControls = new OrbitControls(this.perspCamera, canvas);
    this.perspControls.enableDamping = true;
    this.perspControls.maxPolarAngle = Math.PI / 2 - 0.02; // no bajar del piso
    this.perspControls.minDistance = 0.5;
    this.perspControls.maxDistance = 100;

    // Cámara ortográfica para la planta 2D: mira hacia abajo, sin perspectiva,
    // así las medidas en pantalla son proporcionales a las reales.
    this.viewSize = 10; // metros visibles en vertical (zoom = 1)
    this.orthoCamera = new THREE.OrthographicCamera(-1, 1, 1, -1, 0.1, 200);
    this.orthoCamera.up.set(0, 0, -1); // el norte (z negativo) queda arriba
    this.orthoControls = new OrbitControls(this.orthoCamera, canvas);
    this.orthoControls.enableRotate = false; // en planta solo se mueve y hace zoom
    this.orthoControls.screenSpacePanning = true;
    this.orthoControls.mouseButtons.LEFT = THREE.MOUSE.PAN;
    this.orthoControls.minZoom = 0.2;
    this.orthoControls.maxZoom = 20;
    this.orthoControls.enabled = false;
  }

  get camera() {
    return this.mode === '3d' ? this.perspCamera : this.orthoCamera;
  }

  get controls() {
    return this.mode === '3d' ? this.perspControls : this.orthoControls;
  }

  /** Cambia entre vista '3d' y 'plan'. */
  setMode(mode) {
    this.mode = mode;
    this.setControlsLocked(false);
  }

  /** Bloquea la cámara (por ejemplo, mientras se arrastra un mueble). */
  setControlsLocked(locked) {
    this.perspControls.enabled = !locked && this.mode === '3d';
    this.orthoControls.enabled = !locked && this.mode === 'plan';
  }

  /** Registra una función que se llamará en cada frame con (camera, mode). */
  onUpdate(fn) {
    this.updateCallbacks.push(fn);
  }

  /**
   * Encuadra ambas cámaras para que se vea completa la caja indicada.
   * @param {THREE.Box3} box caja que envuelve la habitación
   */
  frame(box) {
    const center = box.getCenter(new THREE.Vector3());
    const size = box.getSize(new THREE.Vector3());
    const radius = size.length() / 2;

    // Vista 3D: cámara en diagonal (desde el sureste y arriba), a una distancia
    // suficiente para que la esfera que envuelve la habitación quepa en el fov.
    const fov = THREE.MathUtils.degToRad(this.perspCamera.fov);
    const distance = (radius / Math.sin(fov / 2)) * 1.25; // 25 % extra para las cotas
    const dir = new THREE.Vector3(0.6, 0.75, 1).normalize();
    this.perspCamera.position.copy(center).addScaledVector(dir, distance);
    this.perspControls.target.copy(center);
    this.perspControls.update();

    // Planta: cámara encima del centro, mirando hacia abajo.
    const margin = 1.6; // metros extra para que entren las cotas
    const aspect = this.#aspect();
    this.viewSize = Math.max(size.z + margin * 2, (size.x + margin * 2) / aspect);
    this.orthoCamera.position.set(center.x, 50, center.z);
    this.orthoCamera.zoom = 1;
    this.orthoControls.target.set(center.x, 0, center.z);
    this.#updateOrthoFrustum();
    this.orthoControls.update();
  }

  /**
   * Devuelve una imagen PNG (data URL) de la vista actual, `scale` veces más
   * grande que la pantalla. Las etiquetas de las cotas son HTML (no están en el
   * canvas 3D), así que las dibujamos encima a mano con un canvas 2D.
   */
  screenshot(scale = 2) {
    const { clientWidth: w, clientHeight: h } = this.container;
    const previousRatio = this.renderer.getPixelRatio();

    // 1. Renderizar la escena 3D en alta resolución.
    this.renderer.setPixelRatio(scale);
    this.renderer.setSize(w, h, false);
    for (const fn of this.updateCallbacks) fn(this.camera, this.mode);
    this.renderer.render(this.scene, this.camera);
    this.labelRenderer.render(this.scene, this.camera);

    const out = document.createElement('canvas');
    out.width = w * scale;
    out.height = h * scale;
    const ctx = out.getContext('2d');
    ctx.drawImage(this.renderer.domElement, 0, 0, out.width, out.height);

    // 2. Copiar las etiquetas visibles de las cotas.
    const origin = this.container.getBoundingClientRect();
    ctx.font = `600 ${12 * scale}px system-ui, sans-serif`;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    for (const el of this.labelRenderer.domElement.querySelectorAll('.dim-label')) {
      if (el.style.display === 'none' || el.closest('[style*="display: none"]')) continue;
      const r = el.getBoundingClientRect();
      if (r.width === 0) continue;
      const x = (r.left - origin.left) * scale;
      const y = (r.top - origin.top) * scale;
      ctx.fillStyle = 'rgba(255, 255, 255, 0.92)';
      ctx.beginPath();
      ctx.roundRect(x, y, r.width * scale, r.height * scale, 3 * scale);
      ctx.fill();
      ctx.fillStyle = '#1a1a1a';
      ctx.fillText(el.textContent, x + (r.width * scale) / 2, y + (r.height * scale) / 2);
    }

    // 3. Volver a la resolución normal.
    this.renderer.setPixelRatio(previousRatio);
    this.renderer.setSize(w, h, false);
    return out.toDataURL('image/png');
  }

  #aspect() {
    const { clientWidth: w, clientHeight: h } = this.container;
    return h > 0 ? w / h : 1;
  }

  #updateOrthoFrustum() {
    const halfH = this.viewSize / 2;
    const halfW = halfH * this.#aspect();
    Object.assign(this.orthoCamera, { left: -halfW, right: halfW, top: halfH, bottom: -halfH });
    this.orthoCamera.updateProjectionMatrix();
  }

  #onResize() {
    const { clientWidth: w, clientHeight: h } = this.container;
    if (w === 0 || h === 0) return;
    this.renderer.setSize(w, h);
    this.labelRenderer.setSize(w, h);
    this.perspCamera.aspect = w / h;
    this.perspCamera.updateProjectionMatrix();
    this.#updateOrthoFrustum();
  }

  #render() {
    this.controls.update(); // necesario por el "damping" (movimiento suave)
    for (const fn of this.updateCallbacks) fn(this.camera, this.mode);
    this.renderer.render(this.scene, this.camera);
    this.labelRenderer.render(this.scene, this.camera);
  }
}
