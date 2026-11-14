const fs = require('fs');
const path = require('path');
const puppeteer = require('puppeteer');

const SRC = path.join(__dirname, 'wireframes');
const OUT = process.argv[2] || path.join(__dirname, '..', 'wireframes');
const launch = JSON.parse(fs.readFileSync(path.join(__dirname, 'pp.json'), 'utf8'));

(async () => {
  fs.mkdirSync(OUT, { recursive: true });
  const browser = await puppeteer.launch(launch);
  const page = await browser.newPage();
  await page.setViewport({ width: 1100, height: 800, deviceScaleFactor: 2 });
  for (const f of fs.readdirSync(SRC).filter((x) => x.endsWith('.html')).sort()) {
    await page.goto('file://' + path.join(SRC, f).replace(/\\/g, '/'));
    const el = await page.$('.win');
    const png = path.join(OUT, f.replace('.html', '.png'));
    await el.screenshot({ path: png });
    console.log(png);
  }
  await browser.close();
})().catch((e) => { console.error(e); process.exitCode = 1; });
