package ai.catheu.notebook.evaluate;

import ai.catheu.notebook.jshell.EvalResult;
import ai.catheu.notebook.parse.StaticSnippet;

public record InterpretedSnippet(StaticSnippet staticSnippet, EvalResult evalResult) {
}
