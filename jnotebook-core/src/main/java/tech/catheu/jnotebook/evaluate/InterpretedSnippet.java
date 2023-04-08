package tech.catheu.jnotebook.evaluate;

import tech.catheu.jnotebook.jshell.EvalResult;
import tech.catheu.jnotebook.parse.StaticSnippet;

public record InterpretedSnippet(StaticSnippet staticSnippet, EvalResult evalResult) {
}
