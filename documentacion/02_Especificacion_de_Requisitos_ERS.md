# 02 · Especificación de requisitos de software (ERS)

Basado en IEEE 830-1998 e ISO/IEC/IEEE 29148:2018.
Versión 1.0 · noviembre de 2026 · Software: Amoblador3D 2.0.0

## 1. Introducción

### 1.1 Propósito

Este documento define qué debe hacer Amoblador3D (requisitos funcionales) y con qué calidad
(requisitos no funcionales). Sirve como acuerdo entre el equipo y el docente, y como base para las
pruebas del documento [06](06_Plan_y_Reporte_de_Pruebas.md).

### 1.2 Alcance

Amoblador3D es una aplicación de escritorio para diseñar ambientes de una vivienda en 3D con
medidas reales, amoblarlos respetando reglas físicas y estimar el costo de sus acabados. El alcance
detallado está en el documento [01](01_Acta_de_Constitucion_y_Vision.md#4-alcance).

### 1.3 Definiciones

| Término | Significado |
|---|---|
| Ambiente | Una habitación de la casa (sala, dormitorio…), con sus medidas, aberturas, luces y muebles |
| Abertura | Ventana o puerta sobre una pared |
| Vano | Abertura sin hoja de puerta (paso libre) |
| Mueble paramétrico | Mueble dibujado por código a partir de ancho, profundidad y alto |
| Metrado | Cálculo de cantidades de obra: m² de pared y piso, metros de zócalo |
| Imán | Ayuda que pega el mueble a una pared o a otro mueble al arrastrarlo |
| `.amob3d` | Archivo del proyecto en formato JSON |
| Medidas interiores | Espacio libre entre muros; los muros crecen hacia afuera |

### 1.4 Referencias

- IEEE Std 830-1998, *Recommended Practice for Software Requirements Specifications*.
- ISO/IEC 25010:2023, *Product quality model*.
- Reglamento Nacional de Edificaciones (Perú), Norma A.010 — referencia para medidas mínimas de puertas.

## 2. Descripción general

### 2.1 Perspectiva del producto

Aplicación autónoma (no depende de servidores). Guarda los datos localmente en SQLite dentro de
`%APPDATA%\Amoblador3D`. Puede importar modelos 3D estándar (glTF 2.0) y exportar PNG, CSV, PDF y
`.amob3d`.

### 2.2 Usuarios

| Perfil | Conocimientos | Uso principal |
|---|---|---|
| Usuario doméstico | Computación básica | Probar distribuciones y colores de su casa |
| Estudiante de arquitectura o diseño | Medio | Bocetos rápidos y recorridos |
| Contratista / carpintero | Medio | Mostrar propuestas y calcular materiales |

### 2.3 Entorno operativo

- Windows 10/11 (principal); Linux y macOS con Java 21+.
- Java 21 o superior (o el instalador que ya lo incluye).
- Tarjeta de video con aceleración 3D; pantalla de 1280 × 720 o más.

### 2.4 Restricciones de diseño

- Lenguaje Java 21, interfaz JavaFX 21, base de datos SQLite, construcción con Maven.
- Sin conexión a internet obligatoria.
- Medidas en metros y precios en soles (S/).

## 3. Requisitos funcionales

Prioridad: **A** = esencial, **M** = importante, **B** = deseable. Todos están implementados en la
versión 2.0.0.

### 3.1 Ambiente y casa

| Código | Requisito | Prioridad |
|---|---|---|
| RF-01 | El sistema debe permitir definir ancho y largo (1 a 30 m), alto (2 a 6 m) y grosor de muro (0,05 a 0,50 m) de un ambiente. | A |
| RF-02 | El sistema debe permitir agregar ventanas y seis tipos de puerta (normal, principal, doble, corrediza, portón de cochera y vano) en cualquiera de las cuatro paredes. | A |
| RF-03 | El sistema debe impedir que dos aberturas se superpongan y mantener una separación mínima de 5 cm con esquinas, techo y otras aberturas. | A |
| RF-04 | Al reducir las medidas del ambiente, el sistema debe eliminar las aberturas que ya no caben y avisar al usuario. | M |
| RF-05 | El sistema debe permitir agregar columnas, muros internos y recortar una esquina (habitación en L). | M |
| RF-06 | El sistema debe permitir crear una casa con varios ambientes (hasta 30), moverlos, girarlos de 90 en 90° y ubicarlos en hasta 4 pisos. | A |
| RF-07 | El sistema debe impedir que dos ambientes del mismo piso se encimen y pegar sus muros al arrastrarlos. | A |
| RF-08 | Cuando la puerta de un ambiente toque el muro de otro, el sistema debe abrir el paso en ese muro. | M |
| RF-09 | El sistema debe avisar si ninguna puerta de la casa da al exterior. | B |

### 3.2 Acabados

| Código | Requisito | Prioridad |
|---|---|---|
| RF-10 | El sistema debe permitir elegir material y color de cada pared, del piso y del techo entre 11 materiales. | A |
| RF-11 | El sistema debe impedir materiales que no corresponden a la superficie (por ejemplo, alfombra en una pared). | M |
| RF-12 | El sistema debe permitir aplicar un acabado a todas las paredes a la vez. | B |
| RF-13 | El sistema debe permitir configurar alto y color del zócalo, o quitarlo (alto 0). | B |

### 3.3 Muebles

| Código | Requisito | Prioridad |
|---|---|---|
| RF-14 | El sistema debe ofrecer un catálogo de muebles agrupado por categorías con buscador que ignore mayúsculas y tildes. | A |
| RF-15 | Al agregar un mueble, el sistema debe ubicarlo en un lugar libre; si no cabe derecho, debe probar girado; si no cabe, debe explicar por qué. | A |
| RF-16 | El sistema debe impedir que un mueble choque con paredes, columnas, otros muebles o el área de giro de una puerta, y que tape ventanas. | A |
| RF-17 | El sistema debe permitir mover, girar (90° y 15°), redimensionar, duplicar, eliminar y bloquear muebles. | A |
| RF-18 | El sistema debe permitir seleccionar varios muebles y moverlos, girarlos, duplicarlos o eliminarlos en grupo. | M |
| RF-19 | El sistema debe permitir copiar, cortar y pegar muebles, incluso entre proyectos. | M |
| RF-20 | El sistema debe permitir cambiar color principal, color secundario y acabado (mate, satinado, brillante, metálico) de cada mueble. | M |
| RF-21 | El sistema debe permitir importar modelos 3D `.glb/.gltf` indicando sus medidas reales. | M |
| RF-22 | El sistema debe incluir un diseñador de muebles por piezas (cajas, cilindros, esferas, barras) que guarde los diseños en el catálogo. | M |
| RF-23 | Al arrastrar un mueble, el imán debe pegarlo a paredes y bordes o centros de otros muebles si está a menos de 8 cm. | B |

### 3.4 Visualización

| Código | Requisito | Prioridad |
|---|---|---|
| RF-24 | El sistema debe mostrar el ambiente en vista 3D, planta 2D con cotas y recorrido en primera persona. | A |
| RF-25 | En el recorrido, el usuario no debe atravesar paredes ni muebles. | M |
| RF-26 | El sistema debe permitir agregar luces (bombilla, foco y sol que entra por una ventana) y activar un modo noche. | M |
| RF-27 | El sistema debe permitir guardar vistas de cámara con nombre dentro del proyecto. | B |
| RF-28 | El sistema debe permitir medir distancias sobre el piso con puntos que se pegan a paredes y esquinas de muebles. | B |

### 3.5 Presupuesto e informes

| Código | Requisito | Prioridad |
|---|---|---|
| RF-29 | El sistema debe calcular m² de piso y paredes descontando ventanas, puertas y columnas, y los metros de zócalo. | A |
| RF-30 | El sistema debe calcular galones de pintura (35 m² por galón, 2 manos, redondeado al cuarto de galón), rollos de papel tapiz y m² de piso con 10 % de desperdicio. | A |
| RF-31 | El sistema debe mostrar un presupuesto en soles con precios referenciales que el usuario puede editar y que se conservan para todos los proyectos. | A |
| RF-32 | El sistema debe exportar el presupuesto a CSV y generar un informe PDF con planta acotada, vista 3D, listas y presupuesto. | M |
| RF-33 | El sistema debe exportar la vista actual como imagen PNG. | B |

### 3.6 Proyectos

| Código | Requisito | Prioridad |
|---|---|---|
| RF-34 | El sistema debe guardar, abrir, renombrar y eliminar proyectos en una base de datos local, con miniatura. | A |
| RF-35 | El sistema debe impedir dos proyectos con el mismo nombre (sin distinguir mayúsculas). | M |
| RF-36 | El sistema debe exportar e importar proyectos como archivos `.amob3d`, incluyendo los diseños propios que usen. | A |
| RF-37 | El sistema debe deshacer y rehacer hasta 100 pasos. | A |
| RF-38 | El sistema debe hacer una copia automática cada 60 s del trabajo sin guardar y ofrecer recuperarla si la aplicación se cerró de golpe. | A |
| RF-39 | El sistema debe ofrecer plantillas (dormitorio, sala, comedor, cocina, baño, oficina) y una casa de ejemplo. | M |
| RF-40 | El sistema debe avisar si hay cambios sin guardar al cerrar o abrir otro proyecto. | A |

## 4. Requisitos no funcionales

Clasificados según ISO/IEC 25010.

| Código | Característica | Requisito | Cómo se verifica |
|---|---|---|---|
| RNF-01 | Adecuación funcional | Las medidas se manejan en metros con precisión de 1 cm. | Pruebas `DesignTest`, `EstimateTest` |
| RNF-02 | Eficiencia | Abrir un proyecto típico (1 ambiente, 15 muebles) en menos de 2 s; arrastrar muebles sin saltos visibles. | Prueba manual |
| RNF-03 | Eficiencia | Al cambiar algo, solo se reconstruye la parte de la escena 3D afectada. | Diseño (`Design.Change`, `FurnitureView`) |
| RNF-04 | Usabilidad | Toda acción frecuente tiene atajo de teclado y tooltip; F1 muestra todos los atajos. | Revisión de interfaz |
| RNF-05 | Usabilidad | Los mensajes de error explican la causa en español sencillo ("No hay espacio libre para la cama"). | Pruebas `siNoCabeElMuebleSeExplicaPorQue` |
| RNF-06 | Fiabilidad | Guardar es atómico: si falla a la mitad, la base queda como estaba (transacciones). | `Database.transaction` |
| RNF-07 | Fiabilidad | Un archivo o registro dañado no impide abrir el resto del proyecto; se corrige o descarta con aviso. | Pruebas `superficiesDanadasSeCorrigen…`, `unaVistaDanadaSeDescarta…` |
| RNF-08 | Fiabilidad | Los errores inesperados se registran con su traza en un archivo de registro y se avisa al usuario. | `AppLogTest` |
| RNF-09 | Mantenibilidad | Las reglas del negocio no dependen de JavaFX, para poder probarlas sin interfaz. | Paquetes `model`, `design`, `estimate` |
| RNF-10 | Mantenibilidad | El esquema de la base se actualiza solo entre versiones sin perder datos. | Prueba `unaBaseDeLaVersionAnteriorSeActualiza` |
| RNF-11 | Mantenibilidad | El conjunto de pruebas automáticas pasa al 100 % antes de cada entrega; integración continua en GitHub Actions. | `mvnw verify`, `.github/workflows/ci.yml` |
| RNF-12 | Portabilidad | Funciona en Windows, Linux y macOS con Java 21+; en Windows existe una versión que no requiere instalar Java. | `packaging/crear-instalador.ps1` |
| RNF-13 | Compatibilidad | Los archivos `.amob3d` de versiones anteriores se siguen abriendo. | Prueba `archivoAntiguoSinSuperficies…` |
| RNF-14 | Seguridad | Los datos se guardan solo en la carpeta del usuario; no se envía información por internet. Las consultas SQL usan parámetros (`PreparedStatement`). | Revisión de código |

## 5. Reglas de negocio

| Código | Regla |
|---|---|
| RN-01 | Las medidas del ambiente son interiores; los muros crecen hacia afuera. |
| RN-02 | Todas las puertas empiezan en el piso (alféizar 0). |
| RN-03 | Un mueble ocupa exactamente su caja `ancho × profundidad × alto`, con el frente hacia +Z. |
| RN-04 | Un mueble bloqueado no se mueve ni se elimina. |
| RN-05 | El sol solo existe si hay al menos una ventana; al quitar la última ventana se elimina. |
| RN-06 | Cada objeto 3D se ilumina con un máximo de 3 luces activas (límite de JavaFX). |
| RN-07 | Las paredes con el mismo material y color se suman en una sola línea del presupuesto. |
| RN-08 | Solo se guardan los precios que el usuario cambió; el resto sigue los referenciales. |
| RN-09 | Las escaleras llegan siempre al techo del ambiente. |

## 6. Matriz de trazabilidad (resumen)

| Requisito | Caso de uso | Pruebas automáticas |
|---|---|---|
| RF-01 a RF-04 | CU-02 | `DesignTest`, `DoorsAndStairsTest` |
| RF-05 | CU-02 | `RoomShapeTest` |
| RF-06 a RF-09 | CU-03 | `HomeLayoutTest`, `HomePersistenceTest` |
| RF-10 a RF-13 | CU-04 | `DesignTest`, `RoomShapeTest` |
| RF-14 a RF-20, RF-23 | CU-05, CU-06 | `DesignTest`, `GroupEditingTest`, `FurnitureCustomizationTest`, `FurnitureHandlesTest`, `CatalogTest` |
| RF-21, RF-22 | CU-07, CU-08 | `CustomDesignTest`, `FurnitureShapesTest`, `DesignPersistenceTest` |
| RF-24 a RF-28 | CU-09 | Pruebas manuales (ver doc. 06) |
| RF-29 a RF-33 | CU-10 | `EstimateTest` |
| RF-34 a RF-40 | CU-01, CU-11 | `PersistenceTest`, `HistoryTest`, `RecoveryTest`, `TemplatesTest` |
