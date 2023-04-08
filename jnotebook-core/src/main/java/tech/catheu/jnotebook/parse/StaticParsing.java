package tech.catheu.jnotebook.parse;

import java.nio.file.Path;
import java.util.List;

public record StaticParsing(Path path, List<String> lines, List<StaticSnippet> snippets) {
}
