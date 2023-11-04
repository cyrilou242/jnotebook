/*
 * Copyright 2023 Cyril de Catheu
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package tech.catheu.jnotebook.evaluate;

import org.checkerframework.checker.nullness.qual.NonNull;
import tech.catheu.jnotebook.ExecutionStatus;

import java.nio.file.Path;
import java.util.List;

public record Interpreted(Path path,
                          List<String> lines,
                          List<InterpretedSnippet> interpretedSnippets,
                          @NonNull ExecutionStatus status) {

}
