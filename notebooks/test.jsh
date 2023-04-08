// # le notebook de Cyril
System.out.println("<code>THIS IS CODE</code>");

// hey lol lol lol hey
"<code>THIS IS NEW CODE</code>";

throw new UnsupportedOperationException("Louise has to sleep");

import java.util.List;
import java.util.Map;

import ai.catheu.jnotebook.Nb

// ## plotly HEY hello hey
Nb.plotly(
  List.of(
    Map.of("z", List.of(List.of(1, 2, 3), List.of(3, 2, 1)), "type", "surface")
    ),
  Map.of(), Map.of());


// ## using vega hop hey hey
Nb.vega(Map.of(
                    "width", 650,
                    "height", 400,
                    "data", Map.of(
                            "url", "https://vega.github.io/vega-datasets/data/us-10m.json",
                            "format", Map.of(
                                    "type", "topojson",
                                    "feature", "counties"
                            )
                    ),
                    "transform", List.of(Map.of(
                            "lookup", "id",
                            "from", Map.of(
                                    "data", Map.of("url", "https://vega.github.io/vega-datasets/data/unemployment.tsv"),
                                    "key", "id",
                                    "fields", List.of("rate")
                            )
                    )),
                    "projection", Map.of("type", "albersUsa"),
                    "mark", "geoshape",
                    "encoding", Map.of(
                            "color", Map.of(
                                    "field", "rate",
                                    "type", "quantitative"
                            )
                    )
            ));

// ## Latex hey you youuuuuuuu

/*
This math is inline: $`a^2+b^2=c^2`$.

This math is on a separate line: hehe

```math
a^2+b^2=c^2
```

*/

//hey youuu hehe hoho hehe hoho
int errored(int lol) {
    int valid = 3;
    var x = AnErrorOf(lol);
    return x;
}

