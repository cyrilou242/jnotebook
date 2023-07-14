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
package tech.catheu.jnotebook.server;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Map;

public class HtmlTemplateEngine {

  public final static String TEMPLATE_KEY_CONFIG = "jnb_config";
  public final static String TEMPLATE_KEY_INTERACTIVE = "jnb_interactive";
  public final static String TEMPLATE_KEY_RENDERED = "jnb_rendered";

  private final TemplateEngine delegate;

  public HtmlTemplateEngine() {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    // frontend/index.html is the path of the only template used for the moment
    resolver.setPrefix("frontend/");
    resolver.setSuffix(".html");

    this.delegate  = new TemplateEngine();
    this.delegate.setTemplateResolver(resolver);
  }

  public String render(final Map<String, Object> context) {
    final Context tlContext = new Context();
    context.forEach(tlContext::setVariable);

    return delegate.process("index", tlContext);
  }

}
