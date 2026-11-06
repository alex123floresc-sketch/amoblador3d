# 10 · Plan del proyecto, cronograma y riesgos

Versión 1.1 · noviembre de 2026

## 1. Metodología

Se usó un modelo **iterativo e incremental** con prácticas ágiles (inspirado en Scrum):

- Cada **fase** es un incremento que se puede usar: agrega funciones completas, con sus pruebas, su
  migración de base de datos y su documentación en el README.
- Una rama Git por fase (`fase-N-…`); se integra cuando todas las pruebas pasan.
- Al empezar cada fase se priorizan las funciones del documento de requisitos según el valor para
  el usuario (prioridad A antes que M y B).
- Al terminar cada fase: revisión del funcionamiento, corrección de defectos y actualización de los scripts SQL.

**Ciclo de vida según ISO/IEC 12207:** requisitos → diseño de la arquitectura → construcción → pruebas
→ integración → entrega, repetido en cada fase.

## 2. Estructura de desglose del trabajo (EDT)

<!-- Figura: Estructura de desglose del trabajo (EDT) -->
```mermaid
flowchart LR
    P[Amoblador3D]
    P --> G[1 Gestión]
    P --> R[2 Requisitos]
    P --> D[3 Diseño]
    P --> C[4 Construcción]
    P --> T[5 Pruebas]
    P --> E[6 Entrega]
    G --> G1[1.1 Plan y cronograma] & G2[1.2 Riesgos]
    R --> R1[2.1 Visión] & R2[2.2 ERS] & R3[2.3 Casos de uso]
    D --> D1[3.1 Arquitectura] & D2[3.2 Base de datos] & D3[3.3 Interfaz]
    C --> C1[4.1 Ambiente y acabados] & C2[4.2 Muebles y catálogo] & C3[4.3 Vista 3D y recorrido] & C4[4.4 Presupuesto y PDF] & C5[4.5 Casa multiambiente] & C6[4.6 Persistencia]
    T --> T1[5.1 Pruebas automáticas] & T2[5.2 Pruebas manuales] & T3[5.3 Integración continua]
    E --> E1[6.1 Instalador] & E2[6.2 Manuales] & E3[6.3 Informe final]
```

## 3. Cronograma (real, según el historial de Git)

<!-- Tabla: Cronograma real de fases y entregables -->
| Fase | Entregable | Inicio | Fin | Commit |
|---|---|---|---|---|
| 0 | Prototipo Electron + Three.js | — | 2026-10-21 | `d6d7335` |
| 1.x | Migración a Java + JavaFX + SQLite, correcciones, scripts SQL | 2026-10-21 | 2026-10-25 | `ff7373c`…`65f8e6e` |
| 1 | Metrado, presupuesto, PDF, medir | 2026-10-25 | 2026-10-25 | `8ef40e5` |
| 2 | Imán, selección múltiple, copiar/pegar, bloqueo | 2026-10-25 | 2026-10-25 | `a690de5` |
| 3 | Muebles nuevos, catálogo por categorías, plantillas | 2026-10-25 | 2026-10-25 | `650b5cc` |
| 4 | Recorrido, vistas, modo noche | 2026-10-25 | 2026-10-25 | `e99db7b` |
| 5 | Autoguardado e instalador | 2026-10-25 | 2026-10-25 | `2df95ef` |
| 6 | Columnas, muros, L, techo, zócalo | 2026-10-25 | 2026-10-25 | `3c42b81` |
| 7 | Casa multiambiente, diseñador de muebles | 2026-10-26 | 2026-10-27 | `fe6f6be` |
| 8 | Tipos de puerta, escaleras, pisos, registro de errores, CI, documentación | 2026-10-28 | 2026-11-04 | `6d277dc`…`56587b4` |
| 8.1 | Carátula institucional, encabezados e índices de tablas y figuras en los Word | 2026-11-06 | 2026-11-06 | `77cf676` |

<!-- Figura: Diagrama de Gantt del proyecto -->
```mermaid
gantt
    dateFormat YYYY-MM-DD
    title Cronograma de Amoblador3D
    section Base
    Prototipo Electron           :done, 2026-10-15, 2026-10-21
    Migración a Java             :done, 2026-10-21, 2026-10-25
    section Funciones
    Fases 1 a 6                  :done, 2026-10-25, 1d
    Fase 7 Casa y diseñador      :done, 2026-10-26, 2026-10-27
    Fase 8 Puertas, pisos, calidad :done, 2026-10-28, 2026-11-04
    section Cierre
    Documentación                :done, 2026-11-03, 2026-11-06
    Sustentación                 :milestone, 2026-11-10, 0d
```

