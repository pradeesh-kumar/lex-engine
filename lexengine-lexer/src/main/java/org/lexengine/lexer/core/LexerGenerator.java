/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    generateLanguageAlphabets();
    Nfa nfa =
        new NfaGenerator(lexSpec.regexActionList(), languageAlphabets, alphabetIndex).generate();
    Dfa dfa = new DfaGenerator(nfa).generate();
    this.dfa = new DfaMinimizer(dfa).minimize();
  }

  private void generateLanguageAlphabets() {
    this.lexSpec.regexActionList().stream()
        .map(RegexAction::regex)
        .map(Regex::extractAlphabets)
        .flatMap(List::stream)
        .forEach(languageAlphabets::add);
    var intervals = this.languageAlphabets.intervals();
    this.alphabetIndex = new HashMap<>(intervals.size());
    int i = 1;
    for (var interval : intervals) {
      this.alphabetIndex.put(interval, i++);
    }
  }
}
