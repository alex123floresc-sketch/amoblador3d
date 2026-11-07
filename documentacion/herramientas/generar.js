const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');
const { marked } = require('marked');
const {
  Document, Packer, Paragraph, TextRun, HeadingLevel, AlignmentType, Table, TableRow, TableCell,
  WidthType, BorderStyle, ShadingType, ImageRun, PageBreak, Header, PageNumber, TableOfContents,
  LevelFormat, VerticalAlign, ExternalHyperlink, PageOrientation, Footer, Bookmark, PageReference,
  TabStopType, LeaderType, HorizontalPositionAlign, HorizontalPositionRelativeFrom,
  VerticalPositionRelativeFrom, TextWrappingType, PageBorderDisplay, PageBorderOffsetFrom,
} = require('docx');

const SRC = process.argv[2];
const OUT = process.argv[3];
const WORK = path.join(__dirname, 'tmp-' + Date.now());
fs.mkdirSync(OUT, { recursive: true });
fs.mkdirSync(WORK, { recursive: true });

const FONT = 'Times New Roman';
const MONO = 'Consolas';
const CONTENT_W = 11906 - 2 * 1440;
const INDENT = 720;

const BLACK = '000000', LINE = '999999', GRAY = '404040', CODE_BG = 'F2F2F2';

const EMOJI = /[\u{1F000}-\u{1FAFF}\u{2600}-\u{27BF}\u{2B00}-\u{2BFF}\u{FE0F}\u{200D}\u{2300}-\u{23FF}\u{2900}-\u{297F}\u{25A0}-\u{25FF}\u{2700}-\u{27BF}]/gu;
function clean(t) {
  return t
    .replace(/✅/g, 'Sí')
    .replace(/＋/g, '+')
    .replace(EMOJI, '')
    .replace(/&nbsp;/g, ' ')
    .replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&#39;/g, "'")
    .replace(/\s*\n\s*/g, ' ')
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
        } else out.push(...inner);
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
  return new TextRun({
    text: value,
    bold: s.bold, italics: s.italics, strike: s.strike,
    font: s.code ? MONO : (s.font || FONT),
    size: s.size || (s.code ? (s.inTable ? 18 : 20) : undefined),
    color: s.color,
    underline: s.link ? {} : undefined,
  });
}

const plain = (tokens) => (tokens || []).map((t) => (t.tokens ? plain(t.tokens) : t.text || '')).join('');

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

