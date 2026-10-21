// Genera build/icon.png (512x512) dibujándolo con Canvas en una ventana invisible de Electron.
// Ejecutar con:  npm run icon
// electron-builder convierte este PNG al formato .ico de Windows automáticamente.
import { app, BrowserWindow } from 'electron';
import fs from 'node:fs';
import path from 'node:path';

const draw = () => {
  const c = document.createElement('canvas');
  c.width = c.height = 512;
  const g = c.getContext('2d');
  // Fondo: cuadrado redondeado azul.
  const bg = g.createLinearGradient(0, 0, 512, 512);
  bg.addColorStop(0, '#4f9cf9');
  bg.addColorStop(1, '#2a5fb0');
  g.fillStyle = bg;
  g.beginPath();
  g.roundRect(16, 16, 480, 480, 96);
  g.fill();
  // Habitación en perspectiva isométrica: piso y dos paredes.
  const poly = (color, pts) => {
    g.fillStyle = color;
    g.beginPath();
    pts.forEach(([x, y], i) => (i ? g.lineTo(x, y) : g.moveTo(x, y)));
    g.closePath();
    g.fill();
  };
  poly('#c49a6c', [[256, 420], [430, 320], [256, 220], [82, 320]]); // piso
  poly('#f2efe9', [[82, 320], [256, 220], [256, 90], [82, 190]]); // pared izquierda
  poly('#d9d4cb', [[256, 220], [430, 320], [430, 190], [256, 90]]); // pared derecha
  poly('#bfe3ff', [[300, 200], [380, 246], [380, 186], [300, 140]]); // ventana
  // Un mueble (caja) sobre el piso.
  poly('#8b6b4a', [[200, 350], [270, 310], [270, 270], [200, 310]]);
  poly('#6d4c33', [[200, 350], [150, 322], [150, 282], [200, 310]]);
  poly('#a0764f', [[150, 282], [200, 310], [270, 270], [220, 242]]);
  return c.toDataURL('image/png');
};

app.whenReady().then(async () => {
  const win = new BrowserWindow({ show: false });
  await win.loadURL('data:text/html,<html></html>');
  const dataUrl = await win.webContents.executeJavaScript(`(${draw.toString()})()`);
  const out = path.join(import.meta.dirname, '..', 'build', 'icon.png');
  fs.mkdirSync(path.dirname(out), { recursive: true });
  fs.writeFileSync(out, Buffer.from(dataUrl.split(',')[1], 'base64'));
  console.log('✓ build/icon.png');
  app.quit();
});
