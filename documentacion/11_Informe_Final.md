# UNIVERSIDAD NACIONAL DE JULIACA

### Escuela Profesional de `[completar]`

### Curso: `[completar]`

&nbsp;

# INFORME FINAL DEL PROYECTO

## Amoblador3D: aplicación de escritorio para el diseño de interiores en 3D con medidas reales y estimación de presupuesto

&nbsp;

**Docente:** `[completar]`

**Integrantes:**
- `[Apellidos y nombres — código]`
- `[Apellidos y nombres — código]`

**Semestre:** `[completar]`

Juliaca – Perú, noviembre de 2026

---

## Resumen

El presente proyecto tuvo como objetivo desarrollar una aplicación de escritorio que permita diseñar
y amoblar los ambientes de una vivienda en tres dimensiones con medidas reales y estimar el costo de
sus acabados y muebles. Se empleó un modelo de desarrollo iterativo e incremental en ocho fases,
con Java 21, JavaFX para la interfaz y la visualización 3D, SQLite como base de datos embebida y
JUnit 5 para las pruebas automatizadas. El producto, Amoblador3D 2.0.0, permite definir ambientes
rectangulares o en L con columnas y muros internos, componer casas de hasta treinta ambientes en
cuatro pisos, colocar ventanas, seis tipos de puerta, luces y muebles que no pueden superponerse ni
bloquear el paso, elegir entre once materiales de acabado, recorrer el diseño en primera persona y
obtener el metrado de materiales con un presupuesto en soles y un informe PDF. Las reglas de
colisión se resolvieron con el teorema del eje separador sobre rectángulos orientados. Se
ejecutaron 114 pruebas automatizadas con un 100 % de aprobación y una cobertura de instrucciones del
74 % al 92 % en los paquetes de reglas, presupuesto, persistencia y lectura de modelos. Se concluye que es posible ofrecer una
herramienta gratuita, sin conexión a internet y de aprendizaje rápido que ayude a las familias a
tomar decisiones de amoblamiento antes de comprar.

**Palabras clave:** diseño de interiores, visualización 3D, JavaFX, detección de colisiones,
metrado, presupuesto.

## Abstract

This project aimed to develop a desktop application to design and furnish the rooms of a house in
three dimensions using real measurements and to estimate the cost of finishes and furniture. An
iterative and incremental process in eight phases was followed, using Java 21, JavaFX for the user
interface and 3D rendering, SQLite as an embedded database, and JUnit 5 for automated testing. The
resulting product, Amoblador3D 2.0.0, supports rectangular and L-shaped rooms with columns and
interior walls, houses of up to thirty rooms on four floors, windows, six door types, lights and
furniture that cannot overlap or block doorways, eleven finishing materials, a first-person
walkthrough, and a quantity take-off with a budget in Peruvian soles and a PDF report. Collision
rules were solved with the separating axis theorem on oriented rectangles. All 114 automated tests
passed, with 74 % to 92 % instruction coverage in the rules, budgeting, persistence and model-loading packages. It is concluded
that a free, offline and easy-to-learn tool can help families make furnishing decisions before buying.

**Keywords:** interior design, 3D visualization, JavaFX, collision detection, quantity take-off, budgeting.

---

## Índice

1. Introducción
2. Capítulo I: Planteamiento del problema
3. Capítulo II: Marco teórico
4. Capítulo III: Metodología
5. Capítulo IV: Desarrollo de la solución
6. Capítulo V: Resultados
7. Conclusiones
8. Recomendaciones
9. Referencias
10. Anexos

---

## Introducción

Amoblar o remodelar una vivienda implica decisiones costosas que normalmente se toman sin
herramientas: se compra un mueble que luego no entra, se tapa una ventana o se adquiere pintura y
cerámico de más. Las herramientas profesionales de diseño asistido por computadora resuelven el
problema, pero su costo y curva de aprendizaje las alejan del usuario común.

Este informe presenta Amoblador3D, una aplicación de escritorio que combina la visualización 3D con
reglas que reflejan la realidad física de un ambiente y con el cálculo del metrado y el presupuesto
en soles. El capítulo I plantea el problema y los objetivos; el capítulo II, los conceptos en que
se basa la solución; el capítulo III, la metodología; el capítulo IV, el desarrollo; y el capítulo
V, los resultados obtenidos. Finalmente se presentan las conclusiones y recomendaciones.

---

## Capítulo I: Planteamiento del problema

### 1.1 Descripción de la realidad problemática

