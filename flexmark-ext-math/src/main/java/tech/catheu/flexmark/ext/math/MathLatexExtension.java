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

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import org.jetbrains.annotations.NotNull;
import tech.catheu.katex.Katex;

// for the moment, exploits GitlabExtension Parser.ParserExtension implementation for inline math parsing
public class MathLatexExtension implements Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {

  private final Katex katex;

  private MathLatexExtension(final Katex katex) {
    this.katex = katex;
  }

  public static MathLatexExtension create() {
    return new MathLatexExtension(Katex.newInstance());
  }

  public static MathLatexExtension create(final Katex katex) {
    return new MathLatexExtension(katex);
  }

  @Override
  public void rendererOptions(@NotNull MutableDataHolder mutableDataHolder) {

  }

  public void extend(@NotNull HtmlRenderer.@NotNull Builder htmlRendererBuilder,
                     @NotNull String rendererType) {
    if (htmlRendererBuilder.isRendererType("HTML")) {
      htmlRendererBuilder.nodeRendererFactory(new MathLatexNodeRenderer.Factory(katex));
    }
  }

  @Override
  public void parserOptions(MutableDataHolder mutableDataHolder) {

  }

  @Override
  public void extend(Parser.Builder builder) {
    builder.customInlineParserExtensionFactory(new InlineMathParser.Factory());
  }
}
