# Amoblador3D

Diseñador de interiores 3D con medidas reales, hecho en **Java 21 + JavaFX + SQLite**.

Defines las medidas de la habitación, colocas puertas, ventanas, luces y muebles, eliges el
material y el color de cada pared y del piso, y ves el resultado en 3D o en planta.

## Cómo ejecutarlo

Solo necesitas Java 21 o superior. Maven no hace falta instalarlo: el proyecto trae el
**Maven Wrapper** (`mvnw`), que lo descarga la primera vez.

**Lo más fácil (Windows):** doble clic en **`Ejecutar-Amoblador3D.cmd`**. En IntelliJ, elige la
configuración **Amoblador3D** (arriba a la derecha) y pulsa ▶. Ojo: un doble clic en `mvnw.cmd`
solo no abre nada, porque no sabe qué hacer.

```powershell
.\mvnw.cmd javafx:run          # abrir la aplicación (Windows)
.\mvnw.cmd test                # ejecutar las pruebas
.\mvnw.cmd package             # crear target\amoblador3d.jar (incluye todo)
java -jar target\amoblador3d.jar
```

En Linux/macOS usa `./mvnw` en lugar de `.\mvnw.cmd`.

Guía completa paso a paso (instalar Java, IDEs, bases de datos y problemas frecuentes):
**[GUIA_EJECUCION.md](GUIA_EJECUCION.md)**.

## Funciones

- **Habitación**: ancho, largo, alto y grosor de muro; ventanas y puertas que no se superponen.
- **Paredes, piso y techo**: 11 materiales (pintura, ladrillo, piedra, madera, cerámico, mármol,
  porcelanato, alfombra...) con texturas a escala real y cualquier color. El techo se ve en el
  recorrido. Zócalo con alto (o sin zócalo) y color a elección.
- **Columnas, muros internos y habitación en L** (pestaña *Habitación*): bloques del piso al techo
  con su propio acabado. Los muebles no los atraviesan, no tapan ventanas ni puertas, el imán se
  pega a ellos y el metrado los descuenta del piso y suma sus caras a la pintura.
- **Muebles**: 14 paramétricos (cama, mesa de noche, cómoda, closet, sofá, mueble de TV, librero,
  mesa, silla, mueble de cocina, refrigeradora, inodoro, lavatorio y escritorio), tus propios
  diseños y modelos 3D `.glb/.gltf` (incluidos o importados), agrupados por categoría y con
  buscador. No chocan entre sí, con las paredes ni con el paso de las puertas.
- **Casa con varios ambientes**: un proyecto puede tener sala, dormitorios, cocina, baño… Arriba
  de la vista está la barra de ambientes: clic para decorar uno, **＋ Ambiente** para agregar
  (vacío, desde una plantilla o copiando el actual) y clic derecho para renombrar, duplicar o
  eliminar. **🏠 Casa completa** (Ctrl+4) muestra todos juntos en 3D o en planta: se arrastran para
  armar la distribución (los muros se pegan a los de los demás y nunca se enciman; Alt sin imán),
  se cambian sus medidas desde el panel y se ve el área y el presupuesto de cada ambiente y de toda
  la casa. Doble clic en un ambiente para decorarlo. Se guarda todo junto (base de datos, archivo
  `.amob3d` y autoguardado) y Ctrl+Z deshace también los cambios de la casa.
- **Personalizar cada mueble** (panel derecho): ancho, profundidad y alto, agrandar o achicar todo
  un 10 %, volver a las medidas originales; **color principal** y **color secundario** (frazada,
  cojines, frentes de cajones, puertas, patas, encimera, grifería…) y **acabado** (mate, satinado,
  brillante o metálico). Los modelos 3D se pueden **teñir** de cualquier color.
- **Diseñador de muebles** (botón *✎ Diseñar mueble*, Ctrl+Shift+D): arma tu propio mueble con
  piezas (cajas, cilindros, esferas y barras) en una vista 3D que se gira con el ratón. Cada pieza
  tiene medidas y posición en centímetros, y se pinta con el color principal, el secundario o uno
  propio, con su acabado. Incluye plantillas (mesa, mesa redonda, estante, banco) y puede empezar
  desde cualquier mueble del catálogo; con *Diseñar a partir de este…* se edita, pieza por pieza,
  el mueble seleccionado y se reemplaza en la habitación. Los diseños se guardan en la categoría
  que elijas (por defecto *Mis diseños*); clic derecho sobre su botón para editarlos, duplicarlos o
  quitarlos. Al exportar un `.amob3d` se incluyen los diseños que usa, así se abre igual en otra PC.
