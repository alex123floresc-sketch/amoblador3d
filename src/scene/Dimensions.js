// Cotas (líneas de medida) estilo plano de arquitectura: línea principal,
// líneas de extensión, marcas diagonales en los extremos y una etiqueta con el valor.
import * as THREE from 'three';
import { CSS2DObject } from 'three/addons/renderers/CSS2DRenderer.js';

const lineMaterial = new THREE.LineBasicMaterial({
  color: 0xffd166,
  depthTest: false, // las cotas se ven aunque haya paredes delante
  transparent: true,
});

/** Formatea metros al estilo local: 3.5 -> "3,50 m". */
export function formatMeters(value) {
  return `${value.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} m`;
}

/**
 * Crea una cota entre dos puntos.
 * @param {THREE.Vector3} start punto inicial medido
 * @param {THREE.Vector3} end punto final medido
 * @param {THREE.Vector3} offsetDir dirección (unitaria) hacia donde se separa la cota del objeto
 * @param {number} offset distancia de separación en metros
 * @returns {THREE.Group}
 */
export function createDimension(start, end, offsetDir, offset = 0.4) {
  const group = new THREE.Group();
  const shift = offsetDir.clone().multiplyScalar(offset);
  const a = start.clone().add(shift);
  const b = end.clone().add(shift);

  // Dirección de la medida y marca diagonal (45°) típica de planos.
  const dir = b.clone().sub(a).normalize();
  const tick = dir.clone().add(offsetDir).normalize().multiplyScalar(0.08);
  // La línea de extensión sobrepasa un poco la línea de cota.
  const extra = offsetDir.clone().multiplyScalar(0.1);

  const points = [
    a, b, // línea de cota
    start, a.clone().add(extra), // extensión inicial
    end, b.clone().add(extra), // extensión final
    a.clone().sub(tick), a.clone().add(tick), // marca inicial
    b.clone().sub(tick), b.clone().add(tick), // marca final
  ];
  const geometry = new THREE.BufferGeometry().setFromPoints(points);
  const lines = new THREE.LineSegments(geometry, lineMaterial);
  lines.renderOrder = 999; // dibujar encima del resto
  group.add(lines);

  // Etiqueta HTML en el punto medio.
  const el = document.createElement('div');
  el.className = 'dim-label';
  el.textContent = formatMeters(start.distanceTo(end));
  const label = new CSS2DObject(el);
  label.position.copy(a).lerp(b, 0.5);
  group.add(label);

  return group;
}

/** Libera la geometría y elimina del DOM las etiquetas de un grupo de cotas. */
export function disposeDimensions(group) {
  group.traverse((obj) => {
    if (obj.geometry) obj.geometry.dispose();
    if (obj.isCSS2DObject) obj.element.remove();
  });
}
