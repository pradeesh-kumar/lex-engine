/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;

class MainTest {
  @Test
  void helpAndVersionWorkWithoutExitingTheCallingVm() {
    var output = new ByteArrayOutputStream();
    var errors = new ByteArrayOutputStream();
    var out = new PrintStream(output);
    var err = new PrintStream(errors);
    assertEquals(0, Main.run(new String[] {}, out, err));
    assertTrue(output.toString().contains("parser"));
    assertEquals(0, Main.run(new String[] {"--version"}, out, err));
    assertTrue(output.toString().contains("Lex-Engine"));
    assertEquals(0, Main.run(new String[] {"parser", "--help"}, out, err));
    assertEquals(0, Main.run(new String[] {"lexer"}, out, err));
    assertEquals(0, Main.run(new String[] {"examples", "--list"}, out, err));
    assertTrue(errors.toString().isEmpty());
  }

  @Test
  void invalidCommandsAndGeneratorOptionsReturnFailures() {
    var output = new ByteArrayOutputStream();
    var errors = new ByteArrayOutputStream();
    var out = new PrintStream(output);
    var err = new PrintStream(errors);
    assertEquals(1, Main.run(new String[] {"missing"}, out, err));
    assertEquals(1, Main.run(new String[] {"parser", "--unknown"}, out, err));
    assertEquals(1, Main.run(new String[] {"lexer", "--unknown"}, out, err));
    assertTrue(errors.toString().contains("Unknown command"));
    assertTrue(errors.toString().contains("Unknown option"));
  }
}
