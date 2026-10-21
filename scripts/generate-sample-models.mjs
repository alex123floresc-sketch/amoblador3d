// Genera los modelos .glb de ejemplo de assets/models usando Three.js en Node.
// Ejecutar con:  npm run models:samples
//
// Sirve para tener modelos de prueba sin descargar nada. Los modelos reales
// (descargados de internet o hechos en Blender) se agregan igual: copiando el
// .glb en assets/models y registrándolo en assets/catalog.json.
import * as THREE from 'three';
import { GLTFExporter } from 'three/addons/exporters/GLTFExporter.js';
import fs from 'node:fs/promises';
import path from 'node:path';

// GLTFExporter usa FileReader (API del navegador). En Node creamos una versión mínima.
globalThis.FileReader ??= class {
  readAsArrayBuffer(blob) {
    blob.arrayBuffer().then((result) => {
      this.result = result;
      this.onloadend?.();
    });
  }
  readAsDataURL(blob) {
    blob.arrayBuffer().then((buf) => {
      this.result = `data:${blob.type};base64,${Buffer.from(buf).toString('base64')}`;
      this.onloadend?.();
    });
  }
};

const mat = (color, roughness = 0.7, metalness = 0) => new THREE.MeshStandardMaterial({ color, roughness, metalness });

function add(group, geometry, material, x, y, z) {
  const mesh = new THREE.Mesh(geometry, material);
  mesh.position.set(x, y, z);
  group.add(mesh);
  return mesh;
}

// Sofá de 2 plazas (medidas en metros, frente hacia +Z).
function sofa() {
  const g = new THREE.Group();
  g.name = 'Sofa';
  const fabric = mat(0x5f7d8c, 0.95);
  const cushion = mat(0x6f8f9e, 0.95);
  const wood = mat(0x3b2a1e);
  const box = (w, h, d) => new THREE.BoxGeometry(w, h, d);
  add(g, box(1.6, 0.25, 0.85), fabric, 0, 0.225, 0); // base
  add(g, box(1.6, 0.45, 0.2), fabric, 0, 0.625, -0.325); // respaldo
  add(g, box(0.18, 0.3, 0.85), fabric, -0.71, 0.5, 0); // brazo izq.
  add(g, box(0.18, 0.3, 0.85), fabric, 0.71, 0.5, 0); // brazo der.
  add(g, box(0.6, 0.12, 0.62), cushion, -0.31, 0.41, 0.09); // cojín
  add(g, box(0.6, 0.12, 0.62), cushion, 0.31, 0.41, 0.09); // cojín
  for (const [x, z] of [[-0.72, -0.35], [0.72, -0.35], [-0.72, 0.35], [0.72, 0.35]]) {
    add(g, new THREE.CylinderGeometry(0.025, 0.02, 0.1, 12), wood, x, 0.05, z); // patas
  }
  return g;
}

// Lámpara de pie.
function floorLamp() {
  const g = new THREE.Group();
  g.name = 'Lampara';
  const metal = mat(0x2d2d2d, 0.4, 0.8);
  add(g, new THREE.CylinderGeometry(0.16, 0.18, 0.03, 32), metal, 0, 0.015, 0); // base
  add(g, new THREE.CylinderGeometry(0.012, 0.012, 1.3, 12), metal, 0, 0.68, 0); // tubo
  const shade = mat(0xf3e9d2, 0.9);
  shade.side = THREE.DoubleSide;
  add(g, new THREE.CylinderGeometry(0.12, 0.2, 0.28, 32, 1, true), shade, 0, 1.46, 0); // pantalla
  return g;
}

// Planta en maceta, modelada en CENTÍMETROS a propósito: así se prueba que el
// catálogo la escala a metros correctamente.
function plant() {
  const g = new THREE.Group();
  g.name = 'Planta';
  add(g, new THREE.CylinderGeometry(20, 15, 35, 24), mat(0xb5651d, 0.8), 0, 17.5, 0); // maceta
  add(g, new THREE.CylinderGeometry(18, 18, 3, 24), mat(0x3d2b1f, 1), 0, 34, 0); // tierra
  const leaf = mat(0x3f7d3a, 0.8);
  add(g, new THREE.SphereGeometry(28, 16, 12), leaf, 0, 75, 0);
  add(g, new THREE.SphereGeometry(20, 16, 12), leaf, 12, 95, 6);
  add(g, new THREE.SphereGeometry(18, 16, 12), leaf, -10, 92, -8);
  return g;
}

const exporter = new GLTFExporter();
const outDir = path.join(import.meta.dirname, '..', 'assets', 'models');
await fs.mkdir(outDir, { recursive: true });

for (const [file, build] of [['sofa.glb', sofa], ['lampara-pie.glb', floorLamp], ['planta.glb', plant]]) {
  const glb = await exporter.parseAsync(build(), { binary: true });
  await fs.writeFile(path.join(outDir, file), Buffer.from(glb));
  console.log(`✓ assets/models/${file}`);
}
