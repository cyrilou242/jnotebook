/*
 * Copyright 2023 Cyril de Catheu
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package tech.catheu.jnotebook.render;

public record Rendering(String html) {

  public static Rendering of(final String html) {
    return new Rendering(html);
  }
}
