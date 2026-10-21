// Proceso principal de Electron: crea la ventana, el menú y atiende las
// peticiones de archivos de la interfaz (ver ipc.js).
// En desarrollo (--dev) carga el servidor de Vite (con recarga en caliente);
// en producción carga los archivos compilados de /dist.
import { app, BrowserWindow, dialog, ipcMain } from 'electron';
import path from 'node:path';
import { registerIpc, registerModelProtocol, registerModelSchemeEarly } from './ipc.js';
import { buildMenu } from './menu.js';

const isDev = process.argv.includes('--dev');
let win = null;
let dirty = false; // ¿hay cambios sin guardar? (lo informa la interfaz)

registerModelSchemeEarly();

// Una sola instancia: si se abre un .amob3d con la app ya abierta, se usa esa ventana.
if (!app.requestSingleInstanceLock()) app.quit();

/** Busca un archivo de proyecto en los argumentos (doble clic sobre un .amob3d). */
const projectFromArgs = (argv) => argv.find((arg) => /\.amob3d$/i.test(arg));

function createWindow() {
  win = new BrowserWindow({
    width: 1500,
    height: 920,
    minWidth: 1100,
    minHeight: 650,
    title: 'Amoblador3D',
    backgroundColor: '#1e2227',
    icon: path.join(import.meta.dirname, '../../build/icon.png'),
    webPreferences: {
      preload: path.join(import.meta.dirname, 'preload.cjs'),
      // Seguridad: la interfaz no tiene acceso directo a Node.js.
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
    },
  });

  buildMenu(win);

  // Avisar si se intenta cerrar con cambios sin guardar.
  win.on('close', (event) => {
    if (!dirty) return;
    const choice = dialog.showMessageBoxSync(win, {
      type: 'question',
      buttons: ['Guardar', 'Salir sin guardar', 'Cancelar'],
      defaultId: 0,
      cancelId: 2,
      title: 'Cambios sin guardar',
      message: 'El proyecto tiene cambios sin guardar.',
      detail: '¿Quieres guardarlos antes de salir?',
    });
    if (choice === 1) return; // salir sin guardar
    event.preventDefault();
    if (choice === 0) win.webContents.send('menu', 'save-and-quit');
  });

  win.webContents.once('did-finish-load', () => {
    const file = projectFromArgs(process.argv);
    if (file) win.webContents.send('open-file', file);
  });

  if (isDev) {
    win.loadURL('http://127.0.0.1:5173');
    win.webContents.openDevTools({ mode: 'detach' });
  } else {
    win.loadFile(path.join(import.meta.dirname, '../../dist/index.html'));
  }
}

// La interfaz nos informa del título y de si hay cambios sin guardar.
ipcMain.on('app:state', (_e, state) => {
  dirty = state.dirty;
  win?.setTitle(state.title);
  win?.setDocumentEdited(state.dirty); // macOS: punto en el botón de cerrar
});
ipcMain.on('app:quit', () => {
  dirty = false;
  win?.close();
});

app.on('second-instance', (_e, argv) => {
  if (!win) return;
  if (win.isMinimized()) win.restore();
  win.focus();
  const file = projectFromArgs(argv);
  if (file) win.webContents.send('open-file', file);
});

app.whenReady().then(() => {
  registerModelProtocol();
  registerIpc(() => win);
  createWindow();
  // macOS: volver a abrir ventana al hacer clic en el dock.
  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});
