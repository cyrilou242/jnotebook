// # 📖 Book of jNotebook
// *Disclaimer: this documentation is directly copying some content from the [book of Clerk](https://book.clerk.vision/#rationale), a notebook system for Clojure.*
// ## ⚖️ Rationale
// Computational notebooks allow arguing from evidence by mixing prose with executable code. For a good overview of problems users encounter in traditional notebooks like Jupyter, see [I don't like notebooks](https://www.youtube.com/watch?v=7jiPeIFXb6U) and [What’s Wrong with Computational Notebooks? Pain Points, Needs, and Design Opportunities](https://austinhenley.com/pubs/Chattopadhyay2020CHI_NotebookPainpoints.pdf).
//
// Specifically jNotebook wants to address the following problems:
//
// * notebook editors are less helpful than IDE editors
// * notebook code is hard to reuse
// * out-of-order execution causes reproducibility issues
// * notebook code is hard to version control
// * the Java ecosystem does not provide a great experience for visualization and document formatting
//
// jNotebook is a notebook library for Java that aims to address these problems by doing less, namely:
// * no editing environment: you can keep the code editor you know and love
// * (almost) no new format: jNotebook interprets [JShell files](https://docs.oracle.com/en/java/javase/20/JShell/scripts.html#GUID-C3A41878-9A9A-4D31-BBDF-909729848A3E) and renders them as notebook.
//   Because jNotebook is not required to run JShell files, you can create JShell scripts interactively with jNotebook, you won't depend on jNotebook in production later.
// * no out-of-order execution: jNotebook always evaluates from top to bottom. jNotebook builds a dependency graph of Java statements and only recomputes the needed changes to keep the feedback loop fast.
// * cells outputs are interpreted as html. This gives access to great visualization libraries and standard html for formatting.

// ## 🚀 Getting Started

// ### Quickstart

// `jnotebook` requires Java 17 or higher.
// `jnotebook` is distributed in a single portable binary. Download it.
// ```
// curl -Ls https://get.jnotebook.catheu.tech -o jnotebook
// chmod +x jnotebook
// ```
//
// Then launch.
// ```
// ./jnotebook server
// ```

// ### Install
// See detailed installation instruction for different platforms in the [github project](https://github.com/cyrilou242/jnotebook/#install).

// ### 🤹 jNotebook Demo
// TODO CYRIL

// ### 🔌 In an Existing Project
// TODO CYRIL

// ### Editor integration
// Make sure your editor is configured to work with JShell files.
// TODO CYRIL

// ## 💡 Editor principle
// Cells are delimited by blank lines

String s1 = "Hello";

String s2 = "World";

// for multi-statements cells, only the last value or method result is returned.

String s3 = "!";
String s4 = "!";
String message = s1 + " " + s2 + s3 + s4;

// but everything sent to System.out is returned.

System.out.println("Hello John");
System.out.println("Hello Jane");
String s4 = "Hello Alex";

// html in return values is interpreted. (see [custom html](#custom-html) for more html manipulation)
String sayHello() {
  return "<b>Hello John!</b>";
}
sayHello();

// while System.out is not interpreted as html.
System.out.println("<b>Hello Jane!</b>");

// By default, the java environment imports common classes. The exact list can be found [here](https://github.com/cyrilou242/jnotebook/blob/360919e15414509af3ae1a7b9c246dcfe6c3421e/jnotebook-core/src/main/java/tech/catheu/jnotebook/JShell/PowerJShell.java#L28).
List.of(1,2,3);
Map.of("key", "value");
Thread.sleep(1);

// Mistakes happen! jNotebook tries its best to give helpful error messages.
invalidJava();
//
// Exceptions happen too!
throw new RuntimeException("Panic!");

// ### Markdown
// Latex is supported inline: $`a^2+b^2=c^2`$
// using ```$`a^2+b^2=c^2`$```.
// and on a separate line:
// ```math
// a^2+b^2=c^2
// ```
// using
//<code>\```math<br>a^2+b^2=c^2<br>```</code>

