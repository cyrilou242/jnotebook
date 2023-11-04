/*
 * Copyright 2023 Cyril de Catheu
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package tech.catheu.jnotebook.utils;

import java.util.Optional;

public class JavaUtils {

  public static <T> Optional<T> optional(final T obj) {
    return Optional.ofNullable(obj);
  }
}
