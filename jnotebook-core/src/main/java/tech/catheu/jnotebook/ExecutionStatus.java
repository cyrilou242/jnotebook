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

