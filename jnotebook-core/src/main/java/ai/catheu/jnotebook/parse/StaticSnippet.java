package ai.catheu.jnotebook.parse;

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
