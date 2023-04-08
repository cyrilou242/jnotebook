package ai.catheu.jnotebook.evaluate;

import ai.catheu.jnotebook.jshell.EvalResult;
import ai.catheu.jnotebook.parse.StaticSnippet;

public record InterpretedSnippet(StaticSnippet staticSnippet, EvalResult evalResult) {
}
