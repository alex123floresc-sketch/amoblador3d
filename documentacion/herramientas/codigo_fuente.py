"""Genera documentacion/09_Codigo_Fuente.md: estructura del proyecto y fragmentos principales del código.

    python codigo_fuente.py
    node generar.js .. "../doc 2" 09

Sigue el modelo de entregable «Código fuente»: presenta cómo está organizado el proyecto y copia,
tal cual, los fragmentos más representativos (no todo el código). Los fragmentos se ubican por el
nombre del método, así que el documento se puede regenerar aunque cambien los números de línea.
"""
import re
from datetime import date
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
SALIDA = RAIZ / 'documentacion' / '09_Codigo_Fuente.md'
JAVA = RAIZ / 'src' / 'main' / 'java'
BASE = JAVA / 'pe' / 'edu' / 'unaj' / 'amoblador3d'
POM = (RAIZ / 'pom.xml').read_text(encoding='utf-8')
VERSION = re.search(r'<artifactId>amoblador3d</artifactId>\s*<version>([^<]+)</version>', POM).group(1)
REPO = 'https://github.com/alex123floresc-sketch/amoblador3d'

PAQUETES = [
    ('', 'Arranque de la aplicación'),
    ('model', 'Modelo del dominio: habitación, aberturas, muebles, luces, superficies y casa'),
    ('design', 'Edición del ambiente: reglas, historial (deshacer y rehacer), imán y plantillas'),
    ('catalog', 'Catálogo de muebles: paramétricos, modelos 3D y diseños propios'),
    ('estimate', 'Metrado y presupuesto'),
    ('persistence', 'Base de datos SQLite, archivos .amob3d y autoguardado'),
    ('scene', 'Vista 3D, planta y recorrido en primera persona'),
    ('gltf', 'Lectura de modelos 3D en formato glTF'),
    ('report', 'Informe PDF'),
    ('ui', 'Ventanas, paneles y diálogos'),
    ('util', 'Utilidades: registro de errores, resultados y unidades'),
]

CORTO = {'persistence': 'Base de datos y archivos', 'scene': 'Vista 3D, planta y recorrido',
         'gltf': 'Modelos 3D glTF'}

# (archivo relativo a BASE, firma que identifica el método o None para el archivo completo, título, explicación)
FRAGMENTOS = [
    ('FSHFLauncher.java', None, 'Punto de entrada del programa',
     'Es la clase que ejecuta Java al abrir Space Cat. Su único trabajo es lanzar la aplicación JavaFX '
     '`FSHFApp`, que prepara el registro de errores, abre la base de datos y muestra la ventana principal.'),
    ('model/FSHFFootprint.java', 'public boolean overlaps(', 'Detección de choques entre muebles',
     'Cada mueble ocupa en planta un rectángulo que puede estar girado. Para saber si dos muebles se '
     'superponen se aplica el **teorema del eje separador**: se proyectan ambos rectángulos sobre los '
     'cuatro ejes de sus lados; si en alguno de ellos las proyecciones no se tocan, los muebles no chocan. '
     'Gracias a esta función ningún mueble se encima a otro ni bloquea una puerta.'),
    ('estimate/FSHFPurchaseSpec.java', 'public double quantity(', 'Cálculo de la cantidad de material',
     'Convierte el área en metros cuadrados en la cantidad que hay que comprar: multiplica por el número de '
     'manos (dos en la pintura), suma el desperdicio (10 % en cerámico, madera y papel tapiz), divide entre '
     'el rendimiento (35 m² por galón de pintura) y redondea hacia arriba a la unidad de venta.'),
    ('persistence/FSHFDatabase.java', 'private void migrate(', 'Actualización de la base de datos',
     'Cada versión de Space Cat puede necesitar cambios en las tablas. Esta función lee la versión del '
     'esquema guardada en el archivo de la base de datos y aplica, uno por uno y dentro de una transacción, '
     'los pasos que faltan. Así los proyectos del usuario se conservan al actualizar la aplicación.'),
    ('persistence/FSHFRecovery.java', 'public void write(', 'Autoguardado del trabajo sin guardar',
     'Escribe una copia del proyecto abierto en `recuperacion.json`. Primero la guarda en un archivo temporal '
     'y luego lo reemplaza de una sola vez, para que un corte de luz a mitad de la escritura no deje un '
     'archivo dañado. Si la aplicación se cierra de golpe, al abrirla ofrece recuperar esta copia.'),
]