// ### Mermaid
// Mermaid graphs are supported
// ```mermaid
// sequenceDiagram
//     Alice->>+John: Hello John, how are you?
//     Alice->>+John: John, can you hear me?
//     John-->>-Alice: Hi Alice, I can hear you!
//     John-->>-Alice: I feel great!
// ```
// using
//<code>\```mermaid<br>[MERMAID GRAPH CODE]<br>```</code>
// See [mermaid documentation](https://mermaid.js.org/intro/) for examples.

// ## 🔍 Viewers

// jNotebook provides viewers and utils for data, tables, plots, flamegraphs etc.
// These utils are packaged in a separate dependency `jnotebook-utils`. By default, `jnotebook-utils` is in the classpath.
// All utils are available as static method in `tech.catheu.jnotebook.Nb`.

import tech.catheu.jnotebook.Nb;

// ## 🔢 Tables
// *coming soon*

// ## 📊 Plotly
// jNotebook has built-in support for Plotly's low-ceremony plotting. See Plotly's JavaScript [docs](https://plotly.com/javascript/) for more examples and [options](https://plotly.com/javascript/configuration-options/).
Nb.plotly(List.of(
          Map.of("z", List.of(List.of(1, 2, 3), List.of(3, 2, 1)), "type", "surface")),
          Map.of(),
          Map.of());

// ## 🗺 Vega Lite
// jNotebook also supports [Vega Lite](https://vega.github.io/vega-lite/).
Nb.vega(Map.of(
            "data", Map.of("url", "https://vega.github.io/vega-lite/data/seattle-weather.csv"),
            "mark", "bar",
            "encoding", Map.of(
                    "x", Map.of("timeUnit", "month", "field", "date", "type", "ordinal"),
            "y", Map.of("aggregate", "mean", "field", "precipitation")
          )
        ));


// ## 🏞 Images
// *coming soon*

// ## 🔠 Grid Layouts
// Layouts can be composed via rows, columns and grids
Nb.row(1, 2, 3, 4);

Nb.col(1, 2, 3, 4);

Nb.col(Nb.row("John", "Jane", "Alex"), Nb.row(1, 2, 3), Nb.row(4, 5, 6));

int numCols = 4;
Nb.grid(numCols, 1, 2, 3, 4, 5, 6, 7);

// ## ⚙️ Custom html
// You can use the [j2html](https://j2html.com/) library directly to generate html easily.
// Values inheriting `j2html.tags.DomContent` are rendered as html.
import static j2html.TagCreator.*; // import all html tags
b("Hello red!").withStyle("color: red");

// This makes it easy to create custom viewers
import j2html.tags.DomContent;
DomContent title(String text) {
    return p(text).withStyle("font-weight:bold; font-size: x-large; display: block;margin-left: auto; margin-right: auto");
};
title("A big title.");

// All `Nb` viewers output are of class `j2html.tags.DomContent`. This makes is easy to combine viewers
var graph1 = Nb.vega(Map.of(
                         "data", Map.of("url", "https://vega.github.io/vega-lite/data/seattle-weather.csv"),
                         "mark", "bar",
                         "encoding", Map.of(
                                 "x", Map.of("timeUnit", "month", "field", "date", "type", "ordinal"),
                         "y", Map.of("aggregate", "mean", "field", "precipitation")
                       )
                     ));
var graph2 = Nb.vega(Map.of(
                         "data", Map.of("url", "https://vega.github.io/vega-lite/data/seattle-weather.csv"),
                         "mark", "bar",
                         "encoding", Map.of(
                                 "x", Map.of("timeUnit", "month", "field", "date", "type", "ordinal"),
                         "y", Map.of("aggregate", "mean", "field", "precipitation")
                       )
                     ));
Nb.row(title("My awesome analysis"), Nb.col(graph1, graph2));

// ## 🔥 Flamegraphs
// You can profile methods and generate flamegraphs.

Runnable arrayFilling = () -> {
  List<Integer> a = new ArrayList<>();
  for (int i = 0; i < 10000000; i++) {
    a.add(i);
  }
};
var profilePath = Nb.profile(arrayFilling);
Nb.flame(profilePath);




