package org.lexengine.lexer.gentest;

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;

public class MyLexerTest {

  @Test
  public void lexerTest() throws FileNotFoundException {
    MyLexer lexer = new MyLexer(getClass().getClassLoader().getResource("test-valid-source.txt").getFile());
    if (lexer.hasNext()) {
      System.out.println(lexer.next());
    }
  }
}