- **Plantillas** (*Archivo → Nuevo desde plantilla*, Ctrl+Shift+N): dormitorio, sala, comedor,
  cocina, baño y oficina listos para empezar, con vista previa.
- **Luces**: bombillas, focos y sol que entra por una ventana.
- **Proyectos**: se guardan en SQLite con miniatura; también se exportan/importan como `.amob3d`
  (JSON) y la vista se exporta como PNG.
- **Metrado y presupuesto** (*Proyecto → Metrado y presupuesto*, Ctrl+P): m² de piso y de paredes
  sin ventanas ni puertas, galones de pintura (2 manos), rollos de papel tapiz, m² de cerámico o
  madera con 10 % de desperdicio, metros de zócalo y los muebles. Los precios son referenciales en
  soles, se cambian con doble clic y se guardan para todos los proyectos. Se exporta a CSV (Excel).
- **Informe PDF** (Ctrl+Shift+P): planta con cotas, vista 3D, ventanas y puertas, muebles y presupuesto.
- **Medir distancias** (tecla M): dos clics sobre el piso; los puntos se pegan a las paredes y a las
  esquinas de los muebles.
- **Edición cómoda**: imán al arrastrar (se pega a paredes y a bordes o centros de otros muebles,
  con guías; tecla G, Alt para soltarlo un momento), selección múltiple (Shift/Ctrl + clic,
  rectángulo con Shift + arrastrar, Ctrl+A) para mover, girar, duplicar o eliminar en grupo,
  copiar/cortar/pegar muebles entre proyectos (Ctrl+C/X/V) y bloquear muebles (L).
- **Recorrido en primera persona** (Ctrl+3): caminar por la habitación a la altura de los ojos
  (W/S o ↑/↓ avanzar, A/D de costado, ←/→ girar, arrastrar para mirar, Shift para correr), sin
  atravesar paredes ni muebles.
- **Vistas** (botón *Vistas*): guardar la vista actual con un nombre (se guarda con el proyecto),
  volver a ella y ver la habitación desde cada puerta.
- **Modo noche** (tecla N o pestaña *Luz*): sin sol y con poca luz ambiental, para ver las lámparas.
- **Autoguardado**: copia cada minuto del trabajo sin guardar; si la aplicación se cierra de golpe,
  al abrirla ofrece recuperarlo.
- **Versión para Windows sin instalar Java**: `packaging\crear-instalador.cmd` (ver la guía).
- Deshacer/rehacer, vista 3D y planta 2D, cotas.

### Atajos

| Tecla | Acción |
|---|---|
| Ctrl+N / Ctrl+O / Ctrl+S | Nuevo / Abrir / Guardar |
| Ctrl+Shift+N | Nuevo desde plantilla |
| Ctrl+Z / Ctrl+Y | Deshacer / Rehacer |
| Flechas (Shift: más fino) | Mover el mueble 5 cm (1 cm) |
| R / Shift+R | Girar el mueble 90° |
| Q / E | Girar el mueble 15° |
| Supr / Ctrl+D | Eliminar / Duplicar |
| Ctrl+C / Ctrl+X / Ctrl+V | Copiar / Cortar / Pegar muebles |
| Ctrl+A | Seleccionar todos los muebles |
| Shift o Ctrl + clic, Shift + arrastrar | Agregar a la selección, rectángulo de selección |
| L / G | Bloquear o desbloquear / Imán |
| Ctrl+1 / Ctrl+2 / Ctrl+3 | Vista 3D / Planta 2D / Recorrido |
| Ctrl+4 | Casa completa (todos los ambientes) |
| N | Modo noche |
| Ctrl+P / Ctrl+Shift+P | Presupuesto / Informe PDF |
| Ctrl+Shift+D | Diseñador de muebles (dentro: Ctrl+Z/Y, Ctrl+D duplicar pieza, Supr eliminar, flechas mover) |
| M | Medir distancias (Esc para terminar) |
| F1 | Todos los atajos |

Ratón en 3D: botón izquierdo gira, derecho mueve, rueda acerca. Arrastra un mueble para moverlo.

## Agregar muebles al catálogo

### Opción A: desde la aplicación (recomendada)

*Archivo → Importar modelo 3D* (o *Importar modelo .glb/.gltf…* en la pestaña Muebles). Elige el
archivo, revisa sus medidas reales y listo. El modelo se copia a la carpeta de datos
(*Archivo → Abrir carpeta de modelos importados*). Clic derecho sobre su botón para quitarlo.

### Opción B: incluirlo en la aplicación, editando `catalog.json`

