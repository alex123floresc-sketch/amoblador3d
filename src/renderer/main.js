// Punto de entrada de la interfaz: une la escena 3D con los controles HTML,
// el catálogo, el guardado de proyectos y el historial (deshacer/rehacer).
import { SceneManager } from '../scene/SceneManager.js';
import { Room, DEFAULT_ROOM } from '../scene/Room.js';
import { LightManager } from '../scene/Lights.js';
import { FurnitureManager } from '../scene/Furniture.js';
import { Interaction } from '../scene/Interaction.js';
import { listEntries, setModelEntries } from '../catalog/catalog.js';
import { loadModel } from '../catalog/models.js';
import { serializeProject, applyProject } from '../storage/project.js';
import { History } from '../storage/History.js';
import { RoomForm } from './ui/RoomForm.js';
import { PropertiesPanel } from './ui/PropertiesPanel.js';
import { ImportDialog } from './ui/ImportDialog.js';

const $ = (id) => document.getElementById(id);
// window.api lo crea preload.cjs. Si no existe (p. ej. abriendo la interfaz en
// un navegador normal), las funciones de archivos no estarán disponibles.
const api = window.api;

// ---------- Escena ----------
const sceneManager = new SceneManager($('viewport'));
const room = new Room(sceneManager.scene);
const lights = new LightManager(sceneManager.scene, room);
const furniture = new FurnitureManager(sceneManager.scene, room);
const project = { room, lights, furniture };
let currentPath = null; // ruta del archivo abierto (null = proyecto nuevo sin guardar)

sceneManager.onUpdate((camera, mode) => room.update(camera, mode));
furniture.onWarning = (message) => toast(message);
furniture.onDuplicate = (item) => duplicate(item);

// ---------- Historial (deshacer / rehacer) ----------
const history = new History(
  () => serializeProject(project),
  (state) => restoreState(state),
  () => updateTitle(),
);

/** Aplica un estado del historial conservando la selección si es posible. */
function restoreState(state) {
  const selectedId = selected?.data.id;
  select(null);
  applyProject(state, project);
  syncUi();
  select(allEntities().find((e) => e.data.id === selectedId) ?? null);
}

// ---------- Selección ----------
let selected = null;

function select(entity) {
  if (selected === entity) return;
  selected?.setSelected(false);
  selected = entity;
  selected?.setSelected(true);
  panel.show(selected);
  renderLists();
}

const panel = new PropertiesPanel($('inspector'), {
  onChanged: () => {
    renderLists();
    history.commitSoon();
  },
  onDelete: (entity) => removeEntity(entity),
});

function removeEntity(entity) {
  if (selected === entity) select(null);
  entity.remove();
  renderLists();
  history.commit();
}

function duplicate(item) {
  if (item?.kind !== 'furniture') return toast('Selecciona un mueble para duplicarlo.');
  handleAdd(furniture.duplicate(item));
}

/** Todas las entidades que existen ahora mismo. */
function allEntities() {
  return [...room.openings, ...lights.items, ...furniture.items];
}

// Cuando cambian paredes o ventanas, la luz solar debe recalcularse.
room.onChange(() => {
  const warnings = lights.refresh();
  if (warnings.length) toast(warnings.join('\n'));
  if (selected && !allEntities().includes(selected)) select(null);
  renderLists();
});

new Interaction({
  sceneManager,
  getRoots: () => [room.group, room.wallGroup, lights.group, furniture.group],
  furniture,
  onSelect: select,
  onDrag: () => panel.refresh(),
  onDragEnd: () => history.commit(),
});

// ---------- Listas de la barra lateral ----------
function renderList(ul, items) {
  ul.innerHTML = '';
  for (const entity of items) {
    const li = document.createElement('li');
    li.textContent = entity.label;
    li.classList.toggle('selected', entity === selected);
    li.addEventListener('click', () => select(entity));
    ul.append(li);
  }
}

function renderLists() {
  renderList($('list-openings'), room.openings);
  renderList($('list-lights'), lights.items);
  renderList($('list-furniture'), furniture.items);
}

/** Muestra el resultado de "agregar": si es texto es un error, si no, lo selecciona. */
function handleAdd(result) {
  if (typeof result === 'string') return toast(result);
  select(result);
  renderLists();
  history.commit();
}

for (const btn of document.querySelectorAll('[data-add-opening]')) {
  btn.addEventListener('click', () => handleAdd(room.addOpening(btn.dataset.addOpening)));
}
for (const btn of document.querySelectorAll('[data-add-light]')) {
  btn.addEventListener('click', () => handleAdd(lights.add(btn.dataset.addLight)));
}

