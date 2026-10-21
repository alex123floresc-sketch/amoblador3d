// Carga de modelos glTF (.glb/.gltf) y escalado a medidas reales.
//
// Un modelo descargado de internet puede venir en centímetros, en pulgadas o
// con el origen en cualquier lugar. Para usarlo como mueble lo "normalizamos":
//   1. Lo giramos (si hace falta) para que su frente mire a +Z.
//   2. Medimos su caja envolvente.
//   3. Lo escalamos en cada eje para que mida EXACTAMENTE ancho x alto x profundidad.
//   4. Lo movemos para que el centro de su base quede en el origen.
// Así cumple la misma regla que los muebles paramétricos y las colisiones son exactas.
import * as THREE from 'three';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';

const loader = new GLTFLoader();
const cache = new Map(); // url -> Promise<THREE.Object3D>; cada modelo se descarga una sola vez

/** Carga un modelo (con caché). Devuelve una promesa con la escena del glTF. */
export function loadModel(url) {
  if (!cache.has(url)) {
    const promise = loader.loadAsync(url).then((gltf) => {
      gltf.scene.traverse((o) => {
        if (o.isMesh) {
          o.castShadow = true;
          o.receiveShadow = true;
        }
      });
      return gltf.scene;
    });
    promise.catch(() => cache.delete(url)); // si falla, permitir reintentar
    cache.set(url, promise);
  }
  return cache.get(url);
}

/** Tamaño natural del modelo (en sus propias unidades) tras girarlo `rotationDeg`. */
export function measureModel(scene, rotationDeg = 0) {
  const rotated = new THREE.Group();
  rotated.rotation.y = THREE.MathUtils.degToRad(rotationDeg);
  rotated.add(scene.clone(true));
  rotated.updateMatrixWorld(true);
  return new THREE.Box3().setFromObject(rotated, true).getSize(new THREE.Vector3());
}

/**
 * Crea una copia del modelo escalada a las medidas pedidas.
 * La copia comparte geometrías y materiales con el original (ahorra memoria),
 * por eso se marca como "shared": no hay que liberarla con dispose().
 */
export function fitModel(scene, { width, depth, height }, rotationDeg = 0) {
  const rotated = new THREE.Group();
  rotated.rotation.y = THREE.MathUtils.degToRad(rotationDeg);
  rotated.add(scene.clone(true));
  rotated.updateMatrixWorld(true);

  const box = new THREE.Box3().setFromObject(rotated, true);
  const size = box.getSize(new THREE.Vector3());
  const center = box.getCenter(new THREE.Vector3());
  rotated.position.set(-center.x, -box.min.y, -center.z); // centro de la base al origen

  const scaler = new THREE.Group();
  const safe = (v) => Math.max(v, 1e-4); // evita dividir entre 0 en modelos planos
  scaler.scale.set(width / safe(size.x), height / safe(size.y), depth / safe(size.z));
  scaler.add(rotated);
  scaler.userData.shared = true;
  return scaler;
}
