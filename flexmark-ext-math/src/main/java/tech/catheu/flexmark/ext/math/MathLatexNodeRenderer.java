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
package tech.catheu.flexmark.ext.math;

import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.html.HtmlRendererOptions;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.catheu.katex.Katex;

import java.util.Set;

public class MathLatexNodeRenderer implements NodeRenderer {

  private final Katex katex;

  public MathLatexNodeRenderer(final DataHolder options, Katex katex) {
    // no options for the moment
    this.katex = katex;
  }

  @Override
  public @Nullable Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
    return Set.of(new NodeRenderingHandler<>(InlineMath.class,
                                             MathLatexNodeRenderer.this::render),
                  new NodeRenderingHandler<>(FencedCodeBlock.class,
                                             MathLatexNodeRenderer.this::render));
  }

  private void render(@NotNull InlineMath node,
                      @NotNull NodeRendererContext context, @NotNull HtmlWriter html) {
    final String latexExpression = node.getText().unescape();
    final String katexHtml = katex.renderToString(latexExpression, false);
    html.raw(katexHtml);
  }

  private <N extends Node> void render(@NotNull FencedCodeBlock node,
                                       @NotNull NodeRendererContext context,
                                       @NotNull HtmlWriter html) {
    HtmlRendererOptions htmlOptions = context.getHtmlOptions();
    BasedSequence language = node.getInfoDelimitedByAny(htmlOptions.languageDelimiterSet);

    if (language.isIn(Set.of("math"))) {
      html.line();
      final String latexExpression = node.getContentChars().normalizeEOL();
      final String katexHtml = katex.renderToString(latexExpression, true);
      html.append(katexHtml);

      html.lineIf(htmlOptions.htmlBlockCloseTagEol);
    } else {
      context.delegateRender();
    }
  }

  public static class Factory implements NodeRendererFactory {

    public final Katex katex;

    public Factory(Katex katex) {
      this.katex = katex;
    }

    @NotNull
    @Override
    public NodeRenderer apply(@NotNull DataHolder options) {
      return new MathLatexNodeRenderer(options, katex);
    }
  }
}
