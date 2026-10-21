import { defineConfig } from 'vite';

// Vite sirve y compila solo la interfaz (renderer).
// root: carpeta donde está index.html; base './' para que el build
// funcione al cargarse con file:// desde Electron.
export default defineConfig({
  root: 'src/renderer',
  base: './',
  server: { host: '127.0.0.1', port: 5173, strictPort: true },
  build: { outDir: '../../dist', emptyOutDir: true, chunkSizeWarningLimit: 1000 }, // Three.js pesa ~600 KB
});
