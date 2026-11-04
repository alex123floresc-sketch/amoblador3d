// Convierte documentacion/*.md en documentos Word con formato APA 7 (carpeta "doc 2").
// Uso: node generar.js <carpeta documentacion> <carpeta salida>
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');
const { marked } = require('marked');
const {
  Document, Packer, Paragraph, TextRun, HeadingLevel, AlignmentType, Table, TableRow, TableCell,
  WidthType, BorderStyle, ShadingType, ImageRun, PageBreak, Header, PageNumber, TableOfContents,
  LevelFormat, VerticalAlign, ExternalHyperlink, PageOrientation,
} = require('docx');

const SRC = process.argv[2];
const OUT = process.argv[3];
const WORK = path.join(__dirname, 'tmp-' + Date.now());
fs.mkdirSync(OUT, { recursive: true });
fs.mkdirSync(WORK, { recursive: true });

const FONT = 'Times New Roman';
const MONO = 'Consolas';
const CONTENT_W = 11906 - 2 * 1440; // A4 menos márgenes de 2,54 cm (DXA)
const INDENT = 720; // 1,27 cm

// ------------------------------------------------------------------ texto
// Emojis y símbolos que Times New Roman no tiene: se quitan (los nombres de botones quedan igual).
const EMOJI = /[\u{1F000}-\u{1FAFF}\u{2600}-\u{27BF}\u{2B00}-\u{2BFF}\u{FE0F}\u{200D}\u{2300}-\u{23FF}\u{2900}-\u{297F}\u{25A0}-\u{25FF}\u{2700}-\u{27BF}]/gu;
function clean(t) {
  return t
    .replace(/✅/g, 'Sí')
    .replace(/＋/g, '+')
    .replace(EMOJI, '')
    .replace(/&nbsp;/g, ' ')
    .replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&#39;/g, "'")
    .replace(/ {2,}/g, ' ');
}

function runs(tokens, style = {}) {
  const out = [];
  for (const t of tokens || []) {
    switch (t.type) {
      case 'strong': out.push(...runs(t.tokens, { ...style, bold: true })); break;
      case 'em': out.push(...runs(t.tokens, { ...style, italics: true })); break;
      case 'del': out.push(...runs(t.tokens, { ...style, strike: true })); break;
      case 'codespan': out.push(text(t.text, { ...style, code: true })); break;
      case 'br': out.push(new TextRun({ break: 1 })); break;
      case 'link': {
        const inner = runs(t.tokens, style);
        if (/^https?:/.test(t.href)) {
          out.push(new ExternalHyperlink({ link: t.href, children: runs(t.tokens, { ...style, link: true }) }));
        } else out.push(...inner); // enlaces entre documentos: solo el texto
        break;
      }
      case 'text':
        if (t.tokens && t.tokens.length) out.push(...runs(t.tokens, style));
        else out.push(text(t.text, style));
        break;
      case 'escape': out.push(text(t.text, style)); break;
      case 'html': break;
      default: if (t.text) out.push(text(t.text, style));
    }
  }
  return out;
}

function text(raw, s) {
  const value = s.code ? raw.replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&amp;/g, '&') : clean(raw);
  const pending = /completar/i.test(value) || (s.code && /^\[[^\]]*\]$/.test(value.trim()) && /\s/.test(value));
  return new TextRun({
    text: value,
    bold: s.bold, italics: s.italics, strike: s.strike,
    font: s.code && !pending ? MONO : (s.font || FONT),
    size: s.size || (s.code && !pending ? (s.inTable ? 18 : 20) : undefined),
    highlight: pending ? 'yellow' : undefined,
    color: s.link ? '1F4E79' : undefined,
    underline: s.link ? {} : undefined,
  });
}

const plain = (tokens) => (tokens || []).map((t) => (t.tokens ? plain(t.tokens) : t.text || '')).join('');

// ------------------------------------------------------------------ diagramas
let diagramCount = 0;
function mermaidPng(code) {
  const id = `d${++diagramCount}`;
  const mmd = path.join(WORK, id + '.mmd'), png = path.join(WORK, id + '.png');
  fs.writeFileSync(mmd, code);
  const cli = path.join(__dirname, 'node_modules', '@mermaid-js', 'mermaid-cli', 'src', 'cli.js');
  execFileSync(process.execPath, [cli, '-i', mmd, '-o', png, '-p', path.join(__dirname, 'pp.json'),
    '-s', '2', '-b', 'white', '-q', '-c', path.join(__dirname, 'mm.json'), '--size', '3000'], { stdio: 'pipe' });
  const data = fs.readFileSync(png);
  return { data, w: data.readUInt32BE(16), h: data.readUInt32BE(20) };
}

