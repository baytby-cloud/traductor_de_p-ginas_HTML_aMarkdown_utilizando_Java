# HTML2Markdown

**Asignatura:** Desarrollo de Sistemas III
**Alumno:** TU NOMBRE
**Lenguaje:** Java 21 (compatible con Java 17+)
**Restricción:** solo clases de la API estándar de Java

## Descripción

HTML2Markdown es una aplicación de consola que recibe la URL de una página web,
descarga su HTML mediante una conexión HTTP, interpreta los elementos principales
del documento y genera un archivo Markdown local codificado en UTF-8.

El programa se divide en cinco clases con responsabilidades separadas:

| Clase | Responsabilidad |
|---|---|
| `HTML2Markdown` | Valida argumentos, coordina el flujo y muestra el resumen |
| `WebClient` | Conexión HTTP con `URL`, `URLConnection` y `HttpURLConnection` |
| `HtmlParser` | Analiza el HTML con `ParserDelegator` y genera una lista de tokens |
| `MarkdownConverter` | Aplica las reglas de conversión de HTML a Markdown |
| `MarkdownWriter` | Guarda el archivo con `java.nio.file` en UTF-8 |

Flujo: `URL → WebClient → HTML → HtmlParser → tokens → MarkdownConverter → Markdown → MarkdownWriter → archivo .md`

## Estructura del proyecto

```
HTML2Markdown/
├── README.md
├── .gitignore
├── src/main/java/        (las 5 clases)

├── tests/                (HTML de prueba)
├── ejemplos/             (salidas Markdown representativas)
├── evidencias/           (capturas de las pruebas)
└── output/               (salidas generadas, excluida de Git)
```

## Compilación y ejecución

Desde la carpeta raíz del proyecto:

```
javac -d out src/main/java/*.java
java -cp out HTML2Markdown <url> [archivo_salida.md]
```

Ejemplos:

```
java -cp out HTML2Markdown https://ejemplo.org/articulo.html
java -cp out HTML2Markdown https://ejemplo.org/articulo.html salida.md
```

Si no se indica archivo de salida, se genera uno a partir del nombre de la página
(`articulo.html` produce `articulo.md`).

En IntelliJ IDEA: configurar la URL en **Run → Edit Configurations → Program arguments**.

## Pruebas con servidor local

Para probar con los archivos de `tests/` se sirve la carpeta con Python:

```

python -m http.server 8000 --directory tests
java -cp out HTML2Markdown http://localhost:8000/listas.html output/listas.md
```

## Elementos soportados

| HTML | Markdown |
|---|---|
| `h1` a `h6` | `#` a `######` |
| `p` | Párrafo separado por línea en blanco |
| `strong`, `b` | `**texto**` |
| `em`, `i` | `*texto*` |
| `a href` | `[texto](URL)` |
| `img` | `![alt](URL)` |
| `ul`, `ol`, `li` | Listas con `-` y numeradas, con anidación |
| `br` | Salto de línea |
| `blockquote` | `> texto` |
| `code` | Código en línea con acentos graves |
| `pre` | Bloque de código con tres acentos graves |
| `hr` | `---` |
| `script`, `style` | Contenido omitido |

Las URL relativas de enlaces e imágenes se convierten en absolutas usando la URL base.

## Pruebas realizadas

| Prueba | Condición | Resultado | Evidencia |
|---|---|---|---|
| P01 | Sin argumentos | Mensaje de uso | evidencias/p01.png |
| P02 | URL inválida | Error controlado | evidencias/p02.png |
| P03 | Página inexistente | Error HTTP 404 | evidencias/p03.png |
| P04 | Encabezados h1 a h6 | Correcto | evidencias/p04.png |

| P05 | Negritas y cursivas | Correcto | evidencias/p05.png |
| P06 | Enlaces absolutos y relativos | Correcto | evidencias/p06.png |
| P07 | Imágenes con alt | Correcto | evidencias/p07.png |
| P08 | Listas ordenadas y no ordenadas | Correcto | evidencias/p08.png |
| P09 | Elementos anidados | Correcto | evidencias/p09.png |
| P10 | `script` y `style` | Contenido excluido | evidencias/p10.png |
| P11 | Acentos y entidades | Correcto | evidencias/p11.png |
| P12 | Página vacía | Archivo vacío | evidencias/p12.png |

## Limitaciones

`ParserDelegator` es un analizador heredado basado en HTML 3.2 y puede interpretar mal HTML5 moderno.

No se ejecuta JavaScript, por lo que el contenido generado dinámicamente no se captura.

No se descargan imágenes ni recursos: solo se convierten sus referencias.

No se admiten tablas, formularios ni elementos `iframe`.

`HttpURLConnection` no sigue redirecciones entre `http` y `https`.

Existe un límite de 5 MB por documento y tiempos máximos de conexión y lectura.

Para URL arbitrarias haría falta controlar redirecciones y destinos internos (riesgo de SSRF).

## Preguntas de reflexión

### 1. ¿Qué papel desempeña la aplicación dentro de una arquitectura cliente-servidor?

Actúa como **cliente**. Envía una petición HTTP GET a un servidor web, recibe la
respuesta y procesa localmente el contenido. No sirve contenido a nadie: solo
consume un recurso remoto y lo transforma.

