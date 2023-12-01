/*
 * Copyright 2023 Cyril de Catheu
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package tech.catheu.jnotebook.server;

import gg.jte.CodeResolver;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.TemplateOutput;
import gg.jte.output.StringOutput;
import gg.jte.resolve.DirectoryCodeResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.catheu.jnotebook.Main;
import tech.catheu.jnotebook.utils.JavaUtils;

import java.nio.file.Path;

public class HtmlTemplateEngine {

  private static final Logger LOG = LoggerFactory.getLogger(Main.class);
  private final TemplateEngine delegate;

  public HtmlTemplateEngine() {
    if (JavaUtils.RUN_IN_JAR) {
      this.delegate = TemplateEngine.createPrecompiled(ContentType.Html);
    } else {
      LOG.warn("Using dynamic templates. This should only happen in development.");
      final CodeResolver codeResolver = new DirectoryCodeResolver(Path.of("jnotebook-core/src/main/jte"));
      this.delegate = TemplateEngine.create(codeResolver, ContentType.Html);
    }
  }

  // render is the actual generated html
  public String render(final Main.SharedConfiguration config, final boolean interactive,
                       final String render) {
    final TemplateModel model =  new TemplateModel(config, interactive, render);
    final TemplateOutput output = new StringOutput();
    delegate.render("index.jte", model, output);
    return output.toString();
  }

  public record TemplateModel(Main.SharedConfiguration config, boolean interactive, String render) {}


}
