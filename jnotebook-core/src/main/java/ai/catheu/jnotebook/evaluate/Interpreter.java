package ai.catheu.jnotebook.evaluate;

import ai.catheu.jnotebook.parse.StaticParsing;

public interface Interpreter {

  Interpreted interpret(final StaticParsing staticParsing);

  void stop();
}
