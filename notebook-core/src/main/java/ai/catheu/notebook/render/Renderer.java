package ai.catheu.notebook.render;

import ai.catheu.notebook.evaluate.Interpreted;
import ai.catheu.notebook.evaluate.InterpretedSnippet;
import com.vladsch.flexmark.ext.gitlab.GitLabExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import j2html.tags.DomContent;
import j2html.tags.UnescapedText;
import j2html.tags.specialized.DivTag;

import java.util.Arrays;
import java.util.List;

import static j2html.TagCreator.*;

public class Renderer {

  public static final String VIEWER = "viewer";
  public static final String VIEWER_CODE = "viewer-code";
  public static final String W_FULL = "w-full";
  public static final String MAX_W_WIDE = "max-w-wide";
  public static final String MAX_W_PROSE = "max-w-prose";
  public static final String PX_8 = "px-8";
  public static final String CM_EDITOR = "cm-editor";
  public static final String CM_SCROLLER = "cm-scroller";
  public static final String CM_CONTENT = "cm-content";
  public static final String WHITESPACE_PRE = "whitespace-pre";
  public static final String CM_LINE = "cm-line";
  public static final String OVERFLOW_Y_HIDDEN = "overflow-y-hidden";
  public static final String VIEWER_HTML = "viewer-html-";

  private static final Parser parser;
  private static final HtmlRenderer renderer;

  static {
    MutableDataSet options = new MutableDataSet();
    // uncomment to set optional extensions
    options.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), GitLabExtension.create()));
    // uncomment to convert soft-breaks to hard breaks
    //options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");
    parser = Parser.builder(options).build();
    renderer = HtmlRenderer.builder(options).build();
  }

  public final String render(Interpreted interpreted) {
    final LineAwareRenderer renderer = new LineAwareRenderer(interpreted.lines());
    final DomContent content = each(interpreted.interpretedSnippets(), renderer::render);

    return content.render();
  }

  public void stop() {
  }

  private static class LineAwareRenderer {

    public static final String VIEWER_RESULT = "viewer-result";
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
      final String text = extractComment(lines.subList(s.staticSnippet().start(), s.staticSnippet().end()));
      final UnescapedText markdown = rawHtml(markdownToHtml(text));
      return htmlViewer(markdown);
    }

    private DomContent renderMagic(final InterpretedSnippet s) {
      throw new UnsupportedOperationException();
    }

    private DomContent renderJava(final InterpretedSnippet s) {
      final DomContent codeLines = each(lines.subList(s.staticSnippet().start(),
                                                 s.staticSnippet().end()),
                                   LineAwareRenderer::codeLine);
      final DivTag code = codeViewer(codeLines);
      // FIXME CYRIL make results look better
      final DomContent resultLines = each(s.evalResult().events(), e -> div(e.value()).with(br()));
      final DivTag result = resultViewer(resultLines);

      return join(code, result);
    }

    private static DomContent codeLine(final String codeLine) {
      return div(codeLine).withClasses(CM_LINE);
    }

    private static DivTag codeViewer(DomContent codeLines) {
      final DivTag content = div(codeLines).withClasses(CM_CONTENT, WHITESPACE_PRE);
      final DivTag cm = div(div(content).withClasses(CM_SCROLLER)).withClasses(CM_EDITOR);
      return div(cm).withClasses(VIEWER, VIEWER_CODE, W_FULL, MAX_W_WIDE);
    }

    private static DivTag resultViewer(DomContent resultLines) {
      final DomContent resultLines1 = div(div(resultLines).withClasses(OVERFLOW_Y_HIDDEN)).withClasses("relative");
      return div(resultLines1).withClasses(VIEWER,
                                           VIEWER_RESULT,
                                           W_FULL,
                                           MAX_W_PROSE,
                                           PX_8);
    }

    private static DivTag htmlViewer(final DomContent markdown) {
      final DomContent markdownViewer = div(markdown).withClasses("viewer-markdown");
      return div(markdownViewer).withClasses(VIEWER, VIEWER_HTML, W_FULL, MAX_W_PROSE, PX_8);
    }

    private static String markdownToHtml(final String markdown) {
      final Node document = parser.parse(markdown);
      return renderer.render(document);
    }

    public static String extractComment(final List<String> lines) {
      StringBuilder comment = new StringBuilder();
      boolean inComment = false;
      boolean inJavadoc = false;

      for (String line : lines) {
        line = line.trim();
        if (line.startsWith("//")) {
          comment.append(line.substring(2).trim()).append("\n");
        } else if (line.startsWith("/*")) {
          inComment = true;
          if (line.contains("*/")) {
            inComment = false;
            comment.append(line.substring(line.indexOf("*/") + 2).trim()).append("\n");
          } else {
            comment.append(line.substring(2).trim()).append("\n");
          }
        } else if (line.startsWith("*")) {
          inJavadoc = true;
          if (line.contains("*/")) {
            inJavadoc = false;
            comment.append(line.substring(line.indexOf("*/") + 2).trim()).append("\n");
          } else {
            comment.append(line.substring(1).trim()).append("\n");
          }
        } else if (inComment) {
          if (line.contains("*/")) {
            inComment = false;
            comment.append(line.substring(0, line.indexOf("*/")).trim()).append("\n");
          } else {
            comment.append(line.trim()).append("\n");
          }
        } else if (inJavadoc) {
          if (line.contains("*/")) {
            inJavadoc = false;
            comment.append(line.substring(line.indexOf("*/") + 2).trim()).append("\n");
          } else {
            comment.append(line.trim()).append("\n");
          }
        }
      }

      return comment.toString().trim();
    }
  }
}