// ------------------------------------------------------------------ bloques
function makeConverter(opts) {
  let tables = 0, figures = 0, lastHeading = '', inReferences = false;
  const body = [];
  const lineSpacing = opts.double ? 480 : 360;

  const para = (children, extra = {}) =>
    new Paragraph({ children, spacing: { line: lineSpacing, before: 0, after: 0 }, indent: { firstLine: INDENT }, ...extra });

  function caption(kind, n, title) {
    body.push(new Paragraph({ keepNext: true, spacing: { before: 240, after: 0, line: lineSpacing },
      children: [new TextRun({ text: `${kind} ${n}`, bold: true, font: FONT })] }));
    body.push(new Paragraph({ keepNext: true, spacing: { after: 120, line: lineSpacing },
      children: [new TextRun({ text: title, italics: true, font: FONT })] }));
  }

  function table(t) {
    tables++;
    caption('Tabla', tables, lastHeading || opts.title);
    const cols = t.header.length;
    const lens = t.header.map((h, i) => Math.max(plain(h.tokens).length,
      ...t.rows.map((r) => Math.min(plain(r[i] && r[i].tokens).length, 70)), 4));
    const total = lens.reduce((a, b) => a + b, 0);
    // Mínimo por columna: que quepa su palabra más larga (fechas, códigos) sin partirse.
    const longest = t.header.map((h, i) => Math.max(...[h, ...t.rows.map((r) => r[i])]
      .map((c) => Math.max(0, ...clean(plain(c && c.tokens)).split(/\s+/).map((wd) => wd.length)))));
    const mins = longest.map((l) => Math.max(700, Math.min(2400, l * 100 + 200)));
    // Primero el mínimo de cada columna; lo que sobra se reparte según cuánto texto tiene.
    const minSum = mins.reduce((a, b) => a + b, 0);
    let widths;
    if (minSum >= CONTENT_W) widths = mins.map((m) => Math.floor(m * CONTENT_W / minSum));
    else {
      const extra = lens.map((l, i) => Math.max(0, (l / total) * CONTENT_W - mins[i]));
      const extraSum = extra.reduce((a, b) => a + b, 0) || 1;
      widths = mins.map((m, i) => Math.floor(m + (extra[i] / extraSum) * (CONTENT_W - minSum)));
    }
    widths[cols - 1] += CONTENT_W - widths.reduce((a, b) => a + b, 0);

    const line = { style: BorderStyle.SINGLE, size: 6, color: '000000' };
    const none = { style: BorderStyle.NONE, size: 0, color: 'FFFFFF' };
    const cell = (tokens, i, header, last) => new TableCell({
      width: { size: widths[i], type: WidthType.DXA },
      verticalAlign: VerticalAlign.CENTER,
      margins: { top: 40, bottom: 40, left: 80, right: 80 },
      borders: { top: header ? line : none, bottom: header || last ? line : none, left: none, right: none },
      children: [new Paragraph({ spacing: { line: 240, before: 0, after: 0 },
        children: runs(tokens, { size: 20, inTable: true, bold: header || undefined }) })],
    });
    body.push(new Table({
      width: { size: CONTENT_W, type: WidthType.DXA }, columnWidths: widths,
      rows: [
        new TableRow({ tableHeader: true, children: t.header.map((h, i) => cell(h.tokens, i, true, false)) }),
        ...t.rows.map((r, ri) => new TableRow({ cantSplit: true,
          children: r.map((c, i) => cell(c.tokens, i, false, ri === t.rows.length - 1)) })),
      ],
    }));
    body.push(new Paragraph({ spacing: { after: 240 }, children: [] }));
  }

  // Captura de pantalla: Figura N, título en cursiva, imagen y nota (APA 7).
  function picture(img) {
    figures++;
    caption('Figura', figures, clean(img.text || lastHeading));
    const data = fs.readFileSync(path.join(SRC, decodeURI(img.href)));
    const w = data.readUInt32BE(16), h = data.readUInt32BE(20);
    const maxW = opts.double ? 470 : 600; // con interlineado doble caben dos capturas por página
    const k = Math.min(1, maxW / w, 700 / h);
    body.push(new Paragraph({ alignment: AlignmentType.CENTER, keepNext: true, spacing: { after: 60 },
      children: [new ImageRun({ type: 'png', data, transformation: { width: Math.round(w * k), height: Math.round(h * k) } })] }));
    body.push(new Paragraph({ spacing: { after: 240, line: lineSpacing }, children: [
      new TextRun({ text: 'Nota.', italics: true, font: FONT }),
      new TextRun({ text: ' Captura de pantalla de Amoblador3D 2.0.0. Elaboración propia.', font: FONT })] }));
  }

  function code(t) {
    if (t.lang === 'mermaid') {
      figures++;
      const img = mermaidPng(t.text);
      let w = img.w / 2, h = img.h / 2; // se dibujó a escala 2
      // Un diagrama muy ancho se vería diminuto en vertical: va solo en una página horizontal.
      const wide = w > 640 && w / h > 1.3;
      const start = body.length;
      caption('Figura', figures, lastHeading || opts.title);
      const maxW = wide ? 900 : 600, maxH = wide ? 410 : 740;
      const k = Math.min(1, maxW / w, maxH / h);
      body.push(new Paragraph({ alignment: AlignmentType.CENTER, keepNext: true, spacing: { after: 60 },
        children: [new ImageRun({ type: 'png', data: img.data, transformation: { width: Math.round(w * k), height: Math.round(h * k) } })] }));
      body.push(new Paragraph({ spacing: { after: 240, line: lineSpacing }, children: [
        new TextRun({ text: 'Nota.', italics: true, font: FONT }),
        new TextRun({ text: ' Elaboración propia.', font: FONT })] }));
      if (wide) body.push({ landscape: body.splice(start) });
      return;
    }
    const lines = t.text.split('\n');
    lines.forEach((l, i) => body.push(new Paragraph({
      shading: { type: ShadingType.CLEAR, fill: 'F2F2F2', color: 'auto' },
      spacing: { line: 240, before: i === 0 ? 120 : 0, after: i === lines.length - 1 ? 200 : 0 },
      indent: { left: 240, right: 240 },
      children: [new TextRun({ text: l || ' ', font: MONO, size: 18 })],
    })));
  }

  function list(t, level) {
    for (const item of t.items) {
      const inlineTokens = [];
      const nested = [];
      for (const tk of item.tokens) {
        if (tk.type === 'list') nested.push(tk);
        else if (tk.type === 'text' || tk.type === 'paragraph') inlineTokens.push(...(tk.tokens || [{ type: 'text', text: tk.text }]));
        else if (tk.type === 'checkbox') { /* se dibuja abajo */ }
      }
      const children = runs(inlineTokens);
      if (item.task) children.unshift(new TextRun({ text: (item.checked ? '☑' : '☐') + ' ', font: 'Segoe UI Symbol' }));
      body.push(new Paragraph({
        numbering: item.task ? undefined : { reference: t.ordered ? opts.numRef() : 'bullets', level },
        indent: item.task ? { left: INDENT * (level + 1) } : undefined,
        spacing: { line: lineSpacing, before: 0, after: 0 },
        children,
      }));
      nested.forEach((n) => list(n, Math.min(level + 1, 2)));
    }
    body.push(new Paragraph({ spacing: { after: 120 }, children: [] }));
  }

  function heading(t) {
    const title = clean(plain(t.tokens)).trim();
    inReferences = /^Referencias$/i.test(title);
    lastHeading = title.replace(/^[\d.]+\s+/, '').replace(/^CU-\d+\s+/, '');
    if (t.depth === 1) return; // va en la carátula
    const level = { 2: HeadingLevel.HEADING_1, 3: HeadingLevel.HEADING_2, 4: HeadingLevel.HEADING_3 }[t.depth] || HeadingLevel.HEADING_3;
    const first = t.depth === 2 && opts.newPagePerChapter && body.length > 0;
    body.push(new Paragraph({ heading: level, pageBreakBefore: first, children: runs(t.tokens, { bold: true, italics: t.depth >= 4 || undefined }) }));
  }

  function block(t) {
    switch (t.type) {
      case 'heading': heading(t); break;
      case 'paragraph':
        if (t.tokens.length && t.tokens.every((x) => x.type === 'image' || (x.type === 'text' && !x.text.trim()))) {
          t.tokens.filter((x) => x.type === 'image').forEach(picture);
          break;
        }
        body.push(inReferences
        ? para(runs(t.tokens), { indent: { left: INDENT, hanging: INDENT }, spacing: { line: 480, before: 0, after: 0 } })
        : para(runs(t.tokens))); break;
      case 'table': table(t); break;
      case 'code': code(t); break;
      case 'list': list(t, 0); break;
      case 'blockquote':
        for (const inner of t.tokens) {
          if (inner.type === 'paragraph') body.push(new Paragraph({ indent: { left: INDENT, right: INDENT },
            spacing: { line: lineSpacing, before: 120, after: 120 }, children: runs(inner.tokens, { italics: true }) }));
          else block(inner);
        }
        break;
      case 'hr': if (opts.hrIsPageBreak) body.push(new Paragraph({ children: [new PageBreak()] })); break;
      default: break;
    }
  }
  return { block, body, counts: () => ({ tables, figures }) };
}

