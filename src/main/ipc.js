// Trabajo con archivos que la interfaz pide al proceso principal (vía preload.cjs):
// proyectos, capturas de imagen y catálogo de modelos 3D.
import { app, dialog, ipcMain, protocol, shell } from 'electron';
import fs from 'node:fs/promises';
import path from 'node:path';

// ---------- Carpetas del catálogo ----------
// builtin: modelos que vienen con la app (carpeta assets/ del proyecto).
// user:    modelos importados por el usuario (carpeta de datos del usuario).
export function catalogRoots() {
  // AMOB_ASSETS_DIR permite usar otra carpeta (útil para pruebas automáticas).
  const builtin =
    process.env.AMOB_ASSETS_DIR ?? (app.isPackaged ? path.join(process.resourcesPath, 'assets') : path.join(app.getAppPath(), 'assets'));
  const user = path.join(app.getPath('userData'), 'catalog');
  return { builtin, user };
}

const MIME = {
  '.glb': 'model/gltf-binary',
  '.gltf': 'model/gltf+json',
  '.bin': 'application/octet-stream',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.webp': 'image/webp',
  '.ktx2': 'image/ktx2',
};

/**
 * Protocolo propio "amob://": amob://builtin/models/sofa.glb -> assets/models/sofa.glb
 * Así GLTFLoader puede cargar el modelo Y sus archivos relativos (.bin, texturas)
 * igual en desarrollo y en la app instalada.
 * Debe llamarse ANTES de que la app esté lista (registerSchemesAsPrivileged).
 */
export function registerModelSchemeEarly() {
  protocol.registerSchemesAsPrivileged([
    { scheme: 'amob', privileges: { standard: true, secure: true, supportFetchAPI: true, corsEnabled: true } },
  ]);
}

export function registerModelProtocol() {
  protocol.handle('amob', async (request) => {
    const url = new URL(request.url);
    const root = catalogRoots()[url.host];
    if (!root) return new Response('Origen desconocido', { status: 404 });
    // path.resolve normaliza la ruta (barras, mayúsculas de unidad, '..').
    const filePath = path.resolve(root, '.' + decodeURIComponent(url.pathname));
    // Seguridad: no permitir salir de la carpeta raíz con "../"
    const rel = path.relative(path.resolve(root), filePath);
    if (rel.startsWith('..') || path.isAbsolute(rel)) return new Response('Prohibido', { status: 403 });
    try {
      const body = await fs.readFile(filePath);
      const type = MIME[path.extname(filePath).toLowerCase()] ?? 'application/octet-stream';
      return new Response(body, { headers: { 'content-type': type, 'access-control-allow-origin': '*' } });
    } catch {
      return new Response('No encontrado', { status: 404 });
    }
  });
}

// ---------- Utilidades ----------
const PROJECT_FILTERS = [
  { name: 'Proyecto Amoblador3D', extensions: ['amob3d'] },
  { name: 'JSON', extensions: ['json'] },
];

async function readJson(file, fallback) {
  try {
    return JSON.parse(await fs.readFile(file, 'utf8'));
  } catch (err) {
    if (err.code === 'ENOENT' && fallback !== undefined) return fallback;
    throw err;
  }
}

/** "Sofá Moderno.glb" -> "sofa-moderno" */
function slugify(text) {
  return text
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '') || 'modelo';
}

/** Lee un catalog.json y devuelve las entradas válidas con su URL amob://. */
async function readCatalog(rootName, root, warnings) {
  const file = path.join(root, 'catalog.json');
  let json;
  try {
    json = await readJson(file, { models: [] });
  } catch (err) {
    warnings.push(`catalog.json (${rootName}) tiene un error de formato: ${err.message}`);
    return [];
  }
  const entries = [];
  for (const m of json.models ?? []) {
    const dims = [m.width, m.depth, m.height];
    if (typeof m.id !== 'string' || typeof m.name !== 'string' || typeof m.file !== 'string') {
      warnings.push(`Entrada inválida en catalog.json (${rootName}): necesita id, name y file.`);
      continue;
    }
    if (!dims.every((v) => typeof v === 'number' && v > 0)) {
      warnings.push(`"${m.name}": width, depth y height deben ser números mayores que 0 (en metros).`);
      continue;
    }
    try {
      await fs.access(path.join(root, m.file));
    } catch {
      warnings.push(`"${m.name}": no se encontró el archivo ${m.file}.`);
      continue;
    }
    const url = `amob://${rootName}/${m.file.split(/[\\/]/).map(encodeURIComponent).join('/')}`;
    entries.push({ ...m, url, source: rootName });
  }
  return entries;
}

/** Copia un .gltf junto con sus archivos externos (.bin, texturas) a `dest`. */
async function copyGltfWithResources(src, dest) {
  const json = await readJson(src);
  const uris = [...(json.buffers ?? []), ...(json.images ?? [])]
    .map((x) => x.uri)
    .filter((uri) => uri && !uri.startsWith('data:'));
  await fs.copyFile(src, path.join(dest, path.basename(src)));
  for (const uri of uris) {
    const rel = decodeURIComponent(uri);
    const target = path.join(dest, rel);
    if (!target.startsWith(dest + path.sep)) throw new Error(`Ruta no permitida dentro del .gltf: ${uri}`);
    await fs.mkdir(path.dirname(target), { recursive: true });
    await fs.copyFile(path.join(path.dirname(src), rel), target);
  }
}

