"""Genera el código fuente COMPLETO de Amoblador3D: el nombre de cada archivo y, debajo, su código.

    python codigo_completo.py <carpeta_salida>
    node generar.js <carpeta_salida> <carpeta_salida>

El resultado (Codigo_Fuente_Completo.md) no forma parte de los documentos 00 a 09: es un anexo que se
publica en la página web como PDF.
"""
import sys
from datetime import date
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
SALIDA = Path(sys.argv[1]) if len(sys.argv) > 1 else RAIZ / 'target' / 'codigo_completo'
OTROS = ['pom.xml', 'src/main/resources/ui/app.css', 'src/main/resources/assets/catalog.json',
         'database/amoblador3d_sqlite.sql', 'database/amoblador3d_postgresql.sql',
         'Ejecutar-Amoblador3D.cmd', 'packaging/crear-instalador.ps1', 'packaging/crear-instalador.cmd']
LENGUAJE = {'.java': 'java', '.xml': 'xml', '.css': 'css', '.json': 'json', '.sql': 'sql', '.cmd': 'bat', '.ps1': 'powershell'}


def leer(p):
    for cod in ('utf-8-sig', 'cp1252'):
        try:
            return p.read_text(encoding=cod)
        except UnicodeDecodeError:
            pass
    return p.read_text(encoding='utf-8', errors='replace')


def bloque(texto, lenguaje):
    filas = texto.rstrip('\n').replace('\t', '    ').split('\n')
    ancho = len(str(len(filas)))
    return '~~~~' + lenguaje + '\n' + '\n'.join(f'{i:>{ancho}}  {l.rstrip()}' for i, l in enumerate(filas, 1)) + '\n~~~~\n'


def main():
    java = sorted((RAIZ / 'src' / 'main' / 'java').rglob('*.java'))
    otros = [RAIZ / r for r in OTROS if (RAIZ / r).exists()]
    archivos = java + otros
    lineas = sum(len(leer(p).splitlines()) for p in archivos)
    miles = f'{lineas:,}'.replace(',', ' ')
    md = ['---', 'codigo: AMB3D-CDF-09-A', 'proyecto: Amoblador3D 2.0.0', '---', '',
          '# Código fuente completo', '',
          '## Presentación', '',
          f'Este anexo reúne el código fuente completo de Amoblador3D 2.0.0, desarrollado por el equipo FSHF: '
          f'{len(java)} archivos Java y {len(otros)} archivos de configuración, estilos, base de datos y '
          f'empaquetado, en total {miles} líneas.', '',
          'Cada archivo aparece con su ruta dentro del proyecto como título y, debajo, su código completo. El '
          'número de la izquierda es el número de línea. La explicación de la estructura y de los fragmentos '
          'principales está en el documento 09, Código fuente.', '']
    for p in archivos:
        rel = p.relative_to(RAIZ).as_posix()
        md += [f'## {rel}', '', bloque(leer(p), LENGUAJE.get(p.suffix, 'text'))]
    md += ['## Historial del documento', '', '<!-- Tabla: Historial de cambios de este documento -->',
           '| Versión | Fecha | Cambios | Responsable |', '|---|---|---|---|',
           f'| 1.0 | {date.today().isoformat()} | Código fuente completo de la versión 2.0.0 | Equipo FSHF |', '']
    SALIDA.mkdir(parents=True, exist_ok=True)
    destino = SALIDA / '10_Codigo_Fuente_Completo.md'
    destino.write_text('\n'.join(md), encoding='utf-8')
    print(f'{destino.name}: {len(archivos)} archivos, {lineas} líneas')


if __name__ == '__main__':
    main()