> El inicio del prototipo (15/10/2026) y la sustentación (10/11/2026) son las fechas planificadas
> por el equipo; las demás salen del historial de Git.

## 4. Roles

<!-- Tabla: Roles y responsabilidades del equipo -->
| Rol | Responsabilidad | Integrante |
|---|---|---|
| Jefe de proyecto | Planificación, seguimiento, coordinación con el docente | Flores Calcina, Alexander Edson |
| Analista | Requisitos y casos de uso | Sandia Condori, George James |
| Arquitecto / desarrollador | Diseño, código, base de datos | Flores Calcina, Alexander Edson (con apoyo de todo el equipo) |
| Responsable de calidad | Pruebas y revisión | Huanca Tiña, Erick Reison |
| Documentador | Manuales e informe | Flores Ccori, Brayan |

## 5. Gestión de riesgos

Probabilidad e impacto: 1 (bajo) a 3 (alto). Exposición = P × I.

<!-- Tabla: Matriz de riesgos del proyecto -->
| Id | Riesgo | P | I | Exp. | Respuesta | Estado |
|---|---|---|---|---|---|---|
| R1 | Rendimiento 3D bajo en PCs de laboratorio | 2 | 3 | 6 | Reconstruir solo lo que cambia; materiales compartidos; texturas con caché | Mitigado |
| R2 | Pérdida de trabajo por cortes de luz | 3 | 3 | 9 | Autoguardado cada 60 s y guardado en transacción | Mitigado |
| R3 | Cambios de base de datos que rompen proyectos antiguos | 2 | 3 | 6 | Migraciones numeradas con prueba automática | Mitigado |
| R4 | El usuario no tiene Java instalado | 3 | 2 | 6 | Instalador con Java incluido | Mitigado |
| R5 | Modelos 3D de internet con escala u orientación incorrecta | 3 | 1 | 3 | Diálogo de importación que corrige medidas y giro | Mitigado |
| R6 | Clases muy grandes difíciles de mantener (`MainWindow`) | 3 | 2 | 6 | Documentar la deuda técnica; refactorizar por controladores | Abierto |
| R7 | Errores en producción sin información para diagnosticarlos | 2 | 2 | 4 | Registro de errores con trazas | Mitigado (2.0.0) |
| R8 | Retraso en la entrega por ampliar el alcance | 2 | 3 | 6 | Fases cortas, cada una entregable; alcance cerrado en el doc. 01 | Controlado |
| R9 | Licencias de modelos 3D de terceros | 1 | 2 | 2 | Muebles paramétricos propios; texturas generadas por código | Mitigado |

## 6. Estimación de costos (referencial)

<!-- Tabla: Estimación referencial de costos -->
| Concepto | Cantidad | Costo unitario (S/) | Total (S/) |
|---|---|---|---|
| Horas de trabajo: jefe de proyecto y arquitecto (Flores Calcina, Alexander Edson) | 80 h | 15,00 | 1 200,00 |
| Horas de trabajo: analista (Sandia Condori, George James) | 55 h | 15,00 | 825,00 |
| Horas de trabajo: responsable de calidad (Huanca Tiña, Erick Reison) | 55 h | 15,00 | 825,00 |
| Horas de trabajo: documentador (Flores Ccori, Brayan) | 50 h | 15,00 | 750,00 |
| Software (JDK, JavaFX, SQLite, Maven, IntelliJ Community, Git) | — | 0 (libre) | 0 |
| Hosting del repositorio y CI (GitHub) | — | 0 (plan gratuito) | 0 |
| Computadora de desarrollo (ya disponible) | 1 | — | — |
| **Total** | **240 h** | | **3 600,00** |

Todo el software usado es libre o gratuito, por lo que el costo del producto es solo el de las horas de trabajo.
Las horas se estimaron para unas tres semanas de trabajo (del 15/10 al 06/11/2026), y la tarifa de
S/ 15,00 por hora es referencial, la de un practicante de ingeniería de sistemas.

## Historial del documento

<!-- Tabla: Historial de cambios de este documento -->
| Versión | Fecha | Cambios | Responsable |
|---|---|---|---|
| 1.0 | 2026-11-03 | Versión inicial | Equipo del proyecto |
| 1.1 | 2026-11-06 | Cronograma con los commits reales de la fase 8 y la entrega de documentación; Gantt actualizado | Equipo del proyecto |
