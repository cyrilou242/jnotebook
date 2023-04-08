package tech.catheu.jnotebook.evaluate;

import tech.catheu.jnotebook.parse.StaticParsing;

public interface Interpreter {

  Interpreted interpret(final StaticParsing staticParsing);

  void stop();
}
