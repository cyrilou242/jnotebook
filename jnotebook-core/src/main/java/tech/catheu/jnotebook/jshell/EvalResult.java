/*
 * Copyright 2023 Cyril de Catheu
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package tech.catheu.jnotebook.jshell;

import jdk.jshell.Diag;
import jdk.jshell.SnippetEvent;

import java.util.List;

public record EvalResult(List<SnippetEvent> events,
                         String out,
                         String err,
                         List<List<Diag>> diagnostics,
                         List<List<String>> unresolvedDeps) {
}