function makeConverter(opts) {
  let tables = 0, figures = 0, lastHeading = '', inReferences = false;
  const body = [];

  const pending = {};
  const take = (kind) => { const t = pending[kind]; delete pending[kind]; return t; };
  const index = { Tabla: [], Figura: [] };
  const lineSpacing = opts.double ? 480 : 360;

  const para = (children, extra = {}) =>
    new Paragraph({ children, spacing: { line: lineSpacing, before: 0, after: 0 }, indent: { firstLine: INDENT }, ...extra });

  function caption(kind, n, title) {
    const id = (kind === 'Tabla' ? 'tabla_' : 'figura_') + n;
    index[kind].push({ n, title, id });
    body.push(new Paragraph({ keepNext: true, spacing: { before: 240, after: 0, line: lineSpacing },
      children: [new Bookmark({ id, children: [new TextRun({ text: `${kind} ${n}`, bold: true, font: FONT })] })] }));
    body.push(new Paragraph({ keepNext: true, spacing: { after: 120, line: lineSpacing },
      children: [new TextRun({ text: title, italics: true, font: FONT })] }));
  }

  function table(t, captioned = true, target = body) {
    if (captioned) {
      tables++;
      caption('Tabla', tables, take('Tabla') || lastHeading || opts.title);
    }
    const cols = t.header.length;
    const lens = t.header.map((h, i) => Math.max(plain(h.tokens).length,
      ...t.rows.map((r) => Math.min(plain(r[i] && r[i].tokens).length, 70)), 4));
    const total = lens.reduce((a, b) => a + b, 0);

    const longest = t.header.map((h, i) => Math.max(...[h, ...t.rows.map((r) => r[i])]
      .map((c) => Math.max(0, ...clean(plain(c && c.tokens)).split(/\s+/).map((wd) => wd.length)))));
    const mins = longest.map((l) => Math.max(700, Math.min(2400, l * 100 + 200)));

    const minSum = mins.reduce((a, b) => a + b, 0);
    let widths;
    if (minSum >= CONTENT_W) widths = mins.map((m) => Math.floor(m * CONTENT_W / minSum));
    else {
      const extra = lens.map((l, i) => Math.max(0, (l / total) * CONTENT_W - mins[i]));
      const extraSum = extra.reduce((a, b) => a + b, 0) || 1;
      widths = mins.map((m, i) => Math.floor(m + (extra[i] / extraSum) * (CONTENT_W - minSum)));
    }
    widths[cols - 1] += CONTENT_W - widths.reduce((a, b) => a + b, 0);

    const strong = { style: BorderStyle.SINGLE, size: 8, color: BLACK };
    const thin = { style: BorderStyle.SINGLE, size: 4, color: LINE };
    const none = { style: BorderStyle.NONE, size: 0, color: 'auto' };
    const cell = (tokens, i, header, ri, last) => new TableCell({
      width: { size: widths[i], type: WidthType.DXA },
      verticalAlign: VerticalAlign.CENTER,
      margins: { top: 60, bottom: 60, left: 100, right: 100 },
      borders: { top: header ? strong : none, bottom: header || last ? strong : thin, left: none, right: none },
      children: [new Paragraph({ spacing: { line: 240, before: 0, after: 0 },
        children: runs(tokens, { size: 20, inTable: true, bold: header || undefined }) })],
    });
    target.push(new Table({
      width: { size: CONTENT_W, type: WidthType.DXA }, columnWidths: widths,
      borders: { top: none, bottom: none, left: none, right: none, insideHorizontal: none, insideVertical: none },
      rows: [
        new TableRow({ tableHeader: true, children: t.header.map((h, i) => cell(h.tokens, i, true, 0, false)) }),
        ...t.rows.map((r, ri) => new TableRow({ cantSplit: true,
          children: r.map((c, i) => cell(c.tokens, i, false, ri, ri === t.rows.length - 1)) })),
      ],
    }));
    target.push(new Paragraph({ spacing: { after: 240 }, children: [] }));
  }

  function picture(img) {
    figures++;
    caption('Figura', figures, take('Figura') || clean(img.text || lastHeading));
    const data = fs.readFileSync(path.join(SRC, decodeURI(img.href)));
    const w = data.readUInt32BE(16), h = data.readUInt32BE(20);
    const maxW = opts.double ? 470 : 600;
    const k = Math.min(1, maxW / w, 700 / h);
    body.push(new Paragraph({ alignment: AlignmentType.CENTER, keepNext: true, spacing: { after: 120 }, border: { top: { style: BorderStyle.SINGLE, size: 4, color: LINE, space: 4 }, bottom: { style: BorderStyle.SINGLE, size: 4, color: LINE, space: 4 }, left: { style: BorderStyle.SINGLE, size: 4, color: LINE, space: 4 }, right: { style: BorderStyle.SINGLE, size: 4, color: LINE, space: 4 } },
      children: [new ImageRun({ type: 'png', data, transformation: { width: Math.round(w * k), height: Math.round(h * k) } })] }));
    body.push(new Paragraph({ spacing: { after: 240, line: lineSpacing }, children: [
      new TextRun({ text: 'Nota.', italics: true, font: FONT }),
      new TextRun({ text: ' Captura de pantalla de Amoblador3D 2.0.0. Elaboración propia.', font: FONT })] }));
  }

  function code(t) {
    if (t.lang === 'mermaid') {
      figures++;
      const img = mermaidPng(t.text);
      let w = img.w / 2, h = img.h / 2;

      const wide = w > 640 && w / h > 1.3;
      const start = body.length;
      caption('Figura', figures, take('Figura') || lastHeading || opts.title);
      const maxW = wide ? 900 : 600, maxH = wide ? 410 : 740;
      const k = Math.min(1, maxW / w, maxH / h);
      body.push(new Paragraph({ alignment: AlignmentType.CENTER, keepNext: true, spacing: { after: 120 }, border: { top: { style: BorderStyle.SINGLE, size: 4, color: LINE, space: 4 }, bottom: { style: BorderStyle.SINGLE, size: 4, color: LINE, space: 4 }, left: { style: BorderStyle.SINGLE, size: 4, color: LINE, space: 4 }, right: { style: BorderStyle.SINGLE, size: 4, color: LINE, space: 4 } },
        children: [new ImageRun({ type: 'png', data: img.data, transformation: { width: Math.round(w * k), height: Math.round(h * k) } })] }));
      body.push(new Paragraph({ spacing: { after: 240, line: lineSpacing }, children: [
        new TextRun({ text: 'Nota.', italics: true, font: FONT }),
        new TextRun({ text: ' Elaboración propia.', font: FONT })] }));
      if (wide) body.push({ landscape: body.splice(start) });
      return;
    }
    const lines = t.text.split('\n');
    lines.forEach((l, i) => body.push(new Paragraph({
      shading: { type: ShadingType.CLEAR, fill: CODE_BG, color: 'auto' },
      spacing: { line: 240, before: i === 0 ? 120 : 0, after: i === lines.length - 1 ? 200 : 0 },
      indent: { left: 360, right: 240 },
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
        else if (tk.type === 'checkbox') { }
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
    if (t.depth === 1) return;
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
            spacing: { line: 300, before: 160, after: 160 }, children: runs(inner.tokens, {}) }));
          else block(inner);
        }
        break;
      case 'html': {
        const m = /<!--\s*(Tabla|Figura):\s*([\s\S]*?)\s*-->/.exec(t.text);
        if (m) pending[m[1]] = m[2];
        break;
      }
      case 'hr': if (opts.hrIsPageBreak) body.push(new Paragraph({ children: [new PageBreak()] })); break;
      default: break;
    }
  }
  return { block, body, index, table, counts: () => ({ tables, figures }) };
}

const META = {
  universidad: 'UNIVERSIDAD NACIONAL DE JULIACA',
  facultad: 'Facultad de Ciencias de Ingenierías',
  escuela: 'ESCUELA PROFESIONAL DE INGENIERÍA DE SISTEMAS Y SOFTWARE',
  proyecto: 'Amoblador3D: aplicación de escritorio para el diseño de interiores en 3D con medidas reales y estimación de presupuesto',
  integrantes: ['Flores Calcina, Alexander Edson', 'Sandia Condori, George James', 'Huanca Tiña, Erick Reison', 'Flores Ccori, Brayan'],
  docente: 'Noe Tipo Mamani',
  asignatura: 'Bases de Datos',
  semestre: '2026-II',
  lugar: 'JULIACA – PERÚ',
  anio: '2026',
  estado: 'Versión de entrega, para revisión del docente',
};

const ROL = {
  jefe: 'Flores Calcina, Alexander Edson (jefe de proyecto)',
  arquitecto: 'Flores Calcina, Alexander Edson (arquitecto y desarrollador)',
  analista: 'Sandia Condori, George James (analista)',
  calidad: 'Huanca Tiña, Erick Reison (responsable de calidad)',
  documentador: 'Flores Ccori, Brayan (documentador)',
};
const ELABORA = { '00': 'documentador', '01': 'jefe', '02': 'analista', '03': 'analista', '04': 'arquitecto',
  '05': 'arquitecto', '06': 'calidad', '07': 'documentador', '08': 'arquitecto', '09': 'documentador',
  '10': 'jefe', '11': 'documentador', '12': 'jefe', '13': 'documentador' };
const MESES = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'];
const fechaLarga = (iso) => { const [a, m, d] = iso.split('-').map(Number); return `${d} de ${MESES[m - 1]} de ${a}`; };
const RECURSOS = path.join(__dirname, 'recursos');
const LOGO_CARATULA = fs.readFileSync(path.join(RECURSOS, 'logo_unaj_caratula.png'));
const LOGO_ENCABEZADO = fs.readFileSync(path.join(RECURSOS, 'logo_unaj_encabezado.png'));
const LOGO_ALT = { name: 'Logo UNAJ', title: 'Logo UNAJ', description: 'Logotipo de la Universidad Nacional de Juliaca' };

