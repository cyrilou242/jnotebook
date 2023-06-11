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
