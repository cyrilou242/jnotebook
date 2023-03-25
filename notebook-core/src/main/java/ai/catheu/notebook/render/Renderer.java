package ai.catheu.notebook.render;

import ai.catheu.notebook.evaluate.Interpreted;
import ai.catheu.notebook.evaluate.InterpretedSnippet;
import j2html.tags.DomContent;

import java.util.List;

import static j2html.TagCreator.*;

// todo use https://j2html.com/
// replace null by success
// add some
// generate markdown correctly
public class Renderer {

  // should be some snippets for partial update but
  // let's say we update everything for the moment;
  public final String render(Interpreted interpreted) {
    final LineAwareRenderer renderer = new LineAwareRenderer(interpreted.lines());
    final DomContent content = div(each(interpreted.interpretedSnippets(),
                                renderer::render)).withId("generated");

    return content.render();
  }

  private static class LineAwareRenderer {

    private final List<String> lines;

    public LineAwareRenderer(List<String> lines) {
      this.lines = lines;
    }

    private DomContent render(final InterpretedSnippet s) {
      return switch (s.staticSnippet().type()) {
        case JAVA -> renderJava(s);
        case MAGIC -> renderMagic(s);
        case COMMENT -> renderComment(s);
      };
    }

    private DomContent renderComment(final InterpretedSnippet s) {
      return p(each(lines.subList(s.staticSnippet().start(), s.staticSnippet().end()),
                    l -> span(l).with(br())));
    }

    private DomContent renderMagic(final InterpretedSnippet s) {
      throw new UnsupportedOperationException();
    }

    private DomContent renderJava(final InterpretedSnippet s) {
      return div(code(each(lines.subList(s.staticSnippet().start(),
                                         s.staticSnippet().end()),
                           l -> div(l).with(br()))),
                 p(each(s.events(), e -> div(e.value()).with(br()))));
    }
  }
}
