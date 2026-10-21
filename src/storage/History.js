// Deshacer / rehacer con "fotos" (snapshots) del proyecto.
//
// Idea: después de cada cambio guardamos el proyecto completo como texto JSON.
// Deshacer = volver a cargar la foto anterior. Es más simple y robusto que
// programar la operación inversa de cada acción, y como el proyecto pesa pocos
// KB, guardar 100 fotos no es problema.
const MAX_STEPS = 100;

export class History {
  /**
   * @param {() => object} capture devuelve el estado actual (objeto plano)
   * @param {(state: object) => void} restore aplica un estado guardado
   * @param {() => void} onChange se llama cuando cambian undo/redo/dirty
   */
  constructor(capture, restore, onChange) {
    Object.assign(this, { capture, restore, onChange });
    // Estado inicial (sin avisar a onChange: quien nos crea aún no terminó de iniciarse).
    this.stack = [JSON.stringify(capture())];
    this.index = 0;
    this.saved = this.stack[0];
  }

  /** Empieza un historial nuevo (al abrir o crear un proyecto). */
  reset() {
    this.stack = [JSON.stringify(this.capture())];
    this.index = 0;
    this.saved = this.stack[0];
    this.onChange();
  }

  /** Registra el estado actual como un paso nuevo (si cambió algo). */
  commit() {
    const snapshot = JSON.stringify(this.capture());
    if (snapshot === this.stack[this.index]) return;
    this.stack.length = this.index + 1; // al hacer un cambio nuevo se descarta el "rehacer"
    this.stack.push(snapshot);
    if (this.stack.length > MAX_STEPS) this.stack.shift();
    this.index = this.stack.length - 1;
    this.onChange();
  }

  /** Registra el estado tras una pausa: agrupa cambios seguidos (p. ej. al elegir un color). */
  commitSoon() {
    clearTimeout(this.timer);
    this.timer = setTimeout(() => {
      this.timer = null;
      this.commit();
    }, 300);
  }

  undo() {
    this.#flush();
    if (this.index === 0) return false;
    this.index--;
    this.restore(JSON.parse(this.stack[this.index]));
    this.onChange();
    return true;
  }

  redo() {
    this.#flush();
    if (this.index >= this.stack.length - 1) return false;
    this.index++;
    this.restore(JSON.parse(this.stack[this.index]));
    this.onChange();
    return true;
  }

  /** Marca el estado actual como "guardado en disco". */
  markSaved() {
    this.#flush();
    this.saved = this.stack[this.index];
    this.onChange();
  }

  get dirty() {
    return this.stack[this.index] !== this.saved;
  }

  get canUndo() {
    return this.index > 0;
  }

  get canRedo() {
    return this.index < this.stack.length - 1;
  }

  // Si hay un commit pendiente (commitSoon), hacerlo ya.
  #flush() {
    if (this.timer) {
      clearTimeout(this.timer);
      this.timer = null;
      this.commit();
    }
  }
}