1. Copia el modelo `.glb` (o `.gltf` con sus `.bin` y texturas) en `src/main/resources/assets/models/`.
2. Agrega una entrada en `src/main/resources/assets/catalog.json`:

```json
{ "id": "mi-sillon", "name": "Sillón", "file": "models/mi-sillon.glb",
  "width": 0.9, "depth": 0.85, "height": 0.95, "rotation": 0, "category": "Sala" }
```

| Campo | Significado |
|---|---|
| `id` | Identificador único (sin espacios). Se guarda en los proyectos. |
| `name` | Nombre visible en el botón. |
| `file` | Ruta del modelo, relativa a `assets/`. |
| `width`, `depth`, `height` | Medidas **reales** en metros (ancho X, profundidad Z, alto Y). |
| `rotation` | Grados para que el frente del modelo mire a +Z (0, 90, 180 o 270). |
| `category` | Opcional: Dormitorio, Sala, Comedor, Cocina, Baño, Oficina o Decoración (por defecto). |

3. Vuelve a compilar (`.\mvnw.cmd javafx:run`).

El modelo se escala a las medidas indicadas aunque haya sido modelado en centímetros o milímetros.

**Modelos gratuitos (.glb):** [Poly Haven](https://polyhaven.com/models),
[Sketchfab](https://sketchfab.com) (filtrar por *Downloadable*), [Kenney](https://kenney.nl/assets)
(*Furniture Kit*). Revisa la licencia de cada modelo. No se admiten modelos comprimidos con Draco/Meshopt.

### Opción C: mueble paramétrico (por código)

Crea su método en `scene/ParametricBuilder.java`, regístralo en `build`, declara sus medidas y su
categoría en `catalog/Catalog.java` y su precio referencial en `estimate/PriceList.java`.
Regla: el mueble ocupa exactamente `x ∈ [-ancho/2, ancho/2]`, `z ∈ [-prof/2, prof/2]`,
`y ∈ [0, alto]`, con el frente hacia +Z (`ParametricBuilderTest` lo revisa).

## Formato del proyecto (.amob3d)

Es JSON legible (el mismo de la versión Electron, más `surfaces`):

```json
{
  "app": "Amoblador3D", "version": 1,
  "room": { "width": 4, "length": 3.5, "height": 2.5, "wallThickness": 0.15 },
  "surfaces": { "piso": { "material": "madera", "color": "#b07d4f" },
                "pared-norte": { "material": "ladrillo", "color": "#a4553a" } },
  "openings": [{ "id": "window-1", "type": "window", "wall": "norte", "offset": 1.4, "width": 1.2, "height": 1, "sill": 0.9 }],
  "ambient": 0.6,
  "lights": [{ "id": "point-1", "type": "point", "color": "#ffe2b8", "intensity": 6, "x": 2, "y": 2.25, "z": 1.75 }],
  "furniture": [{ "id": "cama-1", "type": "cama", "x": 2, "z": 1.6, "rotation": 0, "width": 1.4, "depth": 1.9, "height": 0.9, "color": "#8b6b4a" }]
}
```

Las medidas de la habitación son **interiores** (el espacio libre); los muros crecen hacia afuera.
Coordenadas: el origen es la esquina interior **noroeste** a nivel del piso; X crece hacia el este,
Z hacia el sur, Y hacia arriba.

## Datos

Los proyectos y los modelos importados se guardan en `%APPDATA%\Amoblador3D`
(se puede cambiar con `-Damoblador.home=C:\otra\carpeta`).

En `database/` están los scripts de la base de datos: `amoblador3d_sqlite.sql` (el esquema que
usa la aplicación, con un proyecto de ejemplo) y `amoblador3d_postgresql.sql` (la misma base en
PostgreSQL, con vistas de consulta). Cómo cargarlos: [GUIA_EJECUCION.md](GUIA_EJECUCION.md#bases-de-datos).

## Estructura del código

```
src/main/java/pe/edu/unaj/amoblador3d/
  Launcher, App   arranque
  model/          datos del proyecto (sin JavaFX)
  design/         reglas: colisiones, aberturas, luces, deshacer; campos del panel de propiedades
  persistence/    SQLite (con migraciones) y formato .amob3d
  catalog/        catálogo de muebles e importación de modelos
  gltf/           lector de modelos glTF 2.0
  scene/          vista 3D: habitación, texturas, luces, muebles, cotas, cámara
  ui/             ventana principal, paneles y diálogos
src/test/java/    pruebas de reglas, persistencia e historial
database/         scripts SQL (SQLite y PostgreSQL)
```
