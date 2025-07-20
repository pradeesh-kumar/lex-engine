/*
 * Copyright (c) 2024 lex-engine
 * Author: Pradeesh Kumar
 */
package org.lexengine.lexer.core;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class LexClassGeneratorTest {

  @Test
  public void generateLexerClass() {
    LexSpec spec = TestUtils.generateLexSpec("lexer-spec-class-gen.spec");
    Dfa dfa = TestUtils.minimizeDfa(TestUtils.generateDfa(spec));
    Path templatePath =
        Path.of(TestUtils.class.getClassLoader().getResource("scanner-class.template").getFile());
    Path outputPath = templatePath.getParent();
    TableBasedLexClassGenerator lcg =
        new TableBasedLexClassGenerator(dfa, spec, outputPath, templatePath);
    lcg.generate();
  }
}
