"""Cambia el logo y el encabezado de los Word de documentacion/doc 2 SIN regenerarlos.

Conserva las ediciones hechas a mano en Word. Se puede ejecutar cuantas veces se quiera:
para poner el logo definitivo del equipo, reemplaza recursos/logo_equipo.png y vuelve a ejecutar.

    python cambiar_logo.py

Qué hace en cada .docx:
  - Carátula: quita las líneas de universidad, facultad y escuela, y pone el logo del equipo.
  - Encabezado de todas las páginas: quita los textos de la universidad y el logo de la UNAJ,
    y deja solo el logo del equipo con una línea debajo.
"""
import io
import re
import struct
import sys
import zipfile
from pathlib import Path

AQUI = Path(__file__).resolve().parent
DOCS = AQUI.parent / 'doc 2'
LOGO = AQUI / 'recursos' / 'logo_equipo.png'

NOMBRE_LOGO = 'Logo del equipo'
DESCR_LOGO = 'Logotipo del equipo FSHF'
NOMBRES_VIEJOS = ('Logo UNAJ', NOMBRE_LOGO)
TEXTOS_CARATULA = ('UNIVERSIDAD NACIONAL DE JULIACA', 'Facultad de Ciencias de Ingenierías',
                   'ESCUELA PROFESIONAL DE INGENIERÍA DE SISTEMAS Y SOFTWARE')
TEXTO_ENCABEZADO = 'Universidad Nacional de Juliaca'

EMU_CM = 360000
ALTO_ENCABEZADO = int(1.0 * EMU_CM)   # alto del logo en el encabezado
ALTO_CARATULA = int(2.4 * EMU_CM)     # alto del logo en la carátula
ANCHO_MAX = int(15.0 * EMU_CM)


def tam_png(data):
    if data[:8] != b'\x89PNG\r\n\x1a\n':
        sys.exit('El logo debe ser un archivo PNG: ' + str(LOGO))
    return struct.unpack('>II', data[16:24])


def medidas(alto, w, h):
    ancho = int(alto * w / h)
    if ancho > ANCHO_MAX:
        ancho, alto = ANCHO_MAX, int(ANCHO_MAX * h / w)
    return ancho, alto


def texto(p):
    return ''.join(re.findall(r'<w:t(?: [^>]*)?>([^<]*)</w:t>', p))


def rels(z, parte):
    carpeta, nombre = parte.rsplit('/', 1)
    ruta = f'{carpeta}/_rels/{nombre}.rels'
    if ruta not in z.namelist():
        return {}
    xml = z.read(ruta).decode('utf8')
    return {m.group(1): m.group(2) for m in re.finditer(r'<Relationship [^>]*?Id="([^"]+)"[^>]*?Target="([^"]+)"', xml)}


def dibujo_inline(rid, ancho, alto, ident):
    return (
        '<w:r><w:drawing><wp:inline distT="0" distB="0" distL="0" distR="0">'
        f'<wp:extent cx="{ancho}" cy="{alto}"/><wp:effectExtent l="0" t="0" r="0" b="0"/>'
        f'<wp:docPr id="{ident}" name="{NOMBRE_LOGO}" descr="{DESCR_LOGO}" title="{NOMBRE_LOGO}"/>'
        '<wp:cNvGraphicFramePr><a:graphicFrameLocks xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" noChangeAspect="1"/></wp:cNvGraphicFramePr>'
        '<a:graphic xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">'
        '<a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">'
        '<pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">'
        f'<pic:nvPicPr><pic:cNvPr id="0" name="{NOMBRE_LOGO}"/><pic:cNvPicPr/></pic:nvPicPr>'
        f'<pic:blipFill><a:blip r:embed="{rid}"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>'
        f'<pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="{ancho}" cy="{alto}"/></a:xfrm>'
        '<a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr>'
        '</pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r>'
    )


def parrafo_encabezado(rid, ancho, alto):
    return ('<w:p><w:pPr><w:pBdr><w:bottom w:val="single" w:sz="4" w:space="4" w:color="000000"/></w:pBdr>'
            '<w:spacing w:before="0" w:after="0" w:line="240" w:lineRule="auto"/></w:pPr>'
            + dibujo_inline(rid, ancho, alto, 9001) + '</w:p>')


def logo_rid(xml, parte_rels):
    """Id de la relación de la imagen que es el logo (UNAJ o del equipo) en esta parte."""
    for m in re.finditer(r'<w:drawing>.*?</w:drawing>', xml, re.S):
        d = m.group(0)
        if any(f'name="{n}"' in d for n in NOMBRES_VIEJOS):
            rid = re.search(r'r:embed="([^"]+)"', d)
            if rid and rid.group(1) in parte_rels:
                return rid.group(1)
    return None


FILAS_QUITAR = ('CARRERA', 'CURSO', 'DOCENTE', 'SEMESTRE', 'Docente de la asignatura')
EQUIPO = 'FSHF'


