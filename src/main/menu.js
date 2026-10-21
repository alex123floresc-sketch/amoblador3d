// Menú nativo de la aplicación. Cada opción envía una "acción" a la interfaz,
// que es quien sabe ejecutarla (guardar, deshacer, cambiar de vista...).
import { app, dialog, Menu } from 'electron';

export function buildMenu(win) {
  const send = (action) => () => win.webContents.send('menu', action);

  const template = [
    {
      label: 'Archivo',
      submenu: [
        { label: 'Nuevo proyecto', accelerator: 'CmdOrCtrl+N', click: send('new') },
        { label: 'Abrir…', accelerator: 'CmdOrCtrl+O', click: send('open') },
        { type: 'separator' },
        { label: 'Guardar', accelerator: 'CmdOrCtrl+S', click: send('save') },
        { label: 'Guardar como…', accelerator: 'CmdOrCtrl+Shift+S', click: send('save-as') },
        { type: 'separator' },
        { label: 'Exportar imagen…', accelerator: 'CmdOrCtrl+E', click: send('export-image') },
        { type: 'separator' },
        { label: 'Importar modelo 3D (.glb/.gltf)…', accelerator: 'CmdOrCtrl+I', click: send('import-model') },
        { label: 'Abrir carpeta de modelos importados', click: send('open-models-folder') },
        { label: 'Recargar catálogo', click: send('reload-catalog') },
        { type: 'separator' },
        { label: 'Salir', role: 'quit' },
      ],
    },
    {
      label: 'Editar',
      submenu: [
        { label: 'Deshacer', accelerator: 'CmdOrCtrl+Z', click: send('undo') },
        { label: 'Rehacer', accelerator: 'CmdOrCtrl+Y', click: send('redo') },
        { label: 'Rehacer', accelerator: 'CmdOrCtrl+Shift+Z', click: send('redo'), visible: false },
        { type: 'separator' },
        { label: 'Duplicar mueble', accelerator: 'CmdOrCtrl+D', click: send('duplicate') },
        { label: 'Eliminar selección   (Supr)', click: send('delete') },
        { type: 'separator' },
        // Roles nativos para que copiar/pegar funcione en los campos de texto.
        { label: 'Cortar', role: 'cut' },
        { label: 'Copiar', role: 'copy' },
        { label: 'Pegar', role: 'paste' },
        { label: 'Seleccionar todo', role: 'selectAll' },
      ],
    },
    {
      label: 'Ver',
      submenu: [
        { label: 'Vista 3D', accelerator: 'CmdOrCtrl+1', click: send('view-3d') },
        { label: 'Planta 2D', accelerator: 'CmdOrCtrl+2', click: send('view-plan') },
        { label: 'Mostrar/ocultar cotas', accelerator: 'CmdOrCtrl+M', click: send('toggle-dims') },
        { label: 'Centrar cámara', accelerator: 'CmdOrCtrl+0', click: send('reset-camera') },
        { type: 'separator' },
        { label: 'Pantalla completa', role: 'togglefullscreen' },
        { label: 'Herramientas de desarrollo', role: 'toggleDevTools' },
      ],
    },
    {
      label: 'Ayuda',
      submenu: [
        { label: 'Atajos de teclado', accelerator: 'F1', click: send('shortcuts') },
        {
          label: 'Acerca de Amoblador3D',
          click: () =>
            dialog.showMessageBox(win, {
              title: 'Acerca de',
              message: `Amoblador3D ${app.getVersion()}`,
              detail: 'Diseño de interiores en 3D con medidas reales.\nElectron + Vite + Three.js',
            }),
        },
      ],
    },
  ];

  Menu.setApplicationMenu(Menu.buildFromTemplate(template));
}