function cover(info) {
  const c = (txt, o = {}) => new Paragraph({ alignment: AlignmentType.CENTER,
    spacing: { line: o.line || 276, before: o.before || 0, after: o.after === undefined ? 120 : o.after },
    border: o.border,
    children: [new TextRun({ text: txt, bold: o.bold, italics: o.italics, size: o.size, color: o.color,
      font: o.font || FONT, characterSpacing: o.spacing })] });
  const none = { style: BorderStyle.NONE, size: 0, color: 'auto' };
  const noBorders = { top: none, bottom: none, left: none, right: none };
  const labelW = 2300, valueW = 5200;
  const fila = (label, values) => new TableRow({ cantSplit: true, children: [
    new TableCell({ width: { size: labelW, type: WidthType.DXA }, borders: noBorders, margins: { top: 40, bottom: 40, right: 160 },
      children: [new Paragraph({ alignment: AlignmentType.RIGHT, spacing: { line: 276, before: 0, after: 0 },
        children: [new TextRun({ text: label, bold: true, size: 20, font: FONT, characterSpacing: 10 })] })] }),
    new TableCell({ width: { size: valueW, type: WidthType.DXA }, borders: noBorders,
      margins: { top: 40, bottom: 40, left: 200 },
      children: values.map((v) => new Paragraph({ spacing: { line: 276, before: 0, after: 0 },
        children: [new TextRun({ text: v, size: 22, font: FONT })] })) }),
  ] });
  const datos = [`Código ${info.codigo}`, info.version && `Versión ${info.version}`, info.fecha && fechaLarga(info.fecha)]
    .filter(Boolean).join('   ·   ');
  return [
    c(META.universidad, { bold: true, size: 30, after: 60 }),
    c(META.facultad, { bold: true, color: GRAY, after: 60 }),
    c(META.escuela, { size: 22, color: GRAY, after: 360 }),
    new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 480 },
      children: [new ImageRun({ type: 'png', data: LOGO_CARATULA, transformation: { width: 259, height: 94 }, altText: LOGO_ALT })] }),

    c(info.titulo.toUpperCase(), { bold: true, size: 36, line: 360, after: 0 }),
    c('PROYECTO', { bold: true, size: 18, color: GRAY, before: 480, after: 80, spacing: 40 }),
    c(info.proyecto, { bold: true, size: 26, line: 320, after: 200 }),
    c(datos, { size: 20, color: GRAY, after: 600 }),
    new Table({ alignment: AlignmentType.CENTER, width: { size: labelW + valueW, type: WidthType.DXA }, columnWidths: [labelW, valueW],
      borders: { ...noBorders, insideHorizontal: none, insideVertical: none },
      rows: [
        fila('INTEGRANTES', META.integrantes),
        fila('DOCENTE', [META.docente]),
        fila('ASIGNATURA', [META.asignatura]),
        fila('SEMESTRE', [META.semestre]),
      ] }),
    c(META.lugar, { bold: true, before: 720, after: 0 }),
    c(META.anio, { bold: true, after: 0 }),
    new Paragraph({ children: [new PageBreak()] }),
  ];
}

function controlPage(info, conv) {
  const title = (t, before = 0) => new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before, after: 240 },
    children: [new TextRun({ text: t, bold: true, font: FONT, size: 26 })] });
  const esc = (s) => String(s).replace(/\|/g, '\\|');
  const ficha = [
    ['Documento', info.titulo], ['Código', info.codigo], ['Versión', info.version || '—'],
    ['Fecha', info.fecha ? fechaLarga(info.fecha) : '—'], ['Estado', META.estado],
    ['Norma de referencia', info.norma || '—'], ['Proyecto', info.proyecto],
    ['Elaborado por', ROL[ELABORA[info.n]] || 'Equipo del proyecto'],
    ['Revisado por', info.n === '06' ? ROL.jefe : ROL.calidad],
    ['Aprobado por', ROL.jefe], ['Docente de la asignatura', META.docente],
  ];
  const md = '| Campo | Valor |\n|---|---|\n' + ficha.map(([k, v]) => `| **${k}** | ${esc(v)} |`).join('\n');
  const out = [title('Control del documento')];
  conv.table(marked.lexer(md).find((t) => t.type === 'table'), false, out);
  if (info.historial) {
    out.push(title('Historial de revisiones', 240));
    conv.table(info.historial, false, out);
  }
  out.push(new Paragraph({ children: [new PageBreak()] }));
  return out;
}

function frontMatter(md) {
  const m = /^---\r?\n([\s\S]*?)\r?\n---\r?\n/.exec(md);
  if (!m) return { meta: {}, body: md };
  const meta = {};
  for (const line of m[1].split(/\r?\n/)) {
    const i = line.indexOf(':');
    if (i > 0) meta[line.slice(0, i).trim()] = line.slice(i + 1).trim();
  }
  return { meta, body: md.slice(m[0].length) };
}

