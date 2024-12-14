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
    File testSpecFile = new File(TestUtils.class.getClassLoader().getResource(specFile).getFile());
    SpecParser parser = new SpecParser(testSpecFile);
    LexSpec lexSpec = parser.parseSpec();
    List<RegexAction> regexActionList = lexSpec.regexActionList();
    DisjointIntSet languageAlphabets = new DisjointIntSet();
    LexGenUtils.extractAlphabetsFromRegex(regexActionList, languageAlphabets);
    Map<Interval, Integer> alphabetsIndex =
        LexGenUtils.createAlphabetsIndex(languageAlphabets.intervals());
    NfaGenerator nfaGenerator =
        new NfaGenerator(regexActionList, languageAlphabets, alphabetsIndex);
    Nfa nfa = nfaGenerator.generate();
    assertNotNull(nfa);
    return nfa;
  }
}
