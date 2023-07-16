// # 📖 Book of jnotebook
// ## ⚖️ Rationale
// Computational notebooks allow arguing from evidence by mixing prose with executable code. For a good overview of problems users encounter in traditional notebooks like Jupyter, see [I don't like notebooks](https://www.youtube.com/watch?v=7jiPeIFXb6U) and [What’s Wrong with Computational Notebooks? Pain Points, Needs, and Design Opportunities](https://austinhenley.com/pubs/Chattopadhyay2020CHI_NotebookPainpoints.pdf).
//
// `jnotebook` tries to address the following problems:
//
// * notebook editors are less helpful than IDE editors
// * notebook code is hard to reuse
// * out-of-order execution causes reproducibility issues
// * notebook code is hard to version control
// * the Java ecosystem does not provide a great experience for visualization and document formatting
//
// `jnotebook` is a notebook library for Java that address these problems by doing less, namely:
// * no editing environment: you can keep the code editor you know and love
// * (almost) no new format: `jnotebook` interprets [JShell files](https://docs.oracle.com/en/java/javase/20/JShell/scripts.html#GUID-C3A41878-9A9A-4D31-BBDF-909729848A3E) and renders them as notebook.
//   Because `jnotebook` is not required to run JShell files, it does not introduce a dependency if you wish to run the JShell file in production.
// * no out-of-order execution: `jnotebook` always evaluates from top to bottom. `jnotebook` builds a dependency graph of Java statements and only recomputes the needed changes to keep the feedback loop fast.
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
// Launch it.
// ```
// ./jnotebook server
//
// # or
// java -jar jnotebook server
// ```
// Then go to <a href="http://localhost:5002" target="_blank">http://localhost:5002</a>.
// By default, the notebook folder is `notebooks`. If it does not exist, it will be created with an example notebook. 
// `jnotebook` automatically detects when a `.jsh` file in the `notebooks` folder is edited
// and renders it in the web app.

// Once your notebook is ready to be published, render it in a single html file with:
// ```
// ./jnotebook render notebooks/my_notebook.jsh my_notebook.html
// ```

// ### Install
// See detailed installation instruction for different platforms in the <a href="https://github.com/cyrilou242/jnotebook/#install" target="_blank">github project</a>.

// ### 🤹 Demo notebooks
// *Coming soon.*

// ### 🔌 In an Existing Project
// #### Maven
// When launched within a maven project, `jnotebook` automatically injects the project
// dependencies in the classpath. If launched in a submodule, only the submodule
// dependencies are injected.

// #### Manual
// Dependencies can be injected manually with the `-cp=<CLASSPATH>` parameter.

// ### IDE integration
// #### IntelliJ
// Enable IntelliSense highlighting and code utilities for JShell `.jsh` files:
// 1. Go to **Settings** | **Editor** | **File Types**
// 2. Click on **JShell snippet**
// 3. In **file name patterns**, click **+** (**add**)
// 4. Add `*.jsh`.

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
String s5 = "Hello Alex";

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

// Mistakes happen! `jnotebook` tries its best to give helpful error messages.
invalidJava();
//
// Exceptions happen too!
throw new RuntimeException("Panic!");

// ### Markdown
// Latex is supported inline: <pre>\$\`a^2+b^2=c^2\`\$</pre> → will render as $`a^2+b^2=c^2`$.
// and as block:
// <pre>
// ```math
// a^2+b^2=c^2
// ```
// </pre>
// ```math
// a^2+b^2=c^2
// ```
//
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
// <pre>```mermaid
// [MERMAID GRAPH CODE]
// ```</pre>
// See [mermaid documentation](https://mermaid.js.org/intro/) for examples.

// ## 🔍 Viewers

// `jnotebook` provides viewers and utils for data, tables, plots, flamegraphs etc.
// These utils are packaged in a separate dependency `jnotebook-utils`. By default, `jnotebook-utils` is in the classpath.
// All utils are available as static method in `tech.catheu.jnotebook.Nb`.

import tech.catheu.jnotebook.Nb;

// ## 🔢 Tables
// *coming soon*

// ## 📊 Plotly
// `jnotebook` has built-in support for Plotly's low-ceremony plotting. See Plotly's JavaScript [docs](https://plotly.com/javascript/) for more examples and [options](https://plotly.com/javascript/configuration-options/).
Nb.plotly(List.of(
          Map.of("z", List.of(List.of(1, 2, 3), List.of(3, 2, 1)), "type", "surface")),
          Map.of(),
          Map.of());

// ## 🗺 Vega Lite
// `jnotebook` also supports [Vega Lite](https://vega.github.io/vega-lite/).
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

// ## Credits
// This documentation is directly copying some content from the [book of Clerk](https://book.clerk.vision/#rationale), a notebook system for Clojure.




