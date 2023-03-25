package ai.catheu.notebook.evaluate;

import java.nio.file.Path;
import java.util.List;

public record Interpreted(Path path,
                          List<String> lines,
                          List<InterpretedSnippet> interpretedSnippets) {
}
