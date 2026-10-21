// Formulario de medidas de la habitación: valida los datos y avisa con onSubmit.
import { formatMeters } from '../../scene/Dimensions.js';

// Límites razonables para una habitación (en metros).
const LIMITS = {
  width: { min: 1, max: 30, label: 'Ancho' },
  length: { min: 1, max: 30, label: 'Largo' },
  height: { min: 2, max: 6, label: 'Alto' },
  wallThickness: { min: 0.05, max: 0.5, label: 'Grosor de muro' },
};

export class RoomForm {
  /**
   * @param {HTMLFormElement} form
   * @param {(dims: object) => void} onSubmit se llama con medidas válidas
   */
  constructor(form, onSubmit) {
    this.form = form;
    this.errorEl = document.getElementById('room-error');
    this.infoEl = document.getElementById('room-info');

    form.addEventListener('submit', (e) => {
      e.preventDefault(); // evita que la página se recargue
      const dims = this.#read();
      if (dims) onSubmit(dims);
    });
  }

  /** Rellena los campos con unas medidas. */
  setValues(dims) {
    for (const key of Object.keys(LIMITS)) this.#field(key).value = dims[key];
    this.#showInfo(dims);
  }

  /** Lee y valida los campos. Devuelve las medidas o null si hay error. */
  #read() {
    const dims = {};
    for (const [key, { min, max, label }] of Object.entries(LIMITS)) {
      const value = parseFloat(this.#field(key).value);
      if (!Number.isFinite(value) || value < min || value > max) {
        this.#showError(`${label} debe estar entre ${min} y ${max} m.`);
        this.#field(key).focus();
        return null;
      }
      dims[key] = Math.round(value * 100) / 100; // precisión de 1 cm
    }
    this.#showError(null);
    this.#showInfo(dims);
    return dims;
  }

  // Usamos namedItem() y no elements[key]: elements.length es la CANTIDAD de
  // campos del formulario, así que elements['length'] no devolvería el input.
  #field(name) {
    return this.form.elements.namedItem(name);
  }

  #showError(message) {
    this.errorEl.hidden = !message;
    this.errorEl.textContent = message ?? '';
  }

  #showInfo({ width, length, height }) {
    const area = width * length;
    const rows = [
      ['Área del piso', `${area.toLocaleString('es-PE', { maximumFractionDigits: 2 })} m²`],
      ['Perímetro', formatMeters(2 * (width + length))],
      ['Volumen', `${(area * height).toLocaleString('es-PE', { maximumFractionDigits: 2 })} m³`],
    ];
    this.infoEl.innerHTML = rows.map(([k, v]) => `<dt>${k}</dt><dd>${v}</dd>`).join('');
  }
}
