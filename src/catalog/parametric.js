// Muebles paramétricos: se construyen con cajas a partir de ancho, profundidad y alto.
//
// Regla que cumplen TODOS los muebles (importante para las colisiones):
//   - Ocupan exactamente x ∈ [-ancho/2, ancho/2], z ∈ [-prof/2, prof/2], y ∈ [0, alto].
//   - El origen es el centro de la base, a nivel del piso.
//   - El frente del mueble mira hacia +Z (la cabecera de la cama queda en -Z).
//
// Para agregar un mueble paramétrico nuevo: crea su función build y regístralo en PARAMETRIC.
import * as THREE from 'three';

/** Agrega una caja (w x h x d) centrada en (x, y, z). */
function box(group, material, w, h, d, x, y, z) {
  const mesh = new THREE.Mesh(new THREE.BoxGeometry(w, h, d), material);
  mesh.position.set(x, y, z);
  mesh.castShadow = true;
  mesh.receiveShadow = true;
  group.add(mesh);
  return mesh;
}

const mat = (color, roughness = 0.7, metalness = 0) => new THREE.MeshStandardMaterial({ color, roughness, metalness });
const darker = (color, factor) => new THREE.Color(color).multiplyScalar(factor);
const metal = () => mat(0xb8b8b8, 0.3, 0.8);

function buildBed({ width: w, depth: d, height: h, color }) {
  const g = new THREE.Group();
  const head = 0.06; // grosor de la cabecera
  const top = Math.min(0.55, h - 0.05); // altura de la superficie del colchón
  const base = top - 0.2;
  const bodyD = d - head; // profundidad sin cabecera
  const bodyZ = head / 2; // centro de base/colchón

  box(g, mat(color), w, base, bodyD, 0, base / 2, bodyZ); // base
  box(g, mat(0xf4f1ea, 0.9), w - 0.04, 0.2, bodyD - 0.04, 0, base + 0.1, bodyZ); // colchón
  const blanketD = bodyD * 0.65;
  box(g, mat(0x5b7fa6, 0.95), w, 0.21, blanketD, 0, base + 0.105, d / 2 - blanketD / 2); // frazada
  box(g, mat(color), w, h, head, 0, h / 2, -d / 2 + head / 2); // cabecera

  const pillows = w >= 1.2 ? 2 : 1;
  const pw = (w - 0.1 * (pillows + 1)) / pillows;
  const ph = Math.min(0.1, h - top);
  for (let i = 0; i < pillows; i++) {
    const x = -w / 2 + 0.1 + pw / 2 + i * (pw + 0.1);
    box(g, mat(0xffffff, 0.95), pw, ph, 0.35, x, top + ph / 2, -d / 2 + head + 0.03 + 0.175); // almohada
  }
  return g;
}

function buildTable({ width: w, depth: d, height: h, color }) {
  const g = new THREE.Group();
  const wood = mat(color);
  const t = 0.04; // grosor del tablero
  box(g, wood, w, t, d, 0, h - t / 2, 0);
  const lx = w / 2 - 0.06;
  const lz = d / 2 - 0.06;
  for (const [sx, sz] of [[-1, -1], [1, -1], [-1, 1], [1, 1]]) {
    box(g, wood, 0.05, h - t, 0.05, sx * lx, (h - t) / 2, sz * lz); // patas
  }
  return g;
}

function buildChair({ width: w, depth: d, height: h, color }) {
  const g = new THREE.Group();
  const wood = mat(color);
  const seat = Math.min(0.46, h * 0.5); // altura del asiento
  const leg = 0.035;
  const lx = w / 2 - leg / 2;
  const lz = d / 2 - leg / 2;
  for (const [sx, sz] of [[-1, -1], [1, -1], [-1, 1], [1, 1]]) {
    box(g, wood, leg, seat - 0.04, leg, sx * lx, (seat - 0.04) / 2, sz * lz); // patas
  }
  box(g, mat(darker(color, 0.8), 0.9), w, 0.04, d, 0, seat - 0.02, 0); // asiento
  for (const sx of [-1, 1]) box(g, wood, leg, h - seat, leg, sx * lx, seat + (h - seat) / 2, -lz); // postes del respaldo
  box(g, wood, w, 0.18, 0.025, 0, h - 0.09, -d / 2 + 0.0125); // respaldo
  return g;
}

