/*
 * Copyright 2023 Cyril de Catheu
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package tech.catheu.jnotebook.parse;

import jdk.jshell.SourceCodeAnalysis.CompletionInfo;

public record StaticSnippet(Type type,
                            int start,
                            int end,
                            CompletionInfo completionInfo) {
  public enum Type {
    JAVA,
    COMMENT,
    MAGIC
  }
}
