/*
 * Copyright 2023 Cyril de Catheu
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package tech.catheu.jnotebook.parse;

import org.checkerframework.checker.nullness.qual.NonNull;
import tech.catheu.jnotebook.ExecutionStatus;

import java.nio.file.Path;
import java.util.List;

public record StaticParsing(@NonNull Path path,
                            @NonNull List<String> lines,
                            @NonNull List<StaticSnippet> snippets,
                            @NonNull ExecutionStatus executionStatus) {
}