function buildCloset({ width: w, depth: d, height: h, color }) {
  const g = new THREE.Group();
  box(g, mat(color), w, h, d - 0.04, 0, h / 2, -0.02); // cuerpo
  const doors = w > 1.6 ? 3 : 2;
  const dw = w / doors;
  const doorMat = mat(darker(color, 0.93), 0.6);
  for (let i = 0; i < doors; i++) {
    const x = -w / 2 + dw * (i + 0.5);
    box(g, doorMat, dw - 0.006, h - 0.01, 0.02, x, h / 2, d / 2 - 0.03); // puerta
    const side = i < doors / 2 ? 1 : -1; // manija hacia el centro del closet
    box(g, metal(), 0.02, 0.25, 0.02, x + side * (dw / 2 - 0.05), h * 0.5, d / 2 - 0.01); // manija
  }
  return g;
}

function buildDesk({ width: w, depth: d, height: h, color }) {
  const g = new THREE.Group();
  const wood = mat(color);
  const t = 0.03; // grosor del tablero
  const under = h - t; // espacio bajo el tablero
  box(g, wood, w, t, d, 0, h - t / 2, 0); // tablero
  box(g, wood, 0.03, under, d - 0.02, -w / 2 + 0.015, under / 2, 0); // lateral izquierdo
  const pw = Math.min(0.4, w * 0.35); // ancho de la cajonera
  const px = w / 2 - pw / 2;
  box(g, wood, pw, under, d - 0.04, px, under / 2, -0.01); // cajonera
  const drawerH = (under - 0.02) / 3;
  const frontMat = mat(darker(color, 0.85), 0.5);
  for (let i = 0; i < 3; i++) {
    box(g, frontMat, pw - 0.02, drawerH - 0.01, 0.015, px, 0.01 + drawerH * (i + 0.5), d / 2 - 0.0125); // frente de cajón
  }
  const backW = w - pw - 0.03;
  box(g, wood, backW, 0.3, 0.02, -w / 2 + 0.03 + backW / 2, under - 0.15, -d / 2 + 0.02); // faldón trasero
  return g;
}

/**
 * Catálogo de muebles paramétricos.
 *   defaults: medidas iniciales (m) y color
 *   limits:   [mínimo, máximo] permitido para cada medida (m)
 */
export const PARAMETRIC = {
  cama: {
    name: 'Cama',
    defaults: { width: 1.4, depth: 1.9, height: 0.9, color: '#8b6b4a' },
    limits: { width: [0.8, 2.2], depth: [1.8, 2.3], height: [0.5, 1.4] },
    build: buildBed,
  },
  mesa: {
    name: 'Mesa',
    defaults: { width: 1.2, depth: 0.8, height: 0.75, color: '#a0764f' },
    limits: { width: [0.4, 3], depth: [0.4, 1.5], height: [0.4, 1.1] },
    build: buildTable,
  },
  silla: {
    name: 'Silla',
    defaults: { width: 0.45, depth: 0.5, height: 0.9, color: '#6d4c33' },
    limits: { width: [0.35, 0.7], depth: [0.35, 0.7], height: [0.7, 1.2] },
    build: buildChair,
  },
  closet: {
    name: 'Closet',
    defaults: { width: 1.2, depth: 0.6, height: 2.0, color: '#e8e2d6' },
    limits: { width: [0.5, 3], depth: [0.4, 0.8], height: [1.2, 2.6] },
    build: buildCloset,
  },
  escritorio: {
    name: 'Escritorio',
    defaults: { width: 1.2, depth: 0.6, height: 0.75, color: '#d9c6a5' },
    limits: { width: [0.8, 2.4], depth: [0.45, 0.9], height: [0.65, 0.9] },
    build: buildDesk,
  },
};
