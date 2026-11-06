# 06 · Plan y reporte de pruebas

Basado en ISO/IEC/IEEE 29119-3. Versión 1.0 · noviembre de 2026 · Software 2.0.0

## 1. Plan de pruebas

### 1.1 Objetivo

Verificar que Amoblador3D cumple los requisitos del documento [02](02_Especificacion_de_Requisitos_ERS.md)
y que los cambios nuevos no rompen lo que ya funcionaba (regresión).

### 1.2 Niveles y tipos de prueba

<!-- Tabla: Niveles y tipos de prueba aplicados -->
| Nivel | Qué se prueba | Herramienta | Automatizada |
|---|---|---|---|
| Unitarias | Reglas de `Design`, `Footprint`, `Quantities`, `Snapper`, `History`, formato JSON | JUnit 5 | Sí |
| Integración | Guardar y leer en SQLite real (archivo temporal), migraciones, scripts SQL, generación de PDF | JUnit 5 + `@TempDir` | Sí |
| Geometría 3D | Que cada mueble paramétrico ocupe exactamente sus medidas; asas del mueble | JUnit 5 + nodos JavaFX (sin ventana) | Sí |
| Sistema (interfaz) | Flujos completos con ratón y teclado | Lista de comprobación manual (sección 3) | No |
| Regresión | Todo el conjunto anterior en cada cambio | Maven + GitHub Actions | Sí |

### 1.3 Criterios

- **Entrada:** el código compila con `mvnw compile`.
- **Salida (aprobación):** 100 % de las pruebas automáticas pasan y ningún caso manual crítico falla.
- **Suspensión:** si la compilación falla, no se ejecutan las pruebas.

### 1.4 Entorno

<!-- Tabla: Entorno de ejecución de las pruebas -->
| Elemento | Valor |
|---|---|
| Sistema operativo | Windows 11 Home 10.0.26200 |
| Java | 21 en CI (Temurin); 26 en la máquina de desarrollo |
| Construcción | Maven Wrapper (`mvnw.cmd`) |
| Integración continua | GitHub Actions, `windows-latest` (`.github/workflows/ci.yml`) |
| Datos de prueba | Base SQLite temporal por prueba; nunca se toca la base real del usuario |

### 1.5 Cómo ejecutar

```powershell
.\mvnw.cmd test        # solo pruebas
.\mvnw.cmd verify      # pruebas + informe de cobertura en target\site\jacoco\index.html
```

## 2. Reporte de pruebas automáticas

**Fecha de ejecución:** 03/11/2026 · **Comando:** `mvnw verify` · **Resultado:** ✅ BUILD SUCCESS

<!-- Tabla: Resumen de métricas de las pruebas automáticas -->
| Métrica | Valor |
|---|---|
| Clases de prueba | 20 |
| Casos de prueba | **114** |
| Aprobados | 114 (100 %) |
| Fallidos / con error / omitidos | 0 / 0 / 0 |

### 2.1 Resultados por clase

<!-- Tabla: Resultados de las pruebas por clase -->
| Paquete | Clase de prueba | Casos | Resultado | Requisitos |
|---|---|---|---|---|
| catalog | `CatalogTest` | 2 | ✅ | RF-14 |
| catalog | `CustomDesignTest` | 5 | ✅ | RF-22 |
| design | `DesignTest` | 14 | ✅ | RF-01–04, RF-10–12, RF-15–16 |
| design | `DoorsAndStairsTest` | 5 | ✅ | RF-02, RN-09 |
| design | `FurnitureCustomizationTest` | 5 | ✅ | RF-17, RF-20, RF-22 |
| design | `GroupEditingTest` | 9 | ✅ | RF-18, RF-19, RF-23, RN-04 |
| design | `HistoryTest` | 4 | ✅ | RF-37, RF-40 |
| design | `RoomShapeTest` | 7 | ✅ | RF-05, RF-13, RF-29 |
| design | `TemplatesTest` | 2 | ✅ | RF-39 |
| estimate | `EstimateTest` | 6 | ✅ | RF-29–32 |
| model | `HomeLayoutTest` | 7 | ✅ | RF-06–09 |
| persistence | `DesignPersistenceTest` | 4 | ✅ | RF-22, RF-36 |
| persistence | `HomePersistenceTest` | 7 | ✅ | RF-06, RF-07, RF-34 |
| persistence | `PersistenceTest` | 12 | ✅ | RF-34–36, RNF-07, RNF-10, RNF-13 |
| persistence | `RecoveryTest` | 2 | ✅ | RF-38 |
| scene | `FurnitureHandlesTest` | 4 | ✅ | RF-17 |
| scene | `FurnitureShapesTest` | 3 | ✅ | RF-22 |
| scene | `ParametricBuilderTest` | 1 | ✅ | RN-03 |
| gltf | `GltfLoaderTest` | 13 | ✅ | RF-21 |
| util | `AppLogTest` | 2 | ✅ | RNF-08 |