Las familias y pequeños contratistas de la región deciden la distribución de los muebles y la
compra de acabados a partir de medidas tomadas a mano y de la intuición. Los errores más comunes
son: muebles que no caben o que bloquean puertas y ventanas, colores que no combinan una vez
aplicados, y compras de material mal calculadas. Las aplicaciones disponibles son de pago, requieren
conexión permanente a internet o no consideran precios del mercado local.

### 1.2 Formulación del problema

**Problema general:** ¿Cómo apoyar a los usuarios en el diseño y amoblamiento de los ambientes de su
vivienda con medidas reales y en la estimación del costo de sus acabados?

**Problemas específicos:**
1. ¿Cómo representar ambientes y casas con medidas reales en una aplicación accesible?
2. ¿Cómo evitar ubicaciones imposibles de muebles, puertas y ventanas?
3. ¿Cómo calcular el metrado y el presupuesto de los acabados?
4. ¿Cómo asegurar que los proyectos del usuario no se pierdan?

### 1.3 Objetivos

**Objetivo general:** Desarrollar una aplicación de escritorio que permita diseñar y amoblar
ambientes de una vivienda en 3D con medidas reales y estimar el costo de sus acabados y muebles.

**Objetivos específicos:**
1. Modelar ambientes y casas de varios ambientes y pisos con medidas interiores reales.
2. Implementar reglas automáticas de validación de la ubicación de muebles y aberturas.
3. Visualizar el diseño en 3D, en planta y en recorrido en primera persona.
4. Calcular el metrado de acabados y un presupuesto referencial en soles.
5. Persistir los proyectos de forma confiable y verificar la calidad con pruebas automatizadas.

### 1.4 Justificación

- **Social:** ayuda a familias sin acceso a profesionales de diseño a planificar su vivienda.
- **Económica:** reduce compras equivocadas de muebles y materiales; la herramienta es gratuita.
- **Tecnológica:** demuestra la aplicación de gráficos 3D, geometría computacional y bases de datos
  embebidas en una aplicación de escritorio con software libre.
- **Académica:** integra los contenidos de ingeniería de requisitos, diseño de software, bases de
  datos y pruebas.

### 1.5 Alcance y limitaciones

