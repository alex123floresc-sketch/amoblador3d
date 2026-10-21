// Diálogo para agregar un modelo importado al catálogo: nombre, medidas reales
// y giro inicial (por si el modelo viene "mirando" hacia otro lado).
import { measureModel } from '../../catalog/models.js';

const round = (v) => Math.round(v * 100) / 100;

export class ImportDialog {
  constructor(dialog) {
    this.dialog = dialog;
    this.form = dialog.querySelector('form');
    this.sizeEl = dialog.querySelector('.import-size');
    this.errorEl = dialog.querySelector('.error');
    const field = (name) => this.form.elements.namedItem(name);
    this.field = field;

    // Mantener proporciones: al cambiar una medida, las otras se ajustan igual.
    for (const key of ['width', 'depth', 'height']) {
      field(key).addEventListener('change', () => {
        if (!field('keep').checked) return;
        const factor = parseFloat(field(key).value) / this.natural[key];
        if (!Number.isFinite(factor) || factor <= 0) return;
        for (const other of ['width', 'depth', 'height']) {
          if (other !== key) field(other).value = round(this.natural[other] * factor);
        }
      });
    }
    field('rotation').addEventListener('change', () => this.#measure());
    dialog.querySelector('[data-cancel]').addEventListener('click', () => this.#close(null));
    dialog.addEventListener('cancel', () => this.#close(null)); // tecla Esc
    this.form.addEventListener('submit', (e) => {
      e.preventDefault();
      this.#submit();
    });
  }

  /**
   * Abre el diálogo. Devuelve una promesa con { name, width, depth, height, rotation }
   * o null si el usuario cancela.
   */
  open({ name, scene }) {
    this.scene = scene;
    this.field('name').value = name;
    this.field('rotation').value = '0';
    this.errorEl.hidden = true;
    this.#measure();
    this.dialog.showModal();
    return new Promise((resolve) => (this.resolve = resolve));
  }

  // Mide el modelo y propone medidas. Muchos modelos vienen en centímetros o
  // milímetros: si es enorme, suponemos esas unidades y lo convertimos a metros.
  #measure() {
    const size = measureModel(this.scene, parseFloat(this.field('rotation').value));
    const biggest = Math.max(size.x, size.y, size.z);
    const [divisor, unit] = biggest > 2000 ? [1000, 'mm'] : biggest > 20 ? [100, 'cm'] : [1, 'm'];
    this.natural = { width: size.x / divisor, depth: size.z / divisor, height: size.y / divisor };
    for (const key of ['width', 'depth', 'height']) this.field(key).value = round(this.natural[key]);
    this.sizeEl.textContent =
      `Tamaño original: ${round(size.x)} × ${round(size.z)} × ${round(size.y)} (ancho × prof. × alto)` +
      (unit === 'm' ? ', interpretado en metros.' : `, parece estar en ${unit}: se convirtió a metros.`) +
      ' Ajusta las medidas reales del mueble.';
  }

  #submit() {
    const name = this.field('name').value.trim();
    const dims = {};
    for (const [key, max] of [['width', 6], ['depth', 6], ['height', 4]]) {
      dims[key] = round(parseFloat(this.field(key).value));
      if (!(dims[key] >= 0.05 && dims[key] <= max)) {
        this.errorEl.textContent = `Las medidas deben estar entre 0,05 y ${max} m.`;
        this.errorEl.hidden = false;
        return;
      }
    }
    if (!name) {
      this.errorEl.textContent = 'Escribe un nombre.';
      this.errorEl.hidden = false;
      return;
    }
    this.#close({ name, ...dims, rotation: parseFloat(this.field('rotation').value) });
  }

  #close(result) {
    if (this.dialog.open) this.dialog.close();
    this.resolve?.(result);
    this.resolve = null;
  }
}