### 2.2 Casos de prueba destacados

<!-- Tabla: Casos de prueba destacados -->
| Id | Caso (método) | Entrada | Resultado esperado |
|---|---|---|---|
| CP-01 | `losMueblesNuevosNoChocanEntreSiNiConLaPuerta` | Agregar varios muebles en un dormitorio de 4 × 3,5 m | Ninguna huella se solapa con otra ni con el giro de la puerta |
| CP-02 | `siDerechoNoCabeSePruebaGirado` | Ambiente angosto y un mueble largo | Se coloca girado 90° |
| CP-03 | `siNoCabeElMuebleSeExplicaPorQue` | Ambiente lleno | Se rechaza con un mensaje que dice el motivo |
| CP-04 | `unaPuertaNoPuedeQuedarBloqueadaPorUnMueble` | Mover un mueble delante de una puerta | Se rechaza el movimiento |
| CP-05 | `alAchicarLaHabitacionLasAberturasQueNoCabenSeEliminanConAviso` | Reducir el ancho del ambiente | Se eliminan las aberturas que no caben y se lista cuáles |
| CP-06 | `metradoDescuentaVentanasYPuertas` | Ambiente con 1 ventana y 1 puerta | Área de pared = perímetro × alto − vanos |
| CP-07 | `laPinturaSeCompraPorCuartosDeGalon` | Ejemplo: un área que necesita 2,1 galones | Se compran 2,25 galones (cuarto de galón hacia arriba) |
| CP-08 | `losPisosLlevanDiezPorCientoDeDesperdicio` | Ejemplo: 14 m² de cerámico | Se compran 15,4 m² |
| CP-09 | `unaBaseDeLaVersionAnteriorSeActualiza` | Base con esquema antiguo | Se migra a v9 sin perder proyectos |
| CP-10 | `superficiesDanadasSeCorrigenSinPerderElProyecto` | `.amob3d` con material inexistente | Se abre con el acabado por defecto y aviso |
| CP-11 | `deshacerRehacerYCambiosSinGuardar` | Cambiar, deshacer, rehacer | Se recupera cada estado y la marca "sin guardar" es correcta |
| CP-12 | `unaPuertaPegadaAOtroAmbienteLeAbrePaso` | Dos ambientes vecinos, una puerta en el muro común | El muro del vecino se abre en ese tramo |
| CP-13 | `losPisosSeApilanYNoChocanEntreSi` | Ambientes en piso 0 y piso 1 en el mismo lugar | Se permite (distinto piso) |
| CP-14 | `todosLosMueblesCabenEnSusMedidas` | Cada uno de los 18 paramétricos | Su geometría ocupa exactamente ancho × profundidad × alto |
| CP-16 | `losModelosComprimidosConDracoSeRechazanConUnMensajeClaro` | glTF con `KHR_draco_mesh_compression` | Error que menciona Draco |
| CP-17 | `seAplicaLaTransformacionDeLosNodos` | Nodo padre trasladado y nodo hijo escalado ×2 | Los vértices quedan movidos y escalados |
| CP-15 | `seGuardanLosMensajesYLasTrazasEnElArchivo` | Un aviso y un error con excepción | El registro contiene ambos y la traza |

### 2.3 Cobertura de código (JaCoCo)

Datos de `target/site/jacoco/jacoco.csv` del 03/11/2026.

<!-- Tabla: Cobertura de código por paquete según JaCoCo -->
| Paquete | Instrucciones | Ramas | Líneas | Comentario |
|---|---|---|---|---|
| `estimate` | 92 % | 84 % | 93 % | Lógica de presupuesto |
| `model` | 89 % | 75 % | 94 % | |
| `persistence` | 89 % | 72 % | 91 % | Incluye base real y migraciones |
| `report` | 89 % | 83 % | 91 % | Se genera un PDF real en la prueba |
| `util` | 80 % | 55 % | 77 % | |
| `gltf` | 79 % | 58 % | 81 % | Modelos `.gltf` y `.glb` armados en memoria |
| `design` | 78 % | 63 % | 79 % | Núcleo de reglas |
| `catalog` | 74 % | 52 % | 69 % | |
| `scene` | 25 % | 16 % | 23 % | Solo geometría; el dibujo 3D se prueba a mano |
| `ui` | 0 % | 0 % | 0 % | Interfaz probada manualmente |
| **Total** | **40 %** | | | La interfaz gráfica es más de la mitad del código |