$('ambient').addEventListener('input', (e) => {
  lights.setAmbient(parseFloat(e.target.value));
  $('ambient-value').textContent = e.target.value;
});
$('ambient').addEventListener('change', () => history.commit());

// ---------- Catálogo (Fase 5) ----------
function renderCatalog() {
  const fill = (container, entries) => {
    container.innerHTML = '';
    for (const entry of entries) {
      const btn = document.createElement('button');
      const { width, depth, height } = entry.defaults;
      btn.textContent = `+ ${entry.name}`;
      btn.title = `${entry.name}: ${width} x ${depth} x ${height} m${entry.source === 'user' ? ' (importado)' : ''}`;
      btn.addEventListener('click', () => handleAdd(furniture.add(entry.id)));
      container.append(btn);
    }
  };
  fill($('catalog-parametric'), listEntries('parametric'));
  fill($('catalog-models'), listEntries('model'));
}

/** Lee los catalog.json (incluidos + importados) y actualiza los botones. */
async function reloadCatalog() {
  if (api) {
    try {
      const { entries, warnings } = await api.loadCatalog();
      setModelEntries(entries);
      if (warnings.length) toast(`Catálogo:\n${warnings.join('\n')}`);
    } catch (err) {
      toast(`No se pudo leer el catálogo: ${err.message}`);
    }
  }
  renderCatalog();
}

const importDialog = new ImportDialog($('import-dialog'));

/** Importa un .glb/.gltf: lo copia, pide sus medidas reales y lo agrega al catálogo. */
async function importModel() {
  const imported = await api.importModel();
  if (!imported) return; // canceló el diálogo de archivos
  let scene;
  try {
    scene = await loadModel(imported.url);
  } catch (err) {
    await api.discardModel(imported.folder);
    return toast(`El archivo no es un modelo glTF válido: ${err.message ?? err}`);
  }
  const info = await importDialog.open({ name: imported.name, scene });
  if (!info) return api.discardModel(imported.folder);

  await api.saveCatalogEntry({ id: imported.id, file: imported.file, ...info });
  await reloadCatalog();
  handleAdd(furniture.add(imported.id));
  toast(`"${info.name}" se agregó al catálogo.`, 'info');
}

// ---------- Formulario de la habitación ----------
const roomForm = new RoomForm($('room-form'), (dims) => {
  const warnings = [...room.build(dims), ...lights.fitToRoom(), ...furniture.fitToRoom()];
  if (warnings.length) toast(warnings.join('\n'));
  sceneManager.frame(room.getBounds());
  panel.refresh();
  history.commit();
});

/** Pone los controles HTML de acuerdo con el estado de la escena. */
function syncUi() {
  roomForm.setValues(room.dims);
  $('ambient').value = lights.ambient.intensity;
  $('ambient-value').textContent = lights.ambient.intensity;
  renderLists();
  panel.refresh();
}

// ---------- Proyectos (Fase 6) ----------
function confirmDiscard() {
  return !history.dirty || window.confirm('Hay cambios sin guardar. ¿Descartarlos?');
}

function loadDefaultScene() {
  select(null);
  applyProject({ app: 'Amoblador3D', version: 1, room: DEFAULT_ROOM, ambient: 0.6 }, project);
  room.addOpening('window');
  room.addOpening('door');
  lights.add('point');
  lights.add('sun');
  currentPath = null;
  syncUi();
  sceneManager.frame(room.getBounds());
  history.reset();
}

async function openProject(filePath) {
  if (!confirmDiscard()) return;
  try {
    const result = await api.openProject(filePath);
    if (!result) return;
    select(null);
    const warnings = applyProject(result.data, project);
    currentPath = result.filePath;
    syncUi();
    sceneManager.frame(room.getBounds());
    history.reset();
    toast(warnings.length ? warnings.join('\n') : `Abierto: ${fileName(currentPath)}`, warnings.length ? 'error' : 'info');
  } catch (err) {
    toast(`No se pudo abrir el proyecto: ${err.message}`);
  }
}

/** Guarda. Devuelve true si se guardó. */
async function saveProject(saveAs = false) {
  try {
    const path = await api.saveProject(serializeProject(project), currentPath, saveAs);
    if (!path) return false;
    currentPath = path;
    history.markSaved();
    toast(`Guardado: ${fileName(path)}`, 'info');
    return true;
  } catch (err) {
    toast(`No se pudo guardar: ${err.message}`);
    return false;
  }
}

async function exportImage() {
  // Quitamos la selección para que el contorno azul no salga en la imagen.
  const previous = selected;
  previous?.setSelected(false);
  const dataUrl = sceneManager.screenshot(2);
  previous?.setSelected(true);
  try {
    const path = await api.saveImage(dataUrl);
    if (path) toast(`Imagen guardada: ${fileName(path)}`, 'info');
  } catch (err) {
    toast(`No se pudo guardar la imagen: ${err.message}`);
  }
}

