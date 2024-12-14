/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import java.io.File;
import java.util.Map;
import org.lexengine.lexer.logging.Out;

public class LexerGenerator {

  private final File lexerspecFile;
  private final DisjointIntSet languageAlphabets;
  private Map<Interval, Integer> alphabetIndex;
  private LexSpec lexSpec;
  private Dfa dfa;

  public LexerGenerator(File lexerspecFile) {
    this.lexerspecFile = lexerspecFile;
    this.languageAlphabets = new DisjointIntSet();
  }

  public void generate() {
    this.lexSpec = new SpecParser(lexerspecFile).parseSpec();
    LexGenUtils.extractAlphabetsFromRegex(lexSpec.regexActionList(), languageAlphabets);
    Out.debug("Language alphabets: " + languageAlphabets);
    this.alphabetIndex = LexGenUtils.createAlphabetsIndex(this.languageAlphabets.intervals());
    Nfa nfa =
        new NfaGenerator(lexSpec.regexActionList(), languageAlphabets, alphabetIndex).generate();
    Dfa dfa = new DfaGenerator(nfa).generate();
    this.dfa = new DfaMinimizer(dfa).minimize();
  }
}
