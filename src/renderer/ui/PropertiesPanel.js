// Panel de propiedades genérico: dibuja un formulario a partir de los campos
// que declara la entidad seleccionada (entity.fields()). Así el mismo panel
// sirve para ventanas, puertas, luces y muebles.
//
// Tipos de campo: number, select, color, checkbox.

export class PropertiesPanel {
  /**
   * @param {HTMLElement} el contenedor del panel
   * @param {object} callbacks
   * @param {() => void} callbacks.onChanged se llama tras un cambio exitoso
   * @param {(entity) => void} callbacks.onDelete
   */
  constructor(el, { onChanged, onDelete }) {
    this.el = el;
    this.onChanged = onChanged;
    this.onDelete = onDelete;
    this.entity = null;
    this.show(null);
  }

  /** Muestra las propiedades de una entidad (o un mensaje si es null). */
  show(entity) {
    this.entity = entity;
    this.signature = null;
    this.el.innerHTML = '';

    if (!entity) {
      this.el.innerHTML = `
        <h2>Propiedades</h2>
        <p class="hint">Haz clic sobre un elemento en la vista o en las listas de la izquierda para editarlo.</p>
        <p class="hint"><b>Atajos:</b><br>
          Clic + arrastrar: mover mueble<br>Supr: eliminar · Esc: deseleccionar<br>R / Shift+R: girar mueble 90°<br>
          Q / E: girar 15°<br>Flechas: mover 5 cm (Shift: 1 cm)<br>
          Ctrl+D: duplicar mueble<br>Ctrl+Z / Ctrl+Y: deshacer / rehacer<br>
          Ctrl+S: guardar · Ctrl+O: abrir<br>Ctrl+E: exportar imagen<br>Ctrl+1 / Ctrl+2: vista 3D / planta</p>`;
      return;
    }

    this.titleEl = document.createElement('h2');
    this.form = document.createElement('div');
    this.form.className = 'props';
    this.errorEl = document.createElement('p');
    this.errorEl.className = 'error';
    this.errorEl.hidden = true;
    this.el.append(this.titleEl, this.form, this.errorEl);

    if (entity.actions) {
      const row = document.createElement('div');
      row.className = 'button-row';
      for (const action of entity.actions()) {
        const btn = document.createElement('button');
        btn.textContent = action.label;
        btn.title = action.title ?? '';
        btn.addEventListener('click', () => this.#report(action.run()));
        row.append(btn);
      }
      this.el.append(row);
    }

    const del = document.createElement('button');
    del.className = 'danger';
    del.textContent = 'Eliminar (Supr)';
    del.addEventListener('click', () => this.onDelete(entity));
    this.el.append(del);

    this.refresh();
  }

  /**
   * Actualiza los valores mostrados (por ejemplo, mientras se arrastra un mueble).
   * Solo vuelve a crear los campos si cambió la lista de campos; así no se
   * pierde el foco al pasar de un campo a otro con Tab.
   */
  refresh() {
    const entity = this.entity;
    if (!entity) return;
    this.titleEl.textContent = entity.label;
    const fields = entity.fields();
    const signature = JSON.stringify(fields);
    if (signature !== this.signature) {
      this.signature = signature;
      this.form.innerHTML = '';
      this.inputs = {};
      for (const field of fields) this.form.append(this.#createRow(field));
    }
    const values = entity.values();
    for (const field of fields) {
      const input = this.inputs[field.key];
      if (field.type === 'checkbox') input.checked = values[field.key];
      else input.value = values[field.key];
    }
  }

  #createRow(field) {
    const label = document.createElement('label');
    label.className = field.type === 'checkbox' ? 'check' : '';
    const text = document.createElement('span');
    text.textContent = field.label;

    let input;
    if (field.type === 'select') {
      input = document.createElement('select');
      for (const [value, name] of field.options) input.add(new Option(name, value));
    } else {
      input = document.createElement('input');
      input.type = field.type;
      if (field.type === 'number') Object.assign(input, { step: field.step, min: field.min, max: field.max });
    }
    this.inputs[field.key] = input;

    // "change" se dispara al confirmar (Enter o salir del campo); para el color
    // usamos "input" y así se ve el cambio en vivo mientras se elige.
    const eventName = field.type === 'color' ? 'input' : 'change';
    input.addEventListener(eventName, () => {
      let value = input.value;
      if (field.type === 'number') value = parseFloat(value);
      if (field.type === 'checkbox') value = input.checked;
      this.#report(this.entity.update({ [field.key]: value }));
    });

    if (field.type === 'checkbox') {
      label.append(input, text);
    } else {
      const wrap = document.createElement('span');
      wrap.className = 'input-unit';
      wrap.append(input);
      if (field.unit) wrap.append(` ${field.unit}`);
      label.append(text, wrap);
    }
    return label;
  }

  // Muestra el error (si lo hubo) y vuelve a sincronizar los valores.
  #report(error) {
    this.errorEl.hidden = !error;
    this.errorEl.textContent = error ?? '';
    this.refresh();
    if (!error) this.onChanged();
  }
}