const fileName = (path) => path.split(/[\\/]/).pop();

function updateTitle() {
  const name = currentPath ? fileName(currentPath) : 'Sin título';
  const title = `${name}${history.dirty ? ' •' : ''} — Amoblador3D`;
  document.title = title;
  api?.setState({ dirty: history.dirty, title });
  document.querySelector('[data-action="undo"]').disabled = !history.canUndo;
  document.querySelector('[data-action="redo"]').disabled = !history.canRedo;
}

// ---------- Acciones (menú, barra de herramientas y atajos) ----------
const isTyping = () => document.activeElement?.matches('input, select, textarea');
const FILE_ACTIONS = new Set(['new', 'open', 'save', 'save-as', 'export-image', 'import-model', 'open-models-folder', 'save-and-quit']);

function setView(mode) {
  sceneManager.setMode(mode);
  furniture.setMode(mode);
  document.querySelectorAll('[data-view]').forEach((b) => b.classList.toggle('active', b.dataset.view === mode));
}

const actions = {
  new: () => confirmDiscard() && loadDefaultScene(),
  open: () => openProject(),
  save: () => saveProject(false),
  'save-as': () => saveProject(true),
  'save-and-quit': async () => (await saveProject(false)) && api.quit(),
  'export-image': () => exportImage(),
  'import-model': () => importModel(),
  'open-models-folder': () => api.openModelsFolder(),
  'reload-catalog': () => reloadCatalog().then(() => toast('Catálogo recargado.', 'info')),
  // En un campo de texto, Ctrl+Z debe deshacer lo escrito, no el proyecto.
  undo: () => (isTyping() ? document.execCommand('undo') : history.undo()),
  redo: () => (isTyping() ? document.execCommand('redo') : history.redo()),
  duplicate: () => duplicate(selected),
  delete: () => selected && removeEntity(selected),
  'view-3d': () => setView('3d'),
  'view-plan': () => setView('plan'),
  'toggle-dims': () => {
    $('toggle-dims').checked = !$('toggle-dims').checked;
    room.setDimensionsVisible($('toggle-dims').checked);
  },
  'reset-camera': () => sceneManager.frame(room.getBounds()),
  shortcuts: () => select(null), // el panel vacío muestra la ayuda de atajos
};

function runAction(name) {
  if (FILE_ACTIONS.has(name) && !api) return toast('Esta función solo está disponible en la aplicación de escritorio.');
  actions[name]?.();
}

api?.onMenu(runAction);
api?.onOpenFile((filePath) => openProject(filePath));
for (const btn of document.querySelectorAll('[data-action]')) {
  btn.addEventListener('click', () => runAction(btn.dataset.action));
}
for (const btn of document.querySelectorAll('[data-view]')) {
  btn.addEventListener('click', () => setView(btn.dataset.view));
}
$('toggle-dims').addEventListener('change', (e) => room.setDimensionsVisible(e.target.checked));

// Atajos sin Ctrl (los que llevan Ctrl los maneja el menú nativo).
window.addEventListener('keydown', (e) => {
  if (isTyping() || e.ctrlKey || e.metaKey || e.altKey) return;

  if (e.key === 'Escape') return select(null);
  if (!selected) return;
  if (e.key === 'Delete') return removeEntity(selected);
  if (selected.kind !== 'furniture') return;

  const step = e.shiftKey ? 0.01 : 0.05;
  const { x, z } = selected.data;
  const moves = { ArrowLeft: [-step, 0], ArrowRight: [step, 0], ArrowUp: [0, -step], ArrowDown: [0, step] };
  let error = null;
  if (moves[e.key]) {
    e.preventDefault();
    if (!furniture.moveTo(selected, x + moves[e.key][0], z + moves[e.key][1])) error = 'No se puede mover más en esa dirección.';
  } else if (e.key.toLowerCase() === 'r') error = selected.rotateBy(e.shiftKey ? 90 : -90);
  else if (e.key.toLowerCase() === 'q') error = selected.rotateBy(15);
  else if (e.key.toLowerCase() === 'e') error = selected.rotateBy(-15);
  else return;

  if (error) toast(error);
  panel.refresh();
  history.commitSoon();
});

// ---------- Avisos ----------
let toastTimer;
function toast(message, type = 'error') {
  const el = $('toast');
  el.textContent = message;
  el.className = type;
  el.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => (el.hidden = true), type === 'info' ? 2500 : 5000);
}

// ---------- Arranque ----------
reloadCatalog().then(loadDefaultScene);
