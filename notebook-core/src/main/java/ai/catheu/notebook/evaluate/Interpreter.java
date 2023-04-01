package ai.catheu.notebook.evaluate;

import ai.catheu.notebook.parse.StaticParsing;

public interface Interpreter {

  Interpreted interpret(final StaticParsing staticParsing);

  void stop();
}
