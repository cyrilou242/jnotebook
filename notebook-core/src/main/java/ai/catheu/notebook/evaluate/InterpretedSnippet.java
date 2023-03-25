package ai.catheu.notebook.evaluate;

import ai.catheu.notebook.parse.StaticSnippet;
import jdk.jshell.SnippetEvent;

import java.util.List;

public record InterpretedSnippet(StaticSnippet staticSnippet, List<SnippetEvent> events) {
}