def miles(n):
    return f'{n:,}'.replace(',', ' ')


def lineas(p):
    return len(p.read_text(encoding='utf-8').splitlines())


def extraer(texto, firma):
    filas = texto.split('\n')
    if firma is None:
        return 1, filas[:len(filas) - (1 if filas[-1] == '' else 0)]
    i = next(k for k, l in enumerate(filas) if firma in l)
    while i > 0 and filas[i - 1].strip().startswith('@'):
        i -= 1
    nivel, j = 0, i
    for j in range(i, len(filas)):
        nivel += filas[j].count('{') - filas[j].count('}')
        if nivel == 0 and '{' in ''.join(filas[i:j + 1]):
            break
    return i + 1, filas[i:j + 1]


def listado(inicio, filas):
    ancho = len(str(inicio + len(filas) - 1))
    return '\n'.join(f'{inicio + k:>{ancho}}  {l.rstrip()}' for k, l in enumerate(filas))


def arbol():
    t = [
        'amoblador3d/',
        '├── src/',
        '│   ├── main/',
        '│   │   ├── java/pe/edu/unaj/amoblador3d/     código del programa (Java)',
    ]
    for p, d in PAQUETES[1:]:
        n = len(list((BASE / p).glob('*.java')))
        t.append(f'│   │   │   ├── {p + "/":<14}{n:>3} archivos   {CORTO.get(p, d.split(":")[0])}')
    t += [
        f'│   │   │   └── FSHFApp.java, FSHFLauncher.java   arranque',
        '│   │   └── resources/',
        '│   │       ├── ui/app.css          estilos de la interfaz (tema oscuro)',
        '│   │       ├── assets/             catálogo y modelos 3D incluidos',
        '│   │       └── icon.png            ícono de la aplicación',
        f'│   └── test/                       {len(list((RAIZ / "src" / "test").rglob("*.java")))} archivos de pruebas automáticas (JUnit 5)',
        '├── database/                       scripts SQL (SQLite y PostgreSQL)',
        '├── packaging/                      scripts que crean el instalador para Windows',
        '├── documentacion/                  manuales y documentos del proyecto',
        '├── .mvn/, mvnw, mvnw.cmd           Maven Wrapper (descarga Maven solo)',
        '├── pom.xml                         dependencias y construcción del programa',
        '├── Ejecutar-Amoblador3D.cmd        abre la aplicación con doble clic',
        '└── README.md                       presentación del proyecto',
    ]
    return '\n'.join(t)


def dependencias():
    filas = []
    nombres = {
        'javafx-controls': ('JavaFX', 'Interfaz gráfica y vista 3D'),
        'sqlite-jdbc': ('SQLite JDBC', 'Base de datos local de los proyectos'),
        'gson': ('Gson', 'Lectura y escritura de JSON (.amob3d)'),
        'pdfbox': ('Apache PDFBox', 'Informe PDF'),
        'junit-jupiter': ('JUnit 5', 'Pruebas automáticas'),
    }
    for art, (nombre, uso) in nombres.items():
        m = re.search(r'<artifactId>' + art + r'</artifactId>\s*<version>([^<]+)</version>', POM)
        v = m.group(1) if m else '—'
        if v.startswith('${'):
            v = re.search('<' + v[2:-1] + r'>([^<]+)<', POM).group(1)
        filas.append(f'| {nombre} | {v} | {uso} |')
    return filas