// ------------------------------------------------------------------ documento
const META = {
  universidad: 'UNIVERSIDAD NACIONAL DE JULIACA',
  escuela: 'Escuela Profesional de [completar]',
  proyecto: 'Amoblador3D: diseñador de interiores 3D con medidas reales',
};

function cover(title, subtitle) {
  const c = (txt, o = {}) => new Paragraph({ alignment: AlignmentType.CENTER, spacing: { line: 480, before: o.before || 0 },
    children: [text(txt, { bold: o.bold, size: o.size })] });
  return [
    c(META.universidad, { bold: true, size: 28 }),
    c(META.escuela),
    c(title, { bold: true, size: 28, before: 1800 }),
    ...(subtitle ? [c(subtitle, { size: 24 })] : []),
    ...(subtitle ? [] : [c('Proyecto: ' + META.proyecto, { before: 240 })]),
    c('Curso: [completar]', { before: 1200 }),
    c('Docente: [completar]'),
    c('Integrantes:'),
    c('[Apellidos y nombres — código]'),
    c('[Apellidos y nombres — código]'),
    c('Juliaca – Perú', { before: 1200 }),
    c('Noviembre de 2026'),
    new Paragraph({ children: [new PageBreak()] }),
  ];
}

function toc() {
  return [
    new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 240 }, children: [new TextRun({ text: 'Índice', bold: true, font: FONT, size: 24 })] }),
    new TableOfContents('Índice', { hyperlink: true, headingStyleRange: '1-3' }),
    new Paragraph({ spacing: { before: 240 }, children: [new TextRun({ text: 'Si el índice aparece vacío: clic derecho sobre él → Actualizar campos → Actualizar toda la tabla.', italics: true, size: 18, font: FONT, color: '7F7F7F' })] }),
    new Paragraph({ children: [new PageBreak()] }),
  ];
}

