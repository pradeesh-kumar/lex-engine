/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.lexengine.commons.error.GeneratorException;

public class SpecParserTest {

  @Test
  public void testParseValidFile() {
    File testSpecFile =
        new File(getClass().getClassLoader().getResource("lexer-spec.spec").getFile());
    SpecParser parser = new SpecParser(testSpecFile);
    LexSpec lexSpec = parser.parse();

    assertEquals(16, lexSpec.regexActionList().size());
    assertEquals("MyLexer", lexSpec.lexClassName());
    assertEquals("org.lexengine.lexer.generated", lexSpec.lexPackageName());
  }

  @Test
  public void testParseEmptyFile() {
    File tempFile = createTempSpecFile("");
    assertThrows(
        GeneratorException.class,
        () -> {
          SpecParser parser = new SpecParser(tempFile);
          parser.parse();
        });
    deleteTempFile(tempFile);
  }

  @Test
  public void testParseInvalidPropertyLine() {
    File tempFile = createTempSpecFile("invalid_property\n");
    assertThrows(
        GeneratorException.class,
        () -> {
          SpecParser parser = new SpecParser(tempFile);
          parser.parse();
        });
    deleteTempFile(tempFile);
  }

  @Test
  public void testParseInvalidRegexLine() {
    File tempFile = createTempSpecFile("---\n", "invalid_regex\n");
    assertThrows(
        GeneratorException.class,
        () -> {
          SpecParser parser = new SpecParser(tempFile);
          parser.parse();
        });
    deleteTempFile(tempFile);
  }

  @Test
  public void testParseMissingDivider() {
    File tempFile = createTempSpecFile("\"hello\" world\n");
    assertThrows(
        GeneratorException.class,
        () -> {
          SpecParser parser = new SpecParser(tempFile);
          parser.parse();
        });
    deleteTempFile(tempFile);
  }

  @Test
  public void testParseNoEntriesFound() {
    File tempFile =
        createTempSpecFile("# This is a comment\n", "class=MyClass\n", "package=com.example\n");
    assertThrows(
        GeneratorException.class,
        () -> {
          SpecParser parser = new SpecParser(tempFile);
          parser.parse();
        });
    deleteTempFile(tempFile);
  }

  private File createTempSpecFile(String... content) {
    try {
      File tempFile = File.createTempFile("temp-spec-file", ".txt");
      FileWriter writer = new FileWriter(tempFile);
      for (String line : content) {
        writer.write(line + "\n");
      }
      writer.close();
      return tempFile;
    } catch (IOException e) {
      fail(e.getMessage());
      return null; // unreachable
    }
  }

  private void deleteTempFile(File tempFile) {
    assertTrue(tempFile.delete());
  }
}
