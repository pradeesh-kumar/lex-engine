/*
* Copyright (c) 2025 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestUtils {

  public static Nfa generateNfa(String specFile) {
    return generateNfa(generateLexSpec(specFile));
  }

  public static Nfa generateNfa(List<LexRule> lexRules) {
    DisjointIntSet languageAlphabets = new DisjointIntSet();
    LexUtils.extractAlphabetsFromRegex(lexRules, languageAlphabets);
    Map<Range, Integer> alphabetsIndex = LexUtils.createAlphabetsIndex(languageAlphabets.ranges());
    NfaGenerator nfaGenerator =
        new NfaGenerator(lexRules, languageAlphabets, alphabetsIndex);
    Nfa nfa = nfaGenerator.generate();
    assertNotNull(nfa);
    return nfa;
  }

  public static Dfa generateDfa(List<LexRule> lexRules) {
    Nfa nfa = generateNfa(lexRules);
    return new DfaGenerator(nfa).generate();
  }

  public static Dfa minimizeDfa(Dfa dfa) {
    return new DfaMinimizer(dfa).minimize();
  }

  public static List<LexRule> generateLexSpec(String specFile) {
    File testSpecFile = new File(TestUtils.class.getClassLoader().getResource(specFile).getFile());
    try {
      SpecParser parser = new SpecParser(new FileReader(testSpecFile));
      return parser.parse();
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}