function headingStyle(id, name, size, opts) {
  return { id, name, basedOn: 'Normal', next: 'Normal', quickFormat: true,
    run: { font: FONT, size, bold: true, italics: opts.italics, color: '000000' },
    paragraph: { alignment: opts.center ? AlignmentType.CENTER : AlignmentType.LEFT, keepNext: true, keepLines: true,
      spacing: { before: opts.before, after: 120, line: 360 }, outlineLevel: opts.outline } };
}

/** Parte el contenido en secciones: vertical normal y una horizontal por cada diagrama ancho. */
function sections(children) {
  const header = () => ({ default: new Header({ children: [new Paragraph({ alignment: AlignmentType.RIGHT,
    children: [new TextRun({ children: [PageNumber.CURRENT], font: FONT, size: 24 })] })] }) });
  const margin = { top: 1440, bottom: 1440, left: 1440, right: 1440, header: 708, footer: 708 };
  const portrait = { page: { size: { width: 11906, height: 16838 }, margin } };
  const landscape = { page: { size: { width: 11906, height: 16838, orientation: PageOrientation.LANDSCAPE }, margin } };
  const out = [];
  let current = [];
  for (const c of children) {
    if (c && c.landscape) {
      if (current.length) out.push({ properties: portrait, headers: header(), children: current });
      out.push({ properties: landscape, headers: header(), children: c.landscape });
      current = [];
    } else current.push(c);
  }
  out.push({ properties: portrait, headers: header(), children: current.length ? current : [new Paragraph({ children: [] })] });
  return out;
}

