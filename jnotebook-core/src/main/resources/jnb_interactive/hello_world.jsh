// # Hello world
// Cells are delimited by blank lines

String s1 = "Hello";

String s2 = "World";

// for multi-statements cells, only the last value or method result is returned.

String s3 = "!";
String greeting = s1 + " " + s2 + s3;

// import Nb to get access to built-in integrations
import tech.catheu.jnotebook.Nb;

Nb.plotly(List.of(
          Map.of("z", List.of(List.of(1, 2, 3), List.of(1, 2, 3)), "type", "surface")),
          Map.of(),
          Map.of());

// ## markdown
// ### is
// #### supported
// Learn more on [jnotebook.catheu.tech](https://jnotebook.catheu.tech)
