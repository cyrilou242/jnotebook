package ai.catheu.notebook.jshell;

import jdk.jshell.SnippetEvent;

import java.util.List;

public record EvalResult(List<SnippetEvent> events, String out, String err) {
}
