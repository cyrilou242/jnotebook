/*
 * Copyright 2023 Cyril de Catheu
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package tech.catheu.jnotebook.evaluate;

import tech.catheu.jnotebook.parse.StaticParsing;

public interface Interpreter {

  Interpreted interpret(final StaticParsing staticParsing);

  void stop();
}
