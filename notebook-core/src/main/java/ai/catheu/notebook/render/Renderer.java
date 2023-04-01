package ai.catheu.notebook.render;

import ai.catheu.notebook.evaluate.Interpreted;
import ai.catheu.notebook.evaluate.InterpretedSnippet;
import j2html.tags.DomContent;

import java.util.List;

import static j2html.TagCreator.*;
import static j2html.TagCreator.div;

public class Renderer {

  public final String render(Interpreted interpreted) {
    final LineAwareRenderer renderer = new LineAwareRenderer(interpreted.lines());
    final DomContent content = each(interpreted.interpretedSnippets(), renderer::render);

    return content.render();
  }

  public void stop() {
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
      return join(div(each(lines.subList(s.staticSnippet().start(),
                                        s.staticSnippet().end()),
                          l -> div(l).with(br()))).withClasses(
                         "viewer", "viewer-code", "w-full", "max-w-wide"),
                 div(each(s.events(), e -> div(e.value()).with(br()))).withClasses(
                         "viewer", "viewer-result", "w-full", "max-w-prose", "px-8"));
    }
  }
}
