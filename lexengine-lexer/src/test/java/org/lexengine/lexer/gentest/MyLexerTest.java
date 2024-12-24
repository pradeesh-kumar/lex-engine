package org.lexengine.lexer.gentest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

public class MyLexerTest {

  @Test
  public void lexerTest() throws IOException {
    MyLexer lexer = new MyLexer(getClass().getClassLoader().getResource("test-valid-source.txt").getFile());
    Iterator<String> expectedTokens = Files.readAllLines(Path.of(getClass().getClassLoader().getResource("test-expected-tokens.txt").getFile())).iterator();
    while (lexer.hasNext()) {
      Token actual = lexer.next();
      Assertions.assertEquals(expectedTokens.next().toString(), actual.toString());
      System.out.println(actual);
    }
  }
}
