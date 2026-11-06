# 13 · Normas de redacción y formato de los documentos

Versión 1.0 · noviembre de 2026

Este documento reúne las normas que se aplicaron a los documentos Word de la carpeta `doc 2` y las
que suelen exigirse en informes de proyectos de software en universidades peruanas. Antes de
entregar, compáralas con la **guía o reglamento de tu escuela profesional y las indicaciones del
docente**: si alguna difiere, manda la de la universidad.

## 1. Normas aplicadas a cada documento

<!-- Tabla: Normas de contenido y formato aplicadas a cada documento -->
| Documento | Norma de contenido | Norma de formato |
|---|---|---|
| 01 Acta de constitución y visión | PMBOK 7.ª ed. (PMI) | APA 7.ª ed. |
| 02 Especificación de requisitos (ERS) | IEEE 830-1998 e ISO/IEC/IEEE 29148:2018; calidad según ISO/IEC 25010:2023 | APA 7.ª ed. |
| 03 Casos de uso | UML 2.5 (OMG) | APA 7.ª ed. |
| 04 Arquitectura de software | ISO/IEC/IEEE 42010:2022, modelo C4 | APA 7.ª ed. |
| 05 Diseño de base de datos | Modelo entidad-relación, formas normales (1FN–3FN) | APA 7.ª ed. |
| 06 Plan y reporte de pruebas | ISO/IEC/IEEE 29119-3 (antes IEEE 829) | APA 7.ª ed. |
| 07 Manual de usuario | ISO/IEC/IEEE 26514 (documentación para usuarios) | APA 7.ª ed. |
| 08 Manual técnico | ISO/IEC/IEEE 26514 y buenas prácticas de documentación de código | APA 7.ª ed. |
| 09 Manual de instalación | ISO/IEC/IEEE 26514 | APA 7.ª ed. |
| 10 Plan del proyecto | PMBOK 7.ª ed., ISO/IEC/IEEE 12207 (ciclo de vida) | APA 7.ª ed. |
| 11 Informe final | Estructura de informe de investigación aplicada; Ley Universitaria N.° 30220 | **APA 7.ª ed. completa** |
| 12 Historial de versiones | *Keep a Changelog* y versionado semántico | APA 7.ª ed. |

## 2. Formato de página (APA 7.ª edición)

<!-- Tabla: Formato de página según APA 7.ª edición -->
| Elemento | Norma | Cómo quedó en los Word |
|---|---|---|
| Tamaño de papel | A4 (en Perú; APA original usa Carta) | A4 |
| Márgenes | 2,54 cm en los cuatro lados | 2,54 cm |
| Fuente | Times New Roman 12, Arial 11, Calibri 11 o Georgia 11 | Times New Roman 12 |
| Interlineado | Doble en todo el texto | Doble en el informe final; 1,5 en documentos técnicos y manuales (lo más común en documentación técnica) |
| Alineación | A la izquierda, sin justificar | A la izquierda |
| Sangría | 1,27 cm en la primera línea de cada párrafo | 1,27 cm |
| Espacio entre párrafos | Ninguno adicional | Ninguno |
| Número de página | Arriba a la derecha, desde la carátula | Al pie, a la derecha, como «Página X de Y» junto al nombre del documento (arriba va el encabezado institucional); la carátula no lleva número |
| Diseño visual | APA no lo regula | Colores de la UNAJ: títulos en azul marino con filete dorado, tablas con encabezado azul y filas alternadas, figuras con marco fino y marco doble en la carátula |
| Encabezado | Encabezado breve (APA 7 para estudiantes no lo exige) | Encabezado institucional de la UNAJ: logo a la derecha, universidad, facultad y escuela, con un filete inferior |
| Carátula | Datos de la institución y del trabajo | Modelo institucional de la UNAJ: logo, facultad, escuela, título, integrantes, docente, asignatura, lugar y año |
| Tablas | Interlineado sencillo o 1,5 permitido | Sencillo, 10 pt |

> Muchas universidades peruanas piden **interlineado 1,5** y **margen izquierdo de 3 cm** para
> anillar o empastar. Si tu guía lo pide, cámbialo en Word: *Diseño de página → Márgenes* y
> *Inicio → Párrafo → Interlineado*.