function captionIndex(kind, items) {
  if (!items.length) return [];
  return [
    new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 240 },
      children: [new TextRun({ text: kind === 'Tabla' ? 'Índice de tablas' : 'Índice de figuras', bold: true, font: FONT, size: 24 })] }),
    ...items.map((it) => new Paragraph({
      tabStops: [{ type: TabStopType.LEFT, position: 1134 }, { type: TabStopType.RIGHT, position: CONTENT_W, leader: LeaderType.DOT }],
      indent: { left: 1134, hanging: 1134 },
      spacing: { line: 276, before: 0, after: 100 },
      children: [
        new TextRun({ text: `${kind} ${it.n}`, bold: true, font: FONT }),
        new TextRun({ text: '\t' + it.title + '\t', italics: true, font: FONT }),
        new TextRun({ children: [new PageReference(it.id)], font: FONT }),
      ],
    })),
    new Paragraph({ children: [new PageBreak()] }),
  ];
}

function toc() {
  return [
    new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 240 }, children: [new TextRun({ text: 'Índice', bold: true, font: FONT, size: 24 })] }),
    new TableOfContents('Índice', { hyperlink: true, headingStyleRange: '1-3' }),
    new Paragraph({ children: [new PageBreak()] }),
  ];
}

function headingStyle(id, name, size, opts) {
  return { id, name, basedOn: 'Normal', next: 'Normal', quickFormat: true,
    run: { font: FONT, size, bold: true, italics: opts.italics, color: BLACK },
    paragraph: { alignment: opts.center ? AlignmentType.CENTER : AlignmentType.LEFT, keepNext: true, keepLines: true,
      spacing: { before: opts.before, after: 120, line: 360 }, outlineLevel: opts.outline } };
}

function sections(children, docTitle) {
  const hp = (children, last) => new Paragraph({ spacing: { line: 240, before: 0, after: 0 },
    border: last ? { bottom: { style: BorderStyle.SINGLE, size: 4, color: BLACK, space: 4 } } : undefined, children });
  const ht = (txt) => new TextRun({ text: txt, bold: true, font: FONT, size: 22 });
  const header = () => new Header({ children: [
    hp([new ImageRun({ type: 'png', data: LOGO_ENCABEZADO, transformation: { width: 140, height: 40 }, altText: LOGO_ALT,
      floating: { horizontalPosition: { relative: HorizontalPositionRelativeFrom.MARGIN, align: HorizontalPositionAlign.RIGHT },
        verticalPosition: { relative: VerticalPositionRelativeFrom.PARAGRAPH, offset: 0 },
        wrap: { type: TextWrappingType.NONE }, allowOverlap: true } }),
    ht('Universidad Nacional de Juliaca')]),
    hp([ht('Facultad de Ciencias de Ingenierías')]),
    hp([ht('Escuela Profesional de Ingeniería de Sistemas y Software')], true),
  ] });
  const fr = (o) => new TextRun({ font: FONT, size: 18, color: GRAY, ...o });
  const footer = () => new Footer({ children: [new Paragraph({
    tabStops: [{ type: TabStopType.RIGHT, position: CONTENT_W }],
    border: { top: { style: BorderStyle.SINGLE, size: 4, color: LINE, space: 4 } },
    children: [fr({ text: docTitle }), fr({ text: '\t' }),
      fr({ children: ['Página ', PageNumber.CURRENT, ' de ', PageNumber.TOTAL_PAGES] })] })] });
  const empty = (K) => new K({ children: [new Paragraph({ children: [] })] });
  const margin = { top: 1700, bottom: 1440, left: 1440, right: 1440, header: 708, footer: 708 };
  const portrait = { page: { size: { width: 11906, height: 16838 }, margin } };

  const frame = { style: BorderStyle.DOUBLE, size: 6, color: BLACK, space: 24 };
  const coverBorders = { pageBorders: { display: PageBorderDisplay.FIRST_PAGE, offsetFrom: PageBorderOffsetFrom.PAGE },
    pageBorderTop: frame, pageBorderBottom: frame, pageBorderLeft: frame, pageBorderRight: frame };
  const landscape = { page: { size: { width: 11906, height: 16838, orientation: PageOrientation.LANDSCAPE }, margin } };
  const out = [];

  const sec = (props, kids) => out.length === 0
    ? { properties: { ...props, page: { ...props.page, borders: coverBorders }, titlePage: true }, headers: { default: header(), first: empty(Header) },
      footers: { default: footer(), first: empty(Footer) }, children: kids }
    : { properties: props, headers: { default: header() }, footers: { default: footer() }, children: kids };
  let current = [];
  for (const c of children) {
    if (c && c.landscape) {
      if (current.length) out.push(sec(portrait, current));
      out.push(sec(landscape, c.landscape));
      current = [];
    } else current.push(c);
  }
  out.push(sec(portrait, current.length ? current : [new Paragraph({ children: [] })]));
  return out;
}

