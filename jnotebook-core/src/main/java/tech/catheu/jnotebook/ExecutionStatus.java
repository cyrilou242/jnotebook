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
package tech.catheu.jnotebook;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import static tech.catheu.jnotebook.ExecutionStatus.Status.FAILURE;
import static tech.catheu.jnotebook.ExecutionStatus.Status.OK;

public record ExecutionStatus(@NonNull Status status,
                              @Nullable String failureMessage,
                              @Nullable Exception failureException) {

  public enum Status {
    OK,
    FAILURE
  }

  public boolean isOk() {
    return this.status.equals(OK);
  }

  public static ExecutionStatus ok() {
    return new ExecutionStatus(OK, null, null);
  }

  public static ExecutionStatus failure(@NonNull final String failureMessage,
                                 @Nullable final Exception failureException) {
    return new ExecutionStatus(FAILURE, failureMessage, failureException);
  }
}