### 2. ¿Qué responsabilidades corresponden al servidor y cuáles al cliente?

El **servidor** almacena o genera el documento, atiende la petición y responde con
un código de estado (200, 404...), cabeceras (como `Content-Type`) y el cuerpo.
El **cliente** (esta aplicación) construye la petición, define tiempos de espera,

valida el código y el tipo de contenido, detecta la codificación, lee el cuerpo con
un límite de tamaño, interpreta el HTML, lo convierte y guarda el resultado.

### 3. ¿Qué diferencia existe entre recuperar HTML e interpretar su estructura?

Recuperar es una tarea de red y de entrada/salida: se obtiene una cadena de
caracteres. Interpretar es entender qué significan esos caracteres: dónde empieza y
termina cada etiqueta, qué atributos tiene y cómo se relacionan los elementos. Con
`WebClient` solo se tiene texto; con `HtmlParser` ese texto se convierte en una
secuencia de eventos con significado.

### 4. ¿Por qué no basta reemplazar etiquetas HTML con expresiones regulares?

Porque HTML tiene estructura anidada y las expresiones regulares no pueden llevar
la cuenta de niveles de anidación arbitrarios. Además hay etiquetas sin cierre
(`<br>`, `<li>` en HTML antiguo), atributos con comillas simples, dobles o sin
comillas, comentarios, entidades (`&amp;`) y scripts que contienen `<` en su texto.
Un reemplazo simple de `<strong>` por `**` no sabe si la etiqueta está dentro de un
enlace, de una lista o de un bloque de código. Por eso se usa un analizador por
eventos y las expresiones regulares solo se usan en tareas puntuales, como
detectar el `charset` en la cabecera.

### 5. ¿Qué dificultades presentan los elementos anidados?

Hay que recordar qué elementos están abiertos, por eso el conversor usa pilas y contadores.

Los delimitadores deben cerrarse en el orden correcto (`**[texto](url)**`).

Las listas anidadas necesitan sangría según su profundidad y un contador por cada `ol`.

Un `li` puede contener otra lista, párrafos o enlaces.

Dentro de `pre` el texto no debe escaparse ni modificarse.

Dentro de `script` y `style` hay que ignorar todo hasta su cierre.

### 6. ¿Qué información puede perderse al convertir HTML a Markdown?


Se pierden los estilos CSS (colores, tamaños, fuentes), el diseño y la maquetación,
los atributos `class` e `id`, el comportamiento de JavaScript, formularios, botones,
iframes, audio y video, y elementos semánticos HTML5 que Markdown no representa. También
se pierde el espaciado exacto y las tablas si no se implementa su conversión.
Markdown solo conserva la estructura semántica básica del contenido.

### 7. ¿Por qué deben resolverse las URL relativas?

Un enlace como `/learn/` o `img/logo.png` solo tiene sentido respecto a la página
donde estaba. Al guardar el Markdown en otro lugar (el equipo local, GitHub), esas
referencias quedarían rotas. Se resuelven contra la URL base con `URI.resolve`, que
produce una URL absoluta válida, por ejemplo `https://dev.java/learn/`.

### 8. ¿Qué ventajas ofrece separar descarga, análisis y conversión en clases?

Cada clase tiene una única responsabilidad, lo que facilita entender y depurar el código.

Se pueden probar por separado: el conversor se prueba sin red.

Se puede reemplazar una pieza sin tocar las demás (por ejemplo, otro analizador HTML).

Se reutilizan los componentes en otros proyectos. 

Los errores quedan localizados (de red, de análisis o de escritura).

### 9. ¿Qué limitaciones presenta ParserDelegator con HTML moderno?

Está basado en HTML 3.2. No conoce las etiquetas de HTML5 como `section`, `article`,
`nav`, `header`, `footer` o `figure`, y las trata como desconocidas. Puede interpretar
mal documentos mal formados, no construye un árbol DOM (solo entrega eventos) y
entrega el contenido de `script` y `style` como texto, por lo que el conversor debe
excluirlo. Tampoco ejecuta JavaScript. En las pruebas con example.com observé que
el contenido de `<style>` y `<script>` llegaba como texto y que el analizador entregaba
`<head>`, `<meta>` y `<link>` como etiquetas que había que ignorar.
*(Agrega aquí cualquier otro comportamiento que hayas visto.)*


### 10. ¿Qué modificaciones serían necesarias para convertir tablas HTML a Markdown?

Habría que manejar table, `tr`, `th` y `td` en `MarkdownConverter`:

Guardar el estado de la tabla actual y acumular cada fila como una lista de celdas.

Al cerrar cada `td` o `th`, guardar el texto de la celda (sin saltos de línea).

Al cerrar la fila, escribirla como `| celda1 | celda2 |`.

Después de la primera fila (el encabezado), agregar la línea separadora `|---|---|`.

Escapar el carácter `|` dentro del contenido de las celdas.

Decidir qué hacer con `colspan` y `rowspan`, que Markdown no soporta, y con tablas sin encabezado (generar uno vacío).

Agregar pruebas con tablas simples y tablas con contenido en línea.