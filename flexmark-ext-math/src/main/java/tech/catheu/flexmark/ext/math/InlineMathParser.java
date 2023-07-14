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

import com.vladsch.flexmark.parser.InlineParser;
import com.vladsch.flexmark.parser.InlineParserExtension;
import com.vladsch.flexmark.parser.InlineParserExtensionFactory;
import com.vladsch.flexmark.parser.LightInlineParser;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// copy of GitLabInlineMathParser
public class InlineMathParser implements InlineParserExtension {

  Pattern MATH_PATTERN = Pattern.compile("\\$`((?:.|\n)*?)`\\$");

  public InlineMathParser(LightInlineParser inlineParser) {
  }

  @Override
  public void finalizeDocument(@NotNull InlineParser inlineParser) {

  }

  @Override
  public void finalizeBlock(@NotNull InlineParser inlineParser) {

  }

  @Override
  public boolean parse(@NotNull LightInlineParser inlineParser) {
    if (inlineParser.peek(1) == '`') {
      BasedSequence input = inlineParser.getInput();
      Matcher matcher = inlineParser.matcher(MATH_PATTERN);
      if (matcher != null) {
        inlineParser.flushTextNode();

        BasedSequence mathOpen = input.subSequence(matcher.start(), matcher.start(1));
        BasedSequence mathClosed = input.subSequence(matcher.end(1), matcher.end());
        InlineMath inlineMath = new InlineMath(mathOpen,
                                                           mathOpen.baseSubSequence(
                                                                   mathOpen.getEndOffset(),
                                                                   mathClosed.getStartOffset()),
                                                           mathClosed);
        inlineParser.getBlock().appendChild(inlineMath);
        return true;
      }
    }
    return false;
  }

  public static class Factory implements InlineParserExtensionFactory {
    @Nullable
    @Override
    public Set<Class<?>> getAfterDependents() {
      return null;
    }

    @NotNull
    @Override
    public CharSequence getCharacters() {
      return "$";
    }

    @Nullable
    @Override
    public Set<Class<?>> getBeforeDependents() {
      return null;
    }

    @NotNull
    @Override
    public InlineParserExtension apply(@NotNull LightInlineParser lightInlineParser) {
      return new InlineMathParser(lightInlineParser);
    }

    @Override
    public boolean affectsGlobalScope() {
      return false;
    }
  }
}
