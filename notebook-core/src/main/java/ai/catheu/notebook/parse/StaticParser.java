package ai.catheu.notebook.parse;

import ai.catheu.notebook.parse.StaticSnippet.Type;
import io.methvin.watcher.DirectoryChangeEvent;
import io.reactivex.rxjava3.annotations.NonNull;
import jdk.jshell.JShell;
import jdk.jshell.SourceCodeAnalysis.Completeness;
import jdk.jshell.SourceCodeAnalysis.CompletionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static ai.catheu.notebook.parse.StaticSnippet.Type.COMMENT;
import static ai.catheu.notebook.parse.StaticSnippet.Type.JAVA;
import static io.methvin.watcher.DirectoryChangeEvent.EventType.*;
import static jdk.jshell.SourceCodeAnalysis.Completeness.EMPTY;

public class StaticParser {

  private static final Logger LOG = LoggerFactory.getLogger(StaticParser.class);
  final JShell analysisShell = newJShell();

  public StaticParsing staticSnippets(@NonNull final DirectoryChangeEvent event) {
    try {
      final DirectoryChangeEvent.EventType type = event.eventType();
      final Path filePath = event.path();
      if (type.equals(CREATE)) {
        return snippetsOf(filePath);
      } else if (type.equals(DELETE)) {
        // delete in mem entries and clean everything
        // TODO CYRIL implement
        return new StaticParsing(filePath, null, null);
      } else if (type.equals(MODIFY)) {
        return snippetsOf(filePath);
      } else if (type.equals(OVERFLOW)) {
        // try to recover with a full reload
        return snippetsOf(filePath);
      } else {
        throw new IllegalStateException("Unknown file event kind: " + type.name());
      }
    } catch (Exception e) {
      LOG.error(e.getMessage());
      return new StaticParsing(null, null, null);
    }
  }

  private StaticParsing snippetsOf(final Path filePath) throws IOException {
    final List<String> lines = Files.readAllLines(filePath);
    List<StaticSnippet> notebookSnippets = new ArrayList<>();
    int lineIdx = 0;
    StringBuilder currentSnippet = new StringBuilder();
    int commentStartIndex = Integer.MAX_VALUE;
    int codeStartIndex = Integer.MAX_VALUE;
    boolean inMultiLineComment = false;
    CompletionInfo completionInfo = null;
    // TODO implement jshell commands and magic support
    while (lineIdx < lines.size()) {
      final String currentLine = lines.get(lineIdx);
      currentSnippet.append(currentLine);
      currentSnippet.append("\n");
      completionInfo = analysisShell.sourceCodeAnalysis()
                                    .analyzeCompletion(currentSnippet.toString());
      final Completeness completeness = completionInfo.completeness();
      if (completeness.equals(EMPTY)) {
        commentStartIndex = Math.min(commentStartIndex, lineIdx);
        if (!inMultiLineComment) {
          currentSnippet = new StringBuilder();
        } else {
          // flush the multiline comment
          notebookSnippets.add(new StaticSnippet(Type.COMMENT,
                                                 commentStartIndex,
                                                 lineIdx + 1,
                                                 null));
          commentStartIndex = Integer.MAX_VALUE;
          inMultiLineComment = false;
        }
        lineIdx++;
        continue;
      }
      if (completeness.isComplete()) {
        codeStartIndex = Math.min(codeStartIndex, lineIdx);
        if (commentStartIndex < codeStartIndex) {
          notebookSnippets.add(new StaticSnippet(Type.COMMENT,
                                                 commentStartIndex,
                                                 codeStartIndex,
                                                 null));
          commentStartIndex = Integer.MAX_VALUE;
          inMultiLineComment = false;
        }
        notebookSnippets.add(new StaticSnippet(JAVA,
                                               codeStartIndex,
                                               lineIdx + 1,
                                               completionInfo));
        codeStartIndex = Integer.MAX_VALUE;
        currentSnippet = new StringBuilder();
      } else {
        if (codeStartIndex == Integer.MAX_VALUE && !inMultiLineComment) {
          // first line of something new
          if (currentLine.stripLeading().startsWith("/*")) {
            // it's a multi-line comment
            inMultiLineComment = true;
            commentStartIndex = Math.min(commentStartIndex, lineIdx);
          } else {
            // it's java code
            codeStartIndex = lineIdx;
          }
        }
      }
      lineIdx++;
    }

    final int lastEndIndex = notebookSnippets.get(notebookSnippets.size() - 1).end();
    if (lastEndIndex != lineIdx) {
      // need to flush the last block
      if (codeStartIndex != Integer.MAX_VALUE) {
        // assume it is incomplete code
        notebookSnippets.add(new StaticSnippet(JAVA,
                                               codeStartIndex,
                                               lineIdx,
                                               completionInfo));
      } else {
        // no code was observed --> assume it's comments only
        notebookSnippets.add(new StaticSnippet(COMMENT,
                                               commentStartIndex,
                                               lineIdx,
                                               completionInfo));
      }
    }

    return new StaticParsing(filePath, lines, notebookSnippets);
  }

  private static JShell newJShell() {
    return JShell.builder()
                 // set options
                 .build();
  }


  public void stop() {
    analysisShell.close();
  }
}
