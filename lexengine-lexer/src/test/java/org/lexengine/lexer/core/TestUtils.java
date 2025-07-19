/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.util.List;
import java.util.Map;

public class TestUtils {

  public static Nfa generateNfa(String specFile) {
    LexSpec lexSpec = generateLexSpec(specFile);
    return generateNfa(lexSpec);
  }

  public static Nfa generateNfa(LexSpec lexSpec) {
    List<RegexAction> regexActionList = lexSpec.regexActionList();
    DisjointIntSet languageAlphabets = new DisjointIntSet();
    LexUtils.extractAlphabetsFromRegex(regexActionList, languageAlphabets);
    Map<Range, Integer> alphabetsIndex =
        LexUtils.createAlphabetsIndex(languageAlphabets.ranges());
    NfaGenerator nfaGenerator =
        new NfaGenerator(regexActionList, languageAlphabets, alphabetsIndex);
    Nfa nfa = nfaGenerator.generate();
    assertNotNull(nfa);
    return nfa;
  }

  public static Dfa generateDfa(LexSpec lexSpec) {
    Nfa nfa = generateNfa(lexSpec);
    return new DfaGenerator(nfa).generate();
  }

  public static Dfa minimizeDfa(Dfa dfa) {
    return new DfaMinimizer(dfa).minimize();
  }

  public static LexSpec generateLexSpec(String specFile) {
    File testSpecFile = new File(TestUtils.class.getClassLoader().getResource(specFile).getFile());
    SpecParser parser = new SpecParser(testSpecFile);
    return parser.parseSpec();
  }
}