async function convert(file) {
  const md = fs.readFileSync(path.join(SRC, file), 'utf8');
  let tokens = marked.lexer(md);
  const base = path.basename(file, '.md');
  const isReport = base.startsWith('11_');
  const h1 = tokens.find((t) => t.type === 'heading' && t.depth === 1);
  let title = h1 ? clean(plain(h1.tokens)).replace(/^\d+\s*·\s*/, '').trim() : base;
  let subtitle = null;

  if (isReport) {
    // La carátula del Markdown se reemplaza por la generada; el índice manual, por el automático.
    const firstHr = tokens.findIndex((t) => t.type === 'hr');
    const h2 = tokens.slice(0, firstHr).find((t) => t.type === 'heading' && t.depth === 2);
    title = 'Informe final del proyecto';
    subtitle = h2 ? clean(plain(h2.tokens)) : null;
    tokens = tokens.slice(firstHr + 1);
  }
  // El índice o "Contenido" escrito a mano en el Markdown sobra: Word genera el suyo.
  const idx = tokens.findIndex((t) => t.type === 'heading' && t.depth === 2 && /^(Índice|Contenido)$/.test(plain(t.tokens).trim()));
  if (idx >= 0) {
    const end = tokens.findIndex((t, i) => i > idx && (t.type === 'hr' || (t.type === 'heading' && t.depth <= 2)));
    tokens.splice(idx, (end < 0 ? tokens.length : (tokens[end].type === 'hr' ? end + 1 : end)) - idx);
  }

  let numbered = 0;
  const numberingConfigs = [];
  const conv = makeConverter({
    title, double: isReport, hrIsPageBreak: isReport, newPagePerChapter: isReport,
    numRef: () => { const r = 'num' + numbered; return r; },
  });
  // Cada lista numerada reinicia en 1: se crea una referencia por lista.
  for (const t of tokens) {
    if (t.type === 'list' && t.ordered) {
      const ref = 'num' + numbered;
      numberingConfigs.push({ reference: ref, levels: [0, 1, 2].map((l) => ({ level: l, format: LevelFormat.DECIMAL,
        text: `%${l + 1}.`, alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: INDENT * (l + 1), hanging: 360 } } } })) });
      conv.block(t);
      numbered++;
    } else conv.block(t);
  }

  const doc = new Document({
    creator: 'Equipo Amoblador3D', title, description: 'Documentación del proyecto Amoblador3D',
    features: { updateFields: true },
    styles: {
      default: { document: { run: { font: FONT, size: 24 }, paragraph: { spacing: { line: isReport ? 480 : 360 } } } },
      paragraphStyles: [
        headingStyle('Heading1', 'Heading 1', 24, { center: true, before: 360, outline: 0 }),
        headingStyle('Heading2', 'Heading 2', 24, { before: 240, outline: 1 }),
        headingStyle('Heading3', 'Heading 3', 24, { italics: true, before: 200, outline: 2 }),
      ],
    },
    numbering: { config: [
      { reference: 'bullets', levels: [0, 1, 2].map((l) => ({ level: l, format: LevelFormat.BULLET, text: ['•', '◦', '▪'][l],
        alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: INDENT * (l + 1), hanging: 360 } } } })) },
      ...numberingConfigs,
    ] },
    sections: sections([...cover(title, subtitle), ...toc(), ...conv.body]),
  });
  const outName = base + '.docx';
  const c = conv.counts();
  try {
    fs.writeFileSync(path.join(OUT, outName), await Packer.toBuffer(doc));
    console.log(`${outName}: ${c.tables} tablas, ${c.figures} figuras`);
  } catch (e) {
    if (e.code !== 'EBUSY' && e.code !== 'EPERM') throw e;
    console.log(`${outName}: NO SE ACTUALIZÓ (está abierto en Word; ciérralo y vuelve a generar)`);
  }
}

(async () => {
  const files = fs.readdirSync(SRC).filter((f) => /^\d\d_.*\.md$/.test(f)).sort();
  for (const f of files) await convert(f);
})().catch((e) => { console.error(e); process.exit(1); });