def main():
    archivos = sorted(JAVA.rglob('*.java'))
    total = sum(lineas(f) for f in archivos)
    md = [
        '---', 'codigo: AMB3D-CDF-09', f'proyecto: Space Cat {VERSION}', '---', '',
        '# Código fuente', '',
        '## 1. Introducción', '',
        f'Este documento presenta el código fuente de **Space Cat {VERSION}**, la aplicación de escritorio '
        'del equipo FSHF para diseñar y amoblar ambientes en 3D con medidas reales y estimar su presupuesto. '
        'Explica cómo está organizado el proyecto y reproduce, tal como están en el programa, los fragmentos '
        'de código más representativos.', '',
        f'El programa completo tiene {len(archivos)} archivos Java con {miles(total)} líneas. Está escrito en '
        '**Java 21**, usa **JavaFX** para la interfaz y la vista 3D y **SQLite** para guardar los proyectos, y se '
        'construye con **Maven**. El código completo está disponible en el repositorio del proyecto:', '',
        f'<{REPO}>', '',
        '## 2. Estructura del proyecto', '',
        'El proyecto sigue la estructura estándar de Maven. El código del programa está en `src/main/java`, '
        'dividido en paquetes según su responsabilidad; los recursos (estilos, catálogo y modelos 3D) están en '
        '`src/main/resources`, y las pruebas automáticas, en `src/test`.', '',
        '```text', arbol(), '```', '',
        'La tabla siguiente resume qué contiene cada paquete del programa:', '',
        '<!-- Tabla: Paquetes del programa, contenido, archivos y líneas -->',
        '| Paquete | Contenido | Archivos | Líneas |', '|---|---|---|---|',
    ]
    for p, d in PAQUETES:
        fs = sorted((BASE / p).glob('*.java'))
        nombre = 'pe.edu.unaj.amoblador3d' + ('.' + p if p else '')
        md.append(f'| `{nombre}` | {d} | {len(fs)} | {miles(sum(lineas(f) for f in fs))} |')
    md += ['', 'Todas las clases del programa llevan el prefijo **FSHF**, las iniciales del equipo, para '
           'distinguirlas de las clases de Java y de las librerías.', '',
           '## 3. Tecnologías y librerías', '',
           '<!-- Tabla: Librerías que usa el programa, versión y uso -->',
           '| Librería | Versión | Uso |', '|---|---|---|', *dependencias(), '',
           'Las librerías no se copian al proyecto: Maven las descarga la primera vez que se compila, según lo '
           'indicado en `pom.xml`.', '',
           '## 4. Fragmentos principales del código', '',
           'Los fragmentos siguientes se copiaron sin cambios del código del programa. El número de la izquierda '
           'es el número de línea dentro del archivo indicado.', '']
    for k, (rel, firma, titulo, explicacion) in enumerate(FRAGMENTOS, 1):
        texto = (BASE / rel).read_text(encoding='utf-8')
        inicio, filas = extraer(texto, firma)
        ruta = 'src/main/java/pe/edu/unaj/amoblador3d/' + rel
        md += [f'### 4.{k} {titulo}', '',
               f'Archivo `{ruta}`, líneas {inicio} a {inicio + len(filas) - 1}.', '',
               explicacion, '',
               f'<!-- Figura: {titulo} ({Path(rel).name}) -->',
               '```java', listado(inicio, filas), '```', '']
    md += ['## 5. Cómo obtener y compilar el código completo', '',
           '1. Descargar el proyecto con Git:', '',
           '   ```powershell', f'   git clone {REPO}.git', '   cd amoblador3d', '   ```', '',
           '2. Ejecutar la aplicación (Maven y las librerías se descargan solos la primera vez):', '',
           '   ```powershell', '   .\\mvnw.cmd javafx:run', '   ```', '',
           '3. Ejecutar las pruebas automáticas:', '',
           '   ```powershell', '   .\\mvnw.cmd test', '   ```', '',
           'El paso a paso completo, con los requisitos y los problemas frecuentes, está en el manual de '
           'instalación y ejecución (documento 06).', '',
           '## Historial del documento', '',
           '<!-- Tabla: Historial de cambios de este documento -->',
           '| Versión | Fecha | Cambios | Responsable |', '|---|---|---|---|',
           f'| 1.0 | {date.today().isoformat()} | Estructura del proyecto, librerías y fragmentos principales del '
           'código | Equipo FSHF |', '']
    SALIDA.write_text('\n'.join(md), encoding='utf-8')
    print(f'{SALIDA.name}: estructura y {len(FRAGMENTOS)} fragmentos')


if __name__ == '__main__':
    main()
