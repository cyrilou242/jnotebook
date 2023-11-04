/*
 * Copyright 2023 Cyril de Catheu
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
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