## 3. Niveles de títulos (APA 7)

<!-- Tabla: Niveles de títulos según APA 7 -->
| Nivel | Formato | Uso en los documentos |
|---|---|---|
| 1 | Centrado, negrita, mayúsculas y minúsculas | Secciones principales (`1. Introducción`) |
| 2 | Alineado a la izquierda, negrita | Subsecciones (`1.1 Propósito`) |
| 3 | Alineado a la izquierda, negrita cursiva | Apartados (`3.1.2 …`) |
| 4 | Con sangría, negrita, termina en punto, el texto sigue en la misma línea | Poco usado |
| 5 | Con sangría, negrita cursiva, termina en punto, el texto sigue en la misma línea | Poco usado |

## 4. Tablas y figuras (APA 7)

- Se numeran en orden de aparición con números arábigos: **Tabla 1**, **Figura 1**.
- Encima de cada una va el número **en negrita** y debajo, en la línea siguiente, el título **en cursiva**.
- Cada documento incluye, después del índice general, un **índice de tablas** y un **índice de figuras**
  con el número, el título y la página de cada una (no se generan si el documento no tiene tablas o figuras).
- En el Markdown, el título de cada tabla o diagrama se escribe en un comentario justo antes del
  bloque: `<!-- Tabla: Requisitos funcionales de los muebles -->` o `<!-- Figura: Modelo entidad-relación -->`.
  El título debe decir **qué contiene** la tabla o figura, no repetir el nombre de la sección. En las
  capturas de pantalla el título es el texto alternativo de la imagen (`![título](ruta)`).
- Las tablas llevan solo líneas horizontales: arriba, debajo del encabezado y al final. Sin líneas verticales.
- Si la tabla o figura no es propia, debajo se escribe una **Nota** con la fuente:
  *Nota.* Adaptado de *Título* (p. 23), por Autor, Año.
- Los diagramas de este proyecto (casos de uso, arquitectura, entidad-relación, cronograma) son
  **elaboración propia**.

## 5. Citas y referencias (APA 7)

### 5.1 Citas en el texto

<!-- Tabla: Formatos de citas en el texto -->
| Tipo | Ejemplo |
|---|---|
| Parentética | Las pruebas automatizadas detectan regresiones (Sommerville, 2016). |
| Narrativa | Ericson (2005) explica el teorema del eje separador. |
| Dos autores | (Pressman y Maxim, 2020) |
| Tres o más autores | (García et al., 2023) |
| Cita textual corta (< 40 palabras) | Entre comillas y con página: (Sommerville, 2016, p. 45). |
| Cita textual larga (≥ 40 palabras) | Bloque aparte con sangría de 1,27 cm, sin comillas, con página. |
| Norma técnica | (International Organization for Standardization [ISO], 2023); luego (ISO, 2023). |

### 5.2 Lista de referencias

- Título **Referencias**, centrado y en negrita, en página nueva.
- Orden alfabético por apellido del primer autor; **sangría francesa** de 1,27 cm; interlineado doble.
- Solo se incluyen las obras citadas en el texto.

<!-- Tabla: Formato de la lista de referencias por tipo de fuente -->
| Tipo de fuente | Formato |
|---|---|
| Libro | Apellido, A. A. (Año). *Título en cursiva* (ed.). Editorial. |
| Tesis | Apellido, A. (Año). *Título* [Tesis de licenciatura, Universidad]. Repositorio. URL |
| Artículo | Apellido, A. (Año). Título. *Revista*, *volumen*(número), páginas. https://doi.org/… |
| Norma | Organización. (Año). *Título* (Código de la norma). |
| Página web | Autor u organización. (Año). *Título*. Sitio. URL |
| Software | Autor. (Año). *Nombre* (Versión x) [Software]. URL |

## 6. Estructura del informe final

Estructura habitual de los informes de proyectos en universidades públicas del Perú (verifica el
orden exacto en la guía de tu escuela):