def quitar_datos_academicos(xml):
    """Carátula y página de control: sin carrera, curso, docente ni semestre; con la fila EQUIPO."""
    filas = re.findall(r'<w:tr[ >](?:(?!<w:tr[ >]).)*?</w:tr>', xml[:400000], re.S)
    for fila in filas:
        celdas = re.findall(r'<w:tc>.*?</w:tc>', fila, re.S)
        if celdas and texto(celdas[0]).strip() in FILAS_QUITAR:
            xml = xml.replace(fila, '', 1)
    if '>EQUIPO<' not in xml[:400000]:
        fecha = next((f for f in filas if re.findall(r'<w:tc>.*?</w:tc>', f, re.S)
                      and texto(re.findall(r'<w:tc>.*?</w:tc>', f, re.S)[0]).strip() == 'FECHA'), None)
        if fecha and fecha in xml:
            c0, c1 = re.findall(r'<w:tc>.*?</w:tc>', fecha, re.S)[:2]
            nueva = fecha.replace(c0, c0.replace('>FECHA<', '>EQUIPO<'), 1)
            nueva = nueva.replace(c1, re.sub(r'(<w:t(?: [^>]*)?>)[^<]*(</w:t>)', r'\g<1>' + EQUIPO + r'\2', c1, count=1), 1)
            xml = xml.replace(fecha, nueva + fecha, 1)
    return xml.replace('Versión de entrega, para revisión del docente', 'Versión publicada')


def procesar(ruta, logo, w, h):
    with zipfile.ZipFile(ruta) as z:
        partes = {n: z.read(n) for n in z.namelist()}
        infos = {i.filename: i for i in z.infolist()}
        nombres = z.namelist()
        rels_de = {n: rels(z, n) for n in nombres if re.match(r'word/(document|header\d+)\.xml$', n)}

    medias = set()
    cambios = []

    # 1. Encabezados
    for n in [n for n in nombres if re.match(r'word/header\d+\.xml$', n)]:
        xml = partes[n].decode('utf8')
        rid = logo_rid(xml, rels_de[n])
        if not rid:
            continue
        medias.add('word/' + rels_de[n][rid].lstrip('/').removeprefix('word/'))
        ancho, alto = medidas(ALTO_ENCABEZADO, w, h)
        cuerpo = re.search(r'(<w:hdr[^>]*>)(.*)(</w:hdr>)', xml, re.S)
        xml = cuerpo.group(1) + parrafo_encabezado(rid, ancho, alto) + cuerpo.group(3)
        xml = xml if xml.startswith('<?xml') else '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n' + xml
        partes[n] = xml.encode('utf8')
        cambios.append(n)

    # 2. Carátula
    n = 'word/document.xml'
    xml = partes[n].decode('utf8')
    antes = len(xml)
    for p in re.findall(r'<w:p[ >](?:(?!<w:p[ >]).)*?</w:p>', xml[:200000], re.S):
        if texto(p).strip() in TEXTOS_CARATULA:
            xml = xml.replace(p, '', 1)
    rid = logo_rid(xml, rels_de[n])
    if rid:
        medias.add('word/' + rels_de[n][rid].lstrip('/').removeprefix('word/'))
        ancho, alto = medidas(ALTO_CARATULA, w, h)
        m = re.search(r'<w:drawing>(?:(?!</w:drawing>).)*?r:embed="' + rid + r'".*?</w:drawing>', xml, re.S)
        d = m.group(0)
        d2 = re.sub(r'<wp:extent cx="\d+" cy="\d+"/>', f'<wp:extent cx="{ancho}" cy="{alto}"/>', d)
        d2 = re.sub(r'<a:ext cx="\d+" cy="\d+"/>', f'<a:ext cx="{ancho}" cy="{alto}"/>', d2)
        d2 = re.sub(r'(<wp:docPr [^>]*?)name="[^"]*"([^>]*?)descr="[^"]*"([^>]*?)title="[^"]*"',
                    rf'\1name="{NOMBRE_LOGO}"\2descr="{DESCR_LOGO}"\3title="{NOMBRE_LOGO}"', d2)
        xml = xml.replace(d, d2, 1)
        # Más aire sobre el logo, ya que ahora abre la carátula
        xml = re.sub(r'(<w:p(?: [^>]*)?><w:pPr><w:spacing )w:after="480"/>(<w:jc w:val="center"/></w:pPr>(?:(?!</w:p>).)*?'
                     + NOMBRE_LOGO + ')', r'\1w:before="600" w:after="720"/>\2', xml, count=1, flags=re.S)
    xml = quitar_datos_academicos(xml)
    if len(xml) != antes or rid:
        partes[n] = xml.encode('utf8')
        cambios.append(n)

    # 3. La imagen
    for m in medias:
        partes[m] = logo

    if not cambios:
        return False
    tmp = ruta.with_suffix('.tmp')
    with zipfile.ZipFile(tmp, 'w', zipfile.ZIP_DEFLATED) as z:
        for nombre in nombres:
            z.writestr(infos[nombre], partes[nombre], compress_type=zipfile.ZIP_DEFLATED)
    tmp.replace(ruta)
    return True


def main():
    logo = LOGO.read_bytes()
    w, h = tam_png(logo)
    archivos = sys.argv[1:] or sorted(str(p) for p in DOCS.glob('[0-9][0-9]_*.docx'))
    for a in archivos:
        ruta = Path(a)
        try:
            ok = procesar(ruta, logo, w, h)
            print(f'{ruta.name}: {"actualizado" if ok else "sin logo que cambiar"}')
        except PermissionError:
            print(f'{ruta.name}: NO SE ACTUALIZÓ (está abierto en Word; ciérralo y vuelve a ejecutar)')


if __name__ == '__main__':
    main()
