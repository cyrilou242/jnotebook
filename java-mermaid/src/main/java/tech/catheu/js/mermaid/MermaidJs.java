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
package tech.catheu.js.mermaid;

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

public class MermaidJs implements Mermaid {

  private static final Logger LOG = LoggerFactory.getLogger(MermaidJs.class);
  private static final String MERMAID_PATH = "/mermaid/mermaid.min.10.2.4.js";

  public static final int DEFAULT_CACHE_SIZE = 1000;
  public static final CacheBuilder<Object, Object> DEFAULT_CACHE_BUILDER =
          CacheBuilder.newBuilder().maximumSize(DEFAULT_CACHE_SIZE);

  private final LoadingCache<String, String> renderCache;
  private final ScriptEngine engine;

  public static MermaidJs newInstance() {
    return new MermaidJs(DEFAULT_CACHE_BUILDER);
  }

  public static MermaidJs newInstance(final CacheBuilder<Object, Object> builder) {
    return new MermaidJs(builder);
  }

  private MermaidJs(final CacheBuilder<Object, Object> builder) {
    engine = new ScriptEngineManager().getEngineByName("graal.js");
    loadMermaidScript();

    renderCache = builder.build(CacheLoader.from(this::computeRenderToString));
  }

  private void loadMermaidScript() {
    try (InputStream in = MermaidJs.class.getResourceAsStream(MERMAID_PATH); final BufferedReader reader = new java.io.BufferedReader(
            new InputStreamReader(in))) {
      engine.eval(reader);
    } catch (IOException e) {
      LOG.error("Failed loading mermaid engine {}.", MERMAID_PATH);
      throw new RuntimeException("Could not load mermaid engine.", e);
    } catch (ScriptException e) {
      LOG.error("Failed running katex script.");
      throw new RuntimeException("Could not initialize mermaid engine.", e);
    }
  }

  @Override
  public String render(String graphDefintion) {
    try {
      return renderCache.get(graphDefintion);
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  private String computeRenderToString(String graphDefinition) {
    //await mermaid.render('noUseId', "graph TB\na-->b", ).then(r=>{return r.svg;}).catch(error=>{return "<pre>"+error.message+"</pre>";});
    final String preparedGraphDefinition =
            graphDefinition.replace("\\", "\\\\").replace("\n", "\\n");
    final String script =
            "await mermaid.render('noUseId',\"" + preparedGraphDefinition + ").then(r=>{return r.svg;}).catch(error=>{return \"<pre>Mermaid: \" + error.message + \"</pre>\";})";
    try {
      return engine.eval(script).toString();
    } catch (ScriptException e) {
      LOG.error("Failed running script {}. Input: {}", script, graphDefinition);
      throw new RuntimeException("Failed running mermaid script", e);
    }
  }

  public static void main(String[] args) {
    var mermaid = MermaidJs.newInstance();
    final String lol = mermaid.render("graph TB\na-->b");
    final String error = mermaid.render("graph TBLOL\na-->b");
    String haha = "ha";
  }
}