1. **Carátula:** universidad, facultad, escuela, logo, tipo de trabajo, título, integrantes, docente, asignatura, ciudad y año.
2. **Resumen** (150 a 250 palabras) y **palabras clave** (3 a 6).
3. **Abstract** y **keywords** (traducción al inglés).
4. **Índice** general, de tablas y de figuras.
5. **Introducción.**
6. **Capítulo I. Planteamiento del problema:** realidad problemática, formulación, objetivos, justificación, alcances y limitaciones.
7. **Capítulo II. Marco teórico:** antecedentes (internacionales, nacionales y locales), bases teóricas, términos básicos.
8. **Capítulo III. Metodología.**
9. **Capítulo IV. Desarrollo / Resultados.**
10. **Discusión** (se compara con los antecedentes).
11. **Conclusiones** (una por objetivo).
12. **Recomendaciones.**
13. **Referencias.**
14. **Anexos.**

## 7. Redacción técnica y académica

<!-- Tabla: Reglas de redacción técnica y académica -->
| Regla | Correcto | Evitar |
|---|---|---|
| Persona impersonal o tercera persona | «Se desarrolló una aplicación…» | «Yo hice…», «Hicimos…» (salvo que la guía lo permita) |
| Tiempo verbal | Pasado para metodología y resultados; presente para conclusiones generales | Mezclar tiempos en un mismo párrafo |
| Siglas | Se escriben completas la primera vez: «Especificación de Requisitos de Software (ERS)» | Usar una sigla sin definirla |
| Números | Del cero al nueve en letras, 10 en adelante en cifras; siempre en cifras con unidades | «5 muebles»; «cinco m» |
| Decimales | Con coma, según el Sistema Internacional y la Norma Metrológica Peruana: 2,54 cm | 2.54 cm |
| Miles | Con espacio: 19 400 líneas | 19.400 o 19,400 |
| Unidades | Símbolo del SI con espacio: 2,5 m; 35 m²; S/ 70 | 2,5mts; 35 m2 |
| Términos en inglés | En *cursiva* la primera vez: *software*, *snapshot* | Mezclar sin marcar |
| Claridad | Una idea por párrafo; párrafos de 3 a 7 oraciones | Párrafos de una página |
| Precisión | «La aplicación abrió el proyecto en 1,2 s» | «La aplicación es muy rápida» |

## 8. Integridad académica

- Toda idea ajena se cita, aunque se parafrasee.
- Las universidades peruanas usan programas antiplagio (por ejemplo Turnitin) y fijan un porcentaje
  máximo de similitud; consulta el de tu escuela.
- Los trabajos de grado se registran en el repositorio institucional y en RENATI (SUNEDU), por lo
  que deben cumplir estas normas desde la primera entrega.
- Si se usaron herramientas de inteligencia artificial para apoyar la redacción o el código,
  decláralo según lo que indique tu docente o el reglamento de tu universidad.

## 9. Lista de verificación antes de entregar

- [ ] Reemplazar todos los `[completar]` (resaltados en amarillo en los Word).
- [ ] Actualizar los índices (general, de tablas y de figuras): *Ctrl+E* (seleccionar todo) y luego *F9* → *Actualizar toda la tabla*.
- [ ] Agregar antecedentes y discusión en el informe final.
- [ ] Revisar ortografía (*Revisar → Ortografía y gramática*, idioma Español (Perú)).
- [ ] Revisar que cada tabla y figura tenga número y título y que se mencione en el texto.
- [ ] Revisar que cada cita tenga su referencia y viceversa.
- [ ] Exportar a PDF si el docente lo pide (*Guardar como → PDF*).

## Referencias

American Psychological Association. (2020). *Publication manual of the American Psychological
Association* (7.ª ed.). https://doi.org/10.1037/0000165-000

IEEE. (1998). *IEEE recommended practice for software requirements specifications* (IEEE Std 830-1998).

International Organization for Standardization. (2018). *Systems and software engineering — Life
cycle processes — Requirements engineering* (ISO/IEC/IEEE 29148:2018).

International Organization for Standardization. (2023). *Systems and software engineering —
Systems and software Quality Requirements and Evaluation (SQuaRE) — Product quality model*
(ISO/IEC 25010:2023).

Ley N.° 30220, Ley Universitaria. (2014, 9 de julio). Diario Oficial El Peruano.

Project Management Institute. (2021). *A guide to the project management body of knowledge
(PMBOK guide)* (7.ª ed.).