// ---------- Manejadores IPC ----------
export function registerIpc(getWindow) {
  // Abrir proyecto: con ruta (al abrir con doble clic) o mostrando el diálogo.
  ipcMain.handle('project:open', async (_e, filePath) => {
    if (!filePath) {
      const res = await dialog.showOpenDialog(getWindow(), { title: 'Abrir proyecto', filters: PROJECT_FILTERS, properties: ['openFile'] });
      if (res.canceled) return null;
      filePath = res.filePaths[0];
    }
    return { filePath, data: await readJson(filePath) };
  });

  // Guardar proyecto: pide ruta si es nuevo o si es "Guardar como".
  ipcMain.handle('project:save', async (_e, { data, filePath, saveAs }) => {
    if (!filePath || saveAs) {
      const res = await dialog.showSaveDialog(getWindow(), {
        title: 'Guardar proyecto',
        defaultPath: filePath ?? 'mi-habitacion.amob3d',
        filters: PROJECT_FILTERS,
      });
      if (res.canceled) return null;
      filePath = res.filePath;
    }
    if (!/\.(amob3d|json)$/i.test(filePath)) filePath += '.amob3d';
    await fs.writeFile(filePath, JSON.stringify(data, null, 2), 'utf8');
    return filePath;
  });

  // Guardar captura PNG (recibe la imagen como data URL "data:image/png;base64,...").
  ipcMain.handle('image:save', async (_e, dataUrl, filePath) => {
    if (!filePath) {
      const res = await dialog.showSaveDialog(getWindow(), {
        title: 'Exportar imagen',
        defaultPath: 'captura-amoblador3d.png',
        filters: [{ name: 'Imagen PNG', extensions: ['png'] }],
      });
      if (res.canceled) return null;
      filePath = res.filePath;
    }
    if (!/\.png$/i.test(filePath)) filePath += '.png';
    await fs.writeFile(filePath, Buffer.from(dataUrl.split(',')[1], 'base64'));
    return filePath;
  });

  // Catálogo: une los modelos incluidos y los importados por el usuario.
  ipcMain.handle('catalog:load', async () => {
    const roots = catalogRoots();
    const warnings = [];
    const entries = [...(await readCatalog('builtin', roots.builtin, warnings)), ...(await readCatalog('user', roots.user, warnings))];
    return { entries, warnings, folders: roots };
  });

  // Importar: el usuario elige un .glb/.gltf y lo copiamos a su carpeta de modelos.
  ipcMain.handle('catalog:import', async () => {
    const res = await dialog.showOpenDialog(getWindow(), {
      title: 'Importar modelo 3D',
      filters: [{ name: 'Modelos glTF', extensions: ['glb', 'gltf'] }],
      properties: ['openFile'],
    });
    if (res.canceled) return null;
    const src = res.filePaths[0];
    const name = path.basename(src, path.extname(src));
    const slug = `${slugify(name)}-${Date.now().toString(36)}`;
    const folder = path.join(catalogRoots().user, 'models', slug);
    await fs.mkdir(folder, { recursive: true });
    try {
      if (src.toLowerCase().endsWith('.gltf')) await copyGltfWithResources(src, folder);
      else await fs.copyFile(src, path.join(folder, path.basename(src)));
    } catch (err) {
      await fs.rm(folder, { recursive: true, force: true });
      throw new Error(`No se pudo copiar el modelo: ${err.message}`);
    }
    const file = `models/${slug}/${path.basename(src)}`;
    return {
      id: `user-${slug}`,
      name,
      file,
      folder: slug,
      url: `amob://user/${file.split('/').map(encodeURIComponent).join('/')}`,
    };
  });

  // Si el usuario cancela la importación, borramos la copia.
  ipcMain.handle('catalog:discard', async (_e, folder) => {
    if (!/^[a-z0-9-]+$/.test(folder)) return;
    await fs.rm(path.join(catalogRoots().user, 'models', folder), { recursive: true, force: true });
  });

  // Agregar la entrada importada al catalog.json del usuario.
  ipcMain.handle('catalog:save-entry', async (_e, entry) => {
    const file = path.join(catalogRoots().user, 'catalog.json');
    const json = await readJson(file, { models: [] });
    json.models = (json.models ?? []).filter((m) => m.id !== entry.id);
    json.models.push(entry);
    await fs.mkdir(path.dirname(file), { recursive: true });
    await fs.writeFile(file, JSON.stringify(json, null, 2), 'utf8');
    return entry;
  });

  ipcMain.handle('catalog:open-folder', async () => {
    const dir = catalogRoots().user;
    await fs.mkdir(path.join(dir, 'models'), { recursive: true });
    await shell.openPath(dir);
  });
}
