/*
* Copyright (c) 2025 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import java.io.Reader;
import java.util.List;
import java.util.Map;

import org.lexengine.commons.Options;
import org.lexengine.commons.logging.Out;

/**
 * Generates a lexer based on a provided specification file.
 *
 * <p>The LexerGenerator reads the specification file, extracts the necessary information, generates
 * an NFA, converts it to a DFA, minimizes the DFA, and finally creates the lexer class.
 */
public class LexerGenerator {

  /** A set of disjoint ranges representing the language alphabets. */
  private final DisjointIntSet languageAlphabets;

  private Options options;

  private Reader specReader;

  private List<LexRule> lexRules;

  public LexerGenerator(Options options, Reader specReader) {
    this.options = options;
    this.specReader = specReader;
    this.languageAlphabets = new DisjointIntSet();
  }

  public LexerGenerator(Options options, List<LexRule> lexRules) {
    this.options = options;
    this.languageAlphabets = new DisjointIntSet();
    this.lexRules = lexRules;
  }

  /**
   * Generates the lexer based on the provided specification file.
   *
   * <p>This method performs the following steps:
   *
   * <ul>
   *   <li>Parses the lexer specification file
   *   <li>Extracts the language alphabets from the regular expressions
   *   <li>Creates an index of the language alphabets
   *   <li>Generates an NFA from the regular expressions
   *   <li>Converts the NFA to a DFA
   *   <li>Minimizes the DFA
   *   <li>Generates the lexer class
   * </ul>
   */
  public LexerOut generate() {
    if (lexRules == null) {
      this.lexRules = new SpecParser(this.specReader).parse();
    }
    LexUtils.extractAlphabetsFromRegex(lexRules, languageAlphabets);
    Out.debug("Language alphabets: " + languageAlphabets);
    Map<Range, Integer> alphabetIndex = LexUtils.createAlphabetsIndex(this.languageAlphabets.ranges());
    Nfa nfa = new NfaGenerator(lexRules, languageAlphabets, alphabetIndex).generate();
    Dfa dfa = new DfaGenerator(nfa).generate();
    dfa = new DfaMinimizer(dfa).minimize();
    LexClassGenerator lexClassGenerator = new TableBasedLexClassGenerator(dfa, options);
    Reader lexerClassReader = lexClassGenerator.generate();
    return new LexerOut(lexerClassReader, options.lexerClassName(), options.lexerPackageName());
  }

}