Ver el [documento 01, sección 4](01_Acta_de_Constitucion_y_Vision.md#4-alcance). Las principales
limitaciones son: paredes solo a 90°, precios referenciales (no en tiempo real) y ausencia de
trabajo colaborativo en línea.

---

## Capítulo II: Marco teórico

### 2.1 Antecedentes

> `[completar con 3 a 5 antecedentes: tesis o artículos, de preferencia peruanos (repositorios
> institucionales de universidades, RENATI de SUNEDU, ALICIA de CONCYTEC), sobre aplicaciones de
> diseño de interiores, visualización 3D o realidad virtual en arquitectura. Para cada uno: autor,
> año, título, objetivo, metodología, resultado y relación con este proyecto.]`

### 2.2 Bases teóricas

**Gráficos 3D por computadora.** Una escena 3D se compone de mallas de triángulos, materiales que
definen cómo reflejan la luz y fuentes de luz. JavaFX ofrece un grafo de escena con primitivas
(cajas, cilindros, esferas), mallas arbitrarias (`TriangleMesh`), materiales de tipo Phong y luces
puntuales, focales y direccionales (OpenJFX, 2024).

**Proyecciones y cámaras.** La vista 3D usa una cámara en perspectiva que orbita alrededor de un
punto; la planta usa una vista cenital; el recorrido ubica la cámara a la altura de los ojos.

**Detección de colisiones con el teorema del eje separador.** Dos polígonos convexos no se
intersecan si existe un eje sobre el cual sus proyecciones no se solapan. Para rectángulos
orientados basta probar los ejes normales a sus lados (Ericson, 2005). Se aplica a la huella en
planta de cada mueble, de las columnas y del área de giro de las puertas.

**Formato glTF 2.0.** Estándar abierto de Khronos Group para transmitir modelos 3D, con una variante
binaria (`.glb`) que empaqueta geometría, materiales y texturas (Khronos Group, 2021).

**Metrado.** Cuantificación de las partidas de obra: áreas de muros descontando vanos, áreas de piso
y longitudes de zócalo. Se usan rendimientos comerciales: un galón de pintura látex cubre cerca de
35 m² por mano, y los pisos se compran con un 10 % de desperdicio por cortes.

**Bases de datos embebidas.** SQLite es un motor de base de datos relacional transaccional (ACID)
que se ejecuta dentro de la aplicación y guarda toda la base en un solo archivo (SQLite, 2024).

**Calidad del software.** La norma ISO/IEC 25010 define características como adecuación funcional,
fiabilidad, usabilidad y mantenibilidad, que se usaron para formular los requisitos no funcionales.

**Pruebas de software.** Las pruebas unitarias verifican componentes aislados y las de integración
la colaboración entre ellos; su automatización permite detectar regresiones (Sommerville, 2016).

### 2.3 Definición de términos básicos

Ver el glosario del [documento 02](02_Especificacion_de_Requisitos_ERS.md#13-definiciones) y del
[manual de usuario](07_Manual_de_Usuario.md#16-glosario).

---

## Capítulo III: Metodología

### 3.1 Tipo de proyecto

Proyecto de desarrollo tecnológico (investigación aplicada) que produce un artefacto de software.

### 3.2 Metodología de desarrollo

Modelo iterativo e incremental con prácticas ágiles: ocho fases, cada una con un incremento
funcional probado. Detalle en el [documento 10](10_Plan_de_Proyecto.md).

### 3.3 Herramientas

| Categoría | Herramienta |
|---|---|
| Lenguaje | Java 21 |
| Interfaz y 3D | JavaFX 21 |
| Base de datos | SQLite 3 (y script PostgreSQL para consultas) |
| Librerías | Gson (JSON), Apache PDFBox (PDF) |
| Construcción | Maven con Maven Wrapper |
| Pruebas y calidad | JUnit 5, JaCoCo, GitHub Actions |
| Control de versiones | Git y GitHub |
| Empaquetado | jpackage (instalador para Windows) |

### 3.4 Técnicas de verificación

Pruebas automatizadas unitarias y de integración en cada cambio, lista de comprobación manual de la
interfaz e integración continua. Detalle en el [documento 06](06_Plan_y_Reporte_de_Pruebas.md).

---

## Capítulo IV: Desarrollo de la solución

### 4.1 Requisitos

Se especificaron 40 requisitos funcionales y 14 no funcionales, agrupados en ambiente y casa,
acabados, muebles, visualización, presupuesto y proyectos ([documento 02](02_Especificacion_de_Requisitos_ERS.md)),
y 11 casos de uso ([documento 03](03_Casos_de_Uso.md)).

### 4.2 Arquitectura

Arquitectura en capas: presentación (JavaFX), dominio sin dependencias gráficas y persistencia. La
decisión clave fue mantener todas las reglas en Java puro, lo que permitió probarlas sin abrir
ventanas ([documento 04](04_Documento_de_Arquitectura.md)).

### 4.3 Base de datos

Once tablas en SQLite (esquema v9) con migraciones automáticas entre versiones
([documento 05](05_Diseno_de_Base_de_Datos.md)).

### 4.4 Funcionalidades implementadas por fase

| Fase | Funcionalidades |
|---|---|
| Base | Migración a Java, escena 3D, acabados, proyectos en SQLite |
| 1 | Metrado, presupuesto, informe PDF, herramienta de medir |
| 2 | Imán, selección múltiple, copiar/pegar, bloqueo |
| 3 | Muebles nuevos, catálogo por categorías, plantillas |
| 4 | Recorrido en primera persona, vistas guardadas, modo noche |
| 5 | Autoguardado con recuperación, instalador para Windows |
| 6 | Columnas, muros internos, habitación en L, techo, zócalo |
| 7 | Casa con varios ambientes, diseñador de muebles |
| 8 | Tipos de puerta, escaleras, pisos, registro de errores, integración continua |

### 4.5 Interfaz

`[insertar capturas de pantalla: pantalla de bienvenida, vista 3D de un dormitorio, planta con
cotas, recorrido, casa completa, diseñador de muebles, presupuesto e informe PDF. Usa el botón 📷
de la aplicación (Ctrl+E) para exportarlas.]`

---

## Capítulo V: Resultados

### 5.1 Cumplimiento de objetivos

| Objetivo específico | Resultado | Logro |
|---|---|---|
| OE1 Modelar ambientes y casas | Ambientes de 1 a 30 m, en L, con columnas; casas de hasta 30 ambientes en 4 pisos | ✅ |
| OE2 Validar ubicaciones | Colisiones con paredes, columnas, muebles y giro de puertas; aberturas sin superposición | ✅ |
| OE3 Visualizar | Vista 3D, planta con cotas, recorrido sin atravesar objetos | ✅ |
| OE4 Metrado y presupuesto | Áreas descontando vanos y columnas; pintura, pisos, zócalo y muebles en S/; CSV y PDF | ✅ |
| OE5 Persistencia y calidad | Transacciones, autoguardado, migraciones v1→v9; 114 pruebas al 100 % | ✅ |

### 5.2 Resultados de las pruebas

- 114 casos automatizados, 100 % aprobados (03/11/2026).
- Cobertura de instrucciones: presupuesto 92 %, modelo, persistencia e informe 89 %, lector glTF 79 %,
  reglas 78 %, catálogo 74 %; total 40 % (la interfaz gráfica se verificó manualmente).
- La construcción exige al menos 70 % de cobertura en los paquetes de dominio y datos.

### 5.3 Métricas del producto

| Métrica | Valor |
|---|---|
| Líneas de código Java (producción + pruebas) | ≈ 19 400 |
| Clases de producción | 85 |
| Clases de prueba | 20 |
| Muebles paramétricos | 18 |
| Materiales de acabado | 11 |
| Tablas de la base de datos | 11 |

### 5.4 Discusión

`[completar: comparar los resultados con los antecedentes del capítulo II — en qué se parece y en
qué supera o no a las soluciones revisadas.]`

---

## Conclusiones

1. Se desarrolló Amoblador3D 2.0.0, una aplicación de escritorio gratuita y sin conexión a internet
   que permite diseñar y amoblar ambientes y casas completas en 3D con medidas reales, cumpliendo el
   objetivo general.
2. El teorema del eje separador sobre las huellas en planta permitió impedir de forma exacta que los
   muebles se superpongan o bloqueen el paso, incluso girados en ángulos de 15°.
3. Separar las reglas del negocio de la interfaz gráfica hizo posible automatizar 114 pruebas que se
   ejecutan en segundos y detectan regresiones en cada cambio.
4. El cálculo de metrado con rendimientos comerciales y precios editables entrega un presupuesto
   útil para planificar compras, aunque referencial.
5. Las transacciones, el autoguardado y las migraciones automáticas protegieron los datos del
   usuario a lo largo de nueve versiones del esquema de la base de datos.

## Recomendaciones

1. Continuar dividiendo la clase `MainWindow` en controladores más pequeños (ya se separó el catálogo).
2. Agregar pruebas automatizadas de la interfaz (TestFX).
3. Permitir paredes en ángulo y curvas en una versión futura.
4. Actualizar periódicamente los precios referenciales o permitir importarlos desde una lista de una tienda.
5. Evaluar la usabilidad con usuarios reales (prueba con 5 a 8 personas) y medir el tiempo para
   amoblar una habitación.

## Referencias

Ericson, C. (2005). *Real-time collision detection*. Morgan Kaufmann.

IEEE. (1998). *IEEE recommended practice for software requirements specifications* (IEEE Std 830-1998).

International Organization for Standardization. (2023). *Systems and software engineering — Systems
and software Quality Requirements and Evaluation (SQuaRE) — Product quality model* (ISO/IEC 25010:2023).

Khronos Group. (2021). *glTF 2.0 specification*. https://registry.khronos.org/glTF/specs/2.0/glTF-2.0.html

OpenJFX. (2024). *JavaFX documentation*. https://openjfx.io

Pressman, R. S., & Maxim, B. R. (2020). *Software engineering: A practitioner's approach* (9.ª ed.). McGraw-Hill.

Sommerville, I. (2016). *Software engineering* (10.ª ed.). Pearson.

SQLite. (2024). *About SQLite*. https://www.sqlite.org/about.html

`[agregar las referencias de los antecedentes]`

## Anexos

- Anexo A: Especificación de requisitos — [02](02_Especificacion_de_Requisitos_ERS.md)
- Anexo B: Casos de uso — [03](03_Casos_de_Uso.md)
- Anexo C: Arquitectura — [04](04_Documento_de_Arquitectura.md)
- Anexo D: Base de datos — [05](05_Diseno_de_Base_de_Datos.md)
- Anexo E: Pruebas — [06](06_Plan_y_Reporte_de_Pruebas.md)
- Anexo F: Manual de usuario — [07](07_Manual_de_Usuario.md)
- Anexo G: Manual técnico — [08](08_Manual_Tecnico.md)
- Anexo H: Manual de instalación — [09](09_Manual_de_Instalacion.md)
- Anexo I: Repositorio del código: https://github.com/alex123floresc-sketch/amoblador3d
