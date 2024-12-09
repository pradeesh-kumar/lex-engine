/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import java.util.BitSet;
import org.junit.jupiter.api.Test;

public class TestMe {

  @Test
  public void test() {

    BitSet bs = new BitSet();
    bs.set(10, true);
    bs.set(12, true);
    bs.set(8, true);

    bs.stream().forEach(System.out::println);
  }
}
