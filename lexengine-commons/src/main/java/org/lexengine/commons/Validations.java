package org.lexengine.commons;

import java.util.Collection;

public final class Validations {

  public static void requireNotNull(Object o, String name) {
    if (o == null) {
      throw new IllegalArgumentException(name + " cannot be null");
    }
  }

  public static void requireNonBlank(String s, String name) {
    requireNotNull(s, name);
    if (s.trim().isEmpty()) {
      throw new IllegalArgumentException(name + " cannot be blank");
    }
  }

  public static void requireNotEmpty(Collection<?> collection, String name) {
    if (collection == null || collection.isEmpty()) {
      throw new IllegalArgumentException(name + " cannot be empty");
    }
  }
}
