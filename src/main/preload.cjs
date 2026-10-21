// Preload: el ÚNICO puente entre la interfaz y Node.js/Electron.
// La interfaz no puede tocar el disco directamente (por seguridad); en su lugar
// llama a estas funciones, que piden al proceso principal que haga el trabajo.
// (Es .cjs porque los preload con sandbox deben ser CommonJS.)
const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('api', {
  // Proyectos
  openProject: (filePath) => ipcRenderer.invoke('project:open', filePath),
  saveProject: (data, filePath, saveAs) => ipcRenderer.invoke('project:save', { data, filePath, saveAs }),
  saveImage: (dataUrl, filePath) => ipcRenderer.invoke('image:save', dataUrl, filePath),

  // Catálogo de modelos 3D
  loadCatalog: () => ipcRenderer.invoke('catalog:load'),
  importModel: () => ipcRenderer.invoke('catalog:import'),
  discardModel: (folder) => ipcRenderer.invoke('catalog:discard', folder),
  saveCatalogEntry: (entry) => ipcRenderer.invoke('catalog:save-entry', entry),
  openModelsFolder: () => ipcRenderer.invoke('catalog:open-folder'),

  // Ventana
  setState: (state) => ipcRenderer.send('app:state', state),
  quit: () => ipcRenderer.send('app:quit'),
  onMenu: (callback) => ipcRenderer.on('menu', (_event, action) => callback(action)),
  onOpenFile: (callback) => ipcRenderer.on('open-file', (_event, filePath) => callback(filePath)),
});
