/*
 * Copyright Cyril de Catheu, 2023
 *
 * Licensed under the JNOTEBOOK LICENSE 1.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at https://raw.githubusercontent.com/cyrilou242/jnotebook/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT * WARRANTIES OF ANY KIND,
 * either express or implied.
 * See the License for the specific language governing permissions and limitations under
 * the License.
 */
package tech.catheu.jnotebook.render;

import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.ext.gitlab.GitLabExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import j2html.tags.DomContent;
import j2html.tags.UnescapedText;
import j2html.tags.specialized.DivTag;
import jdk.jshell.Diag;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.catheu.flexmark.ext.math.MathLatexExtension;
import tech.catheu.jnotebook.evaluate.Interpreted;
import tech.catheu.jnotebook.evaluate.InterpretedSnippet;
import tech.catheu.jnotebook.jshell.EvalResult;
import tech.catheu.jnotebook.parse.StaticSnippet;
import tech.catheu.katex.Katex;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static j2html.TagCreator.*;

public class Renderer {

  private static final Logger LOG = LoggerFactory.getLogger(Renderer.class);

  public static final String CM_CONTENT = "cm-content";
  public static final String CM_EDITOR = "cm-editor";
  public static final String CM_FAILURE = "cm-failure";
  public static final String CM_SCROLLER = "cm-scroller";
  public static final String CM_SUCCESS = "cm-success";
  public static final String MAX_W_WIDE = "max-w-wide";
  public static final String MAX_W_PROSE = "max-w-prose";
  public static final String OVERFLOW_Y_HIDDEN = "overflow-y-hidden";
  public static final String PX_8 = "px-8";
  public static final String RELATIVE = "relative";
  public static final String VIEWER = "viewer";
  public static final String VIEWER_CODE = "viewer-code";
  public static final String VIEWER_HTML = "viewer-html-";
  public static final String VIEWER_MARKDOWN = "viewer-markdown";
  public static final String VIEWER_RESULT = "viewer-result";
  public static final String W_FULL = "w-full";
  public static final String WHITESPACE_PRE = "whitespace-pre";

  private static final Parser parser;
  private static final HtmlRenderer renderer;
  public static final String RESULT_ERROR = "result-error";

  static {
    final MutableDataSet options = new MutableDataSet();
    options.set(Parser.EXTENSIONS,
                Arrays.asList(TablesExtension.create(),
                              GitLabExtension.create(),
                              FootnoteExtension.create(),
                              MathLatexExtension.create(Katex.newInstance())));
    // convert soft-breaks to hard breaks
    options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");
    options.set(HtmlRenderer.GENERATE_HEADER_ID, true);
    options.set(HtmlRenderer.RENDER_HEADER_ID, true);
    // MathLatexExtension is taking care of inline and block math
    options.set(GitLabExtension.RENDER_BLOCK_MATH, false);
    options.set(GitLabExtension.INLINE_MATH_PARSER, false);
    parser = Parser.builder(options).build();
    renderer = HtmlRenderer.builder(options).build();
  }

  public final Rendering render(Interpreted interpreted) {
    if (!interpreted.status().isOk()) {
      // FIXME CYRIL - improve format
      return new Rendering(pre(interpreted.status().failureMessage()).withClasses(RESULT_ERROR).render());
    }

    final LineAwareRenderer domRenderer = new LineAwareRenderer(interpreted.lines());
    final DomContent content = domRenderer.render(interpreted);

    return new Rendering(content.render());
  }

  public void stop() {
  }

  private static class LineAwareRenderer {
    private final List<DomContent> elems = new ArrayList<>();
    private final List<InterpretedSnippet> groupedJavaSnippets = new ArrayList<>();
    private final List<String> lines;
    private final Map<EvalResult, EvalHtmlOutputs> outputsCache = new HashMap<>();

    public LineAwareRenderer(List<String> lines) {
      this.lines = lines;
    }

