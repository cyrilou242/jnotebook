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
package tech.catheu.katex;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;

/**
 * katex javascript library adapter.
 * See https://katex.org/.
 * Can transform latex into html.
 * <p>
 * A cache is used. Caching is around 300 times faster than re-evaluating.
 * For load patterns that may often re-evaluate the same input (like jnotebook),
 * caching is important.
 */
public class Katex {

  private static final Logger LOG = LoggerFactory.getLogger(Katex.class);
  private static final String KATEX_PATH = "/katex/katex.min.0.16.8.js";

  public static final int DEFAULT_CACHE_SIZE = 1000;
  public static final CacheBuilder<Object, Object> DEFAULT_CACHE_BUILDER =
          CacheBuilder.newBuilder().maximumSize(DEFAULT_CACHE_SIZE);

  private final LoadingCache<RenderToStringConfiguration, String> renderToStringCache;
  private final ScriptEngine engine;

  public static Katex newInstance() {
    return new Katex(DEFAULT_CACHE_BUILDER);
  }

  public static Katex newInstance(final CacheBuilder<Object, Object> builder) {
    return new Katex(builder);
  }

  private Katex(final CacheBuilder<Object, Object> builder) {
    engine = new ScriptEngineManager().getEngineByName("graal.js");
    loadKatexScript();

    renderToStringCache = builder.build(CacheLoader.from(this::computeRenderToString));

  }

  private void loadKatexScript() {
    try (InputStream in = Katex.class.getResourceAsStream(KATEX_PATH); final BufferedReader reader = new java.io.BufferedReader(
            new InputStreamReader(in))) {
      engine.eval(reader);
    } catch (IOException e) {
      LOG.error("Failed loading latex katex engine {}.", KATEX_PATH);
      throw new RuntimeException("Could not load latex engine.", e);
    } catch (ScriptException e) {
      LOG.error("Failed running katex script.");
      throw new RuntimeException("Could not initialize latex engine.", e);
    }
  }

  public String renderToString(final String latexExpression, final boolean displayMode) {
    try {
      return renderToStringCache.get(new RenderToStringConfiguration(latexExpression,
                                                                     displayMode));
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  private String computeRenderToString(final RenderToStringConfiguration config) {
    final StringBuilder script = new StringBuilder("katex.renderToString(\"");
    script.append(config.latexExpression.replace("\\", "\\\\").replace("\n", "\\n"));
    // inline (displayMode=false) not implemented yet
    script.append("\",{throwOnError:false,displayMode:");
    script.append(config.displayMode ? "true" : "false");
    script.append("})");
    final String scriptString = script.toString();

    try {
      return engine.eval(scriptString).toString();
    } catch (Exception e) { // ScriptException
      LOG.error("Failed running script {}. Input: {}", scriptString, config);
      throw new RuntimeException("Failed running katex script", e);
    }
  }

  private record RenderToStringConfiguration(String latexExpression,
                                             boolean displayMode) {
  }

}
