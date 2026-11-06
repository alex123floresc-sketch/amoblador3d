# 12 · Historial de versiones (CHANGELOG)

Formato basado en *Keep a Changelog*. Fechas en formato AAAA-MM-DD.

## [2.0.1 — Documentación] — 2026-11-06 — `77cf676`

### Cambiado
- Los Word de `documentacion/doc 2` usan la **carátula institucional de la UNAJ** (logo, facultad,
  escuela, integrantes, docente y asignatura) y un **encabezado** con el logo en todas las páginas
  salvo la carátula; el número de página pasa al pie.
- Cada documento incluye **índice de tablas** e **índice de figuras** con título específico y página.
- Todas las tablas y diagramas tienen un título propio (`<!-- Tabla: … -->` en el Markdown).
- Datos del equipo completados en el acta, el informe final y el índice.
- Casos de uso CU-04 a CU-11 con ficha completa (requisitos, precondición, postcondición y flujos alternativos).
- Cada documento técnico termina con su historial de cambios.

## [2.0.0 — Fase 8] — 2026-11-04 — `6d277dc`…`56587b4` (rama `fase-7-casa-y-disenador`)

### Agregado
- Seis tipos de puerta: normal, principal, doble, corrediza, portón de cochera y vano sin puerta.
  En una casa, la puerta principal y el portón se colocan solos en una pared que da a la calle.
- Escaleras recta, en L y de caracol, y baranda; las escaleras llegan solas al techo.
- Giro de ambientes de 90 en 90° y hasta 4 pisos; en 3D se ven el piso elegido y los de abajo.
- Paso automático: si la puerta de un ambiente toca el muro de otro, ese muro se abre.
- Aviso si ninguna puerta de la casa da a la calle.
- **Registro de errores** (`util/AppLog`): archivo `logs/amoblador3d-0.log` con versión, Java,
  sistema, avisos y trazas; los errores no controlados se registran y se avisa al usuario.
  Menú *Ayuda → Abrir registro de errores*. *Acerca de* muestra la versión.
- **Integración continua** con GitHub Actions (Windows, Java 21): compila, prueba y publica el `.jar`.
- **Cobertura de pruebas** con JaCoCo (`mvnw verify`) y umbral mínimo del 70 % en los paquetes de dominio y datos.
- 13 pruebas del lector de modelos 3D (`GltfLoaderTest`): `.gltf`, `.glb`, transformaciones, materiales y archivos dañados.
- Verificación de versiones mínimas (JDK 21, Maven 3.6.3) con maven-enforcer.
- Carpeta `documentacion/` con la documentación completa del proyecto.
- Documentos Word con normas APA 7 (`documentacion/doc 2`) y documento 13 de normas de redacción.
- Herramienta de capturas de pantalla (`Capturas`, en las pruebas) y 11 capturas reales en
  `documentacion/capturas`.
- Manual de usuario 2.0 (capturas, tutoriales, mensajes del sistema), manual técnico 2.0 (arranque,
  ciclo de edición, pruebas, depuración, publicación) e informe final con antecedentes verificados y
  discusión.
- Los fallos del autoguardado también quedan en el registro de errores.

### Cambiado
- El catálogo de muebles del panel izquierdo pasa de `MainWindow` a su propia clase `ui/CatalogPanel` (sin cambios para el usuario).
- Base de datos: esquema **v9** (`home_space.rotation`, `home_space.level`, nuevos tipos en `opening`).
- El `.jar` lleva `Implementation-Version` y `Enable-Native-Access` en el manifiesto; las pruebas y
  `javafx:run` usan `--enable-native-access=ALL-UNNAMED` (sin avisos de SQLite en Java 22+).

## [Fase 7] — 2026-10-27 — `fe6f6be`
- Casa con varios ambientes y vista *Casa completa* (tabla `home_space`, v8).
- Diseñador de muebles por piezas (tabla `custom_design`, v7); los diseños viajan en el `.amob3d`.
- Color secundario, acabado, tinte de modelos 3D, escala ±10 %.
- Asas en 3D para estirar, cambiar el alto y girar cada 15°.

## [Fase 6] — 2026-10-25 — `3c42b81`
- Columnas, muros internos y habitación en L (tabla `room_column`, v6).
- Techo con material y zócalo configurable; ambos entran en el presupuesto.

## [Fase 5] — 2026-10-25 — `2df95ef`
- Autoguardado cada minuto con recuperación tras un cierre inesperado.
- Versión para Windows con Java incluido (`jpackage`).

## [Fase 4] — 2026-10-25 — `e99db7b`
- Recorrido en primera persona, vistas guardadas (tabla `camera_view`, v5) y modo noche.

## [Fase 3] — 2026-10-25 — `650b5cc`
- Nueve muebles paramétricos nuevos, catálogo por categorías con buscador y plantillas.

## [Fase 2] — 2026-10-25 — `a690de5`
- Imán con guías, selección múltiple, copiar/pegar entre proyectos y bloqueo de muebles (v4).

## [Fase 1] — 2026-10-25 — `8ef40e5`
- Metrado, presupuesto con precios editables (tabla `price`, v3), CSV, informe PDF y herramienta Medir.

## [1.x] — 2026-10-25 — `ff7373c`, `4f7ada2`, `5b58630`, `65f8e6e`
- Migración de Electron + Three.js a Java + JavaFX + SQLite (esquema v2).
- Correcciones de interfaz (foco, fuga de memoria de texturas, ventana en 1366 × 768) y primeras pruebas.
- Scripts de base de datos SQLite y PostgreSQL y guía de ejecución.

## [0.x] — 2026-10-21 — `d6d7335`
- Versión Electron + Three.js (respaldo antes de migrar a Java).