    // can only be run once
    public final DomContent render(final Interpreted interpreted) {
      if (interpreted.interpretedSnippets().isEmpty()) {
        return b(interpreted.path() + " is empty or was deleted.");
      }

      for (final InterpretedSnippet snippet : interpreted.interpretedSnippets()) {
        if (snippet.staticSnippet().type().equals(StaticSnippet.Type.COMMENT)) {
          if (!groupedJavaSnippets.isEmpty()) {
            flushGroupedJavaSnippets();
          }
          elems.add(renderComment(snippet));
        } else if (snippet.staticSnippet().type().equals(StaticSnippet.Type.MAGIC)) {
          throw new UnsupportedOperationException("MAGIC not implemented yet");
        } else {
          final EvalHtmlOutputs res = getHtmlOuputs(snippet.evalResult());
          if (!res.errors.isEmpty()) {
            // never group a snippet with others if it contains an error
            flushGroupedJavaSnippets();
            elems.add(renderGroupedJava(List.of(snippet)));
            continue;
          }
          if (!groupedJavaSnippets.isEmpty()) {
            final InterpretedSnippet lastSnippet =
                    groupedJavaSnippets.get(groupedJavaSnippets.size() - 1);
            if (lastSnippet.staticSnippet().end() != snippet.staticSnippet().start()) {
              flushGroupedJavaSnippets();
            }
          }
          groupedJavaSnippets.add(snippet);
        }
      }
      flushGroupedJavaSnippets();

      return join(elems.toArray());

    }

    private void flushGroupedJavaSnippets() {
      if (!groupedJavaSnippets.isEmpty()) {
        elems.add(renderGroupedJava(groupedJavaSnippets));
        groupedJavaSnippets.clear();
      }
    }

    private DomContent renderComment(final InterpretedSnippet s) {
      final String text = extractComment(lines.subList(s.staticSnippet().start(),
                                                       s.staticSnippet().end()));
      final UnescapedText markdown = rawHtml(markdownToHtml(text));
      return htmlViewer(markdown);
    }

    private DomContent renderMagic(final InterpretedSnippet s) {
      throw new UnsupportedOperationException();
    }

    private DomContent renderGroupedJava(final List<InterpretedSnippet> snippets) {
      checkArgument(snippets.size() > 0);

      final String codeLines = combineCodeLines(snippets);
      final EvalHtmlOutputs htmlOuputs = combineOutputs(snippets);

      final DivTag code = codeViewer(codeLines, htmlOuputs.errors.isEmpty());
      if (htmlOuputs.evalRes == null && htmlOuputs.stdOut == null && htmlOuputs.errors.isEmpty()) {
        return code;
      }
      final UnescapedText htmlResults = join(htmlOuputs.stdOut,
                                             htmlOuputs.evalRes,
                                             join(htmlOuputs.errors.toArray()));
      final DivTag result = resultViewer(htmlResults, htmlOuputs.errors.isEmpty());

      return join(code, result);
    }

    @NotNull
    private EvalHtmlOutputs combineOutputs(List<InterpretedSnippet> snippets) {
      // take last result only, but group all std out
      final InterpretedSnippet lastSnippet = snippets.get(snippets.size() - 1);
      final EvalHtmlOutputs lastHtmlOutput = getHtmlOuputs(lastSnippet.evalResult());
      final List<Object> combinedStdOut = new ArrayList<>();
      for (final InterpretedSnippet snippet : snippets) {
        final EvalHtmlOutputs outputs = getHtmlOuputs(snippet.evalResult());
        if (outputs.stdOut != null) {
          combinedStdOut.add(outputs.stdOut);
        }
      }
      return new EvalHtmlOutputs(lastHtmlOutput.evalRes,
                                 combinedStdOut.isEmpty() ? null
                                                          : join(combinedStdOut.toArray()),
                                 lastHtmlOutput.errors);
    }

    @NotNull
    private String combineCodeLines(List<InterpretedSnippet> snippets) {
      final int numSnippets = snippets.size();
      final InterpretedSnippet firstSnippet = snippets.get(0);
      final InterpretedSnippet lastSnippet = snippets.get(numSnippets - 1);
      return String.join("\n",
                         lines.subList(firstSnippet.staticSnippet().start(),
                                       lastSnippet.staticSnippet().end()));
    }

    private EvalHtmlOutputs getHtmlOuputs(final EvalResult evalResult) {
      return outputsCache.computeIfAbsent(evalResult, this::computeHtmlOuputs);
    }