**Umbral obligatorio:** `mvnw verify` falla si alguno de los paquetes `model`, `design`, `estimate`,
`catalog`, `persistence`, `report`, `gltf` o `util` baja del **70 %** de instrucciones cubiertas
(regla `cobertura-minima` de JaCoCo en `pom.xml`). Así una función nueva sin pruebas no pasa la
integración continua.

## 3. Pruebas manuales de sistema (lista de comprobación)

Ejecutar antes de cada entrega con `.\mvnw.cmd javafx:run` y marcar el resultado.

<!-- Tabla: Lista de comprobación de pruebas manuales de sistema -->
| Id | Paso | Resultado esperado | Resultado |
|---|---|---|---|
| PM-01 | Abrir la aplicación por primera vez | Aparece la pantalla de bienvenida | `[ ]` |
| PM-02 | Elegir *Casa de ejemplo* | Se abre una casa con sala, dormitorio, cocina y baño | `[ ]` |
| PM-03 | Arrastrar un mueble contra la pared | Se pega a la pared (imán) y muestra guías | `[ ]` |
| PM-04 | Arrastrar un mueble encima de otro | No se encima; se desliza o se queda | `[ ]` |
| PM-05 | Ctrl+3 y caminar con W/A/S/D hacia una pared | No se atraviesa | `[ ]` |
| PM-06 | Tecla N | Modo noche: solo se ven las lámparas | `[ ]` |
| PM-07 | Ctrl+P, doble clic en un precio y cambiarlo | El total se recalcula; al reabrir sigue el precio nuevo | `[ ]` |
| PM-08 | Ctrl+Shift+P | Se genera el PDF y se ofrece abrirlo | `[ ]` |
| PM-09 | Guardar, cerrar, abrir el proyecto | Todo igual, con miniatura | `[ ]` |
| PM-10 | Cerrar la app con el Administrador de tareas con cambios sin guardar; volver a abrir pasado 1 min | Ofrece recuperar el trabajo | `[ ]` |
| PM-11 | Importar un `.glb` | Aparece en el catálogo con sus medidas | `[ ]` |
| PM-12 | Ctrl+Shift+D, crear un mueble con 3 piezas y guardarlo | Aparece en *Mis diseños* | `[ ]` |
| PM-13 | Ctrl+4, arrastrar un ambiente sobre otro | No se enciman | `[ ]` |
| PM-14 | *Ayuda → Abrir registro de errores* | Se abre la carpeta `logs` con `amoblador3d-0.log` | `[ ]` |
| PM-15 | Instalar con `packaging\crear-instalador.cmd` en una PC sin Java | La aplicación abre | `[ ]` |

## 4. Defectos encontrados y corregidos (resumen histórico)

<!-- Tabla: Defectos encontrados y corregidos por fase -->
| Fase | Defecto | Corrección |
|---|---|---|
| Migración a Java | Un error al guardar podía dejar el proyecto a medio escribir | Guardado en transacción (`Database.transaction`) |
| Migración a Java | Un mueble o luz con datos incompletos detenía la carga a la mitad | `ProjectValidator` revisa antes de mostrar |
| Migración a Java | El panel permitía valores (alféizar 0 m) que luego se rechazaban | `Editors` usa los mismos límites que `Design` |
| 2.0.0 | Los errores inesperados no quedaban registrados en ningún lado | Registro de errores `AppLog` y aviso al usuario |

## 5. Mejoras propuestas a las pruebas

1. ~~Probar `GltfLoader`~~ — hecho: `GltfLoaderTest` (13 casos, 79 % de cobertura).
2. Pruebas de interfaz con TestFX + Monocle (sin pantalla) para los flujos PM-01 a PM-09.
3. ~~Fijar un umbral mínimo de cobertura~~ — hecho: 70 % en los paquetes de dominio y datos.
4. Subir la cobertura de ramas de `catalog` y `util` (52 % y 55 %).