async function convert(file) {
  const { meta, body: md } = frontMatter(fs.readFileSync(path.join(SRC, file), 'utf8'));
  let tokens = marked.lexer(md);
  const base = path.basename(file, '.md');
  const isReport = base.startsWith('11_');
  const h1 = tokens.find((t) => t.type === 'heading' && t.depth === 1);
  let title = h1 ? clean(plain(h1.tokens)).replace(/^\d+\s*·\s*/, '').trim() : base;
  let proyecto = META.proyecto;

  if (isReport) {
    const firstHr = tokens.findIndex((t) => t.type === 'hr');
    const h2 = tokens.slice(0, firstHr).find((t) => t.type === 'heading' && t.depth === 2);
    title = 'Informe final del proyecto';
    if (h2) proyecto = clean(plain(h2.tokens));
    tokens = tokens.slice(firstHr + 1);
  }

  let historial = null;
  const hi = tokens.findIndex((t) => t.type === 'heading' && t.depth === 2 && /^Historial del documento$/i.test(plain(t.tokens).trim()));
  if (hi >= 0 && !isReport) {
    const end = tokens.findIndex((t, i) => i > hi && t.type === 'heading' && t.depth <= 2);
    const section = tokens.splice(hi, (end < 0 ? tokens.length : end) - hi);
    historial = section.find((t) => t.type === 'table') || null;
  }
  const last = historial && historial.rows[historial.rows.length - 1];
  const info = { n: base.slice(0, 2), titulo: title, proyecto, codigo: meta.codigo || base, norma: meta.norma,
    version: last ? plain(last[0].tokens).trim() : null, fecha: last ? plain(last[1].tokens).trim() : null, historial };

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
    creator: META.integrantes.join('; '), title, description: 'Documentación del proyecto Amoblador3D',
    features: { updateFields: true },
    styles: {
      default: { document: { run: { font: FONT, size: 24 }, paragraph: { spacing: { line: isReport ? 480 : 360 } } } },
      paragraphStyles: [
        headingStyle('Heading1', 'Heading 1', 26, { center: true, before: 360, outline: 0 }),
        headingStyle('Heading2', 'Heading 2', 24, { before: 240, outline: 1 }),
        headingStyle('Heading3', 'Heading 3', 24, { italics: true, before: 200, outline: 2 }),
        headingStyle('Heading4', 'Heading 4', 24, { italics: true, before: 200, outline: 3 }),
        headingStyle('Heading5', 'Heading 5', 24, { italics: true, before: 200, outline: 4 }),
        headingStyle('Heading6', 'Heading 6', 24, { italics: true, before: 200, outline: 5 }),
        { id: 'Caption', name: 'caption', basedOn: 'Normal', next: 'Normal', run: { color: BLACK, italics: true } },
      ],
      characterStyles: [
        { id: 'Hyperlink', name: 'Hyperlink', basedOn: 'DefaultParagraphFont', run: { color: BLACK, underline: { type: 'single' } } },
      ],
    },
    numbering: { config: [
      { reference: 'bullets', levels: [0, 1, 2].map((l) => ({ level: l, format: LevelFormat.BULLET, text: ['•', '◦', '▪'][l],
        alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: INDENT * (l + 1), hanging: 360 } } } })) },
      ...numberingConfigs,
    ] },
    sections: sections([...cover(info), ...(isReport ? [] : controlPage(info, conv)), ...toc(),
      ...captionIndex('Tabla', conv.index.Tabla), ...captionIndex('Figura', conv.index.Figura), ...conv.body],
    `${info.codigo} · ${title}`),
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
})().catch((e) => { console.error(e); process.exitCode = 1; })
  .finally(() => fs.rmSync(WORK, { recursive: true, force: true }));