    private EvalHtmlOutputs computeHtmlOuputs(final EvalResult evalResult) {
      DomContent evalRes = null;
      final List<DomContent> errors = new ArrayList<>();
      if (!evalResult.events().isEmpty()) {
        final SnippetEvent snippetEvent = evalResult.events().get(0);
        if (snippetEvent.status().equals(Snippet.Status.VALID)) {
          final String value = snippetEvent.value();
          if (value != null && !value.isBlank() && !value.equals("null")) {
            // allow interpretation
            evalRes = div(rawHtml(value));
          }
          if (snippetEvent.exception() != null) {
            errors.add(join(div(snippetEvent.exception().toString())));
          }
        } else {
          final String errorMessage = buildErrorMessage(evalResult);
          errors.add(pre(errorMessage));
        }
      }
      if (evalResult.events().size() > 1) {
        LOG.debug("Skipping snippet events of index >=1");
      }
      DomContent stdOut = null;
      if (!evalResult.out().isEmpty()) {
        stdOut = div(evalResult.out());
      }
      if (!evalResult.err().isEmpty()) {
        errors.add(div(evalResult.out()));
      }

      return new EvalHtmlOutputs(evalRes, stdOut, errors);
    }

    @NotNull
    private static String buildErrorMessage(EvalResult evalResult) {
      final List<Diag> diagnostics = Optional.ofNullable(evalResult.diagnostics())
                                             .map(l -> l.get(0))
                                             .orElse(Collections.emptyList());
      if (!diagnostics.isEmpty()) {
        StringBuilder s = new StringBuilder();
        for (Diag d : diagnostics) {
          final String errorMessage = d.getMessage(Locale.ENGLISH);
          final String source = evalResult.events().get(0).snippet().source();
          int startPosition = (int) d.getStartPosition();
          int endPosition = (int) d.getEndPosition();
          s.append(buildErrorMessage(errorMessage, source, startPosition, endPosition));
        }
        return s.toString();
      }
      if (!evalResult.unresolvedDeps().isEmpty()) {
        final StringBuilder message = new StringBuilder("Unresolved dependencies: \n");
        evalResult.unresolvedDeps()
                  .forEach(deps -> deps.forEach(d -> message.append(d).append("\n")));
        return message.toString();
      }

      return "Invalid snippet. Could not diagnose the issue error";
    }

    private static StringBuilder buildErrorMessage(String errorMessage, String source,
                                                   int startPosition, int endPosition) {
      final StringBuilder s = new StringBuilder();
      s.append("Error: \n").append(errorMessage).append("\n");
      for (final String line : source.split("\n")) {
        if (line.length() < startPosition) {
          s.append(line).append("\n");
          startPosition = startPosition - line.length() - 1;
          endPosition = endPosition - line.length() - 1;
        } else {
          s.append(line).append("\n");
          s.append(" ".repeat(startPosition))
           .append("^".repeat(endPosition - startPosition + 1))
           .append("\n");
          break;
        }
      }
      return s;
    }

    private static DivTag codeViewer(final String codeLines, final boolean success) {
      final DivTag content = div(codeLines).withClasses(CM_CONTENT,
                                                        WHITESPACE_PRE,
                                                        success ? CM_SUCCESS
                                                                : CM_FAILURE);
      final DivTag cm = div(div(content).withClasses(CM_SCROLLER)).withClasses(CM_EDITOR);
      return div(cm).withClasses(VIEWER, VIEWER_CODE, W_FULL, MAX_W_WIDE);
    }

    private static DivTag resultViewer(final DomContent results, final boolean success) {
      final DomContent resultLines1 =
              div(div(results).withClasses(OVERFLOW_Y_HIDDEN)).withClasses(RELATIVE);

      final List<String> classes = new ArrayList<>();
      classes.addAll(List.of(VIEWER, VIEWER_RESULT, W_FULL, MAX_W_PROSE, PX_8));
      if (!success) {
        classes.add(RESULT_ERROR);
      }
      return div(resultLines1).withClasses(classes.toArray(new String[]{}));
    }

    private static DivTag htmlViewer(final DomContent markdown) {
      final DomContent markdownViewer = div(markdown).withClasses(VIEWER_MARKDOWN);
      return div(markdownViewer).withClasses(VIEWER,
                                             VIEWER_HTML,
                                             W_FULL,
                                             MAX_W_PROSE,
                                             PX_8);
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

  private record EvalHtmlOutputs(DomContent evalRes,
                                 DomContent stdOut,
                                 List<DomContent> errors) {
  }
}
