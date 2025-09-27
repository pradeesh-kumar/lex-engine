/*
* Copyright (c) 2025 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.lexengine.commons.Options;

public class LexClassGeneratorTest {

  @Test
  public void generateLexerClass() {
    List<LexRule> lexRules = TestUtils.generateLexSpec("lexer-spec-class-gen.spec");
    Dfa dfa = TestUtils.minimizeDfa(TestUtils.generateDfa(lexRules));
    Path templatePath =
        Path.of(TestUtils.class.getClassLoader().getResource("scanner-class.tpl").getFile());
    TableBasedLexClassGenerator lcg =
        new TableBasedLexClassGenerator(dfa, Options.defaultOptions());
    lcg.generate();
  }
}
