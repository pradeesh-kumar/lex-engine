/*
 * Copyright (c) 2024 lex-engine
 * Author: Pradeesh Kumar
 */
package org.lexengine.lexer.core;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import org.lexengine.commons.logging.Out;
import org.lexengine.lexer.error.ErrorType;
import org.lexengine.lexer.error.GeneratorException;
import org.lexengine.lexer.util.LexerOptions;

/**
 * Generates a lexer based on a provided specification file.
 *
 * <p>The LexerGenerator reads the specification file, extracts the necessary information, generates
 * an NFA, converts it to a DFA, minimizes the DFA, and finally creates the lexer class.
 */
public class LexerGenerator {

  /** The file containing the lexer specification. */
  private final File lexerspecFile;

  /** A set of disjoint ranges representing the language alphabets. */
  private final DisjointIntSet languageAlphabets;

  /** The parsed lexer specification. */
  private LexSpec lexSpec;

  /**
   * Constructs a new LexerGenerator instance with the specified lexer specification file.
   *
   * @param lexerspecFile the file containing the lexer specification
   */
  public LexerGenerator(File lexerspecFile) {
    this.lexerspecFile = lexerspecFile;
    this.languageAlphabets = new DisjointIntSet();
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
  public void generate() {
    mkdirIfNotExists();
    this.lexSpec = new SpecParser(lexerspecFile).parse();
    LexUtils.extractAlphabetsFromRegex(lexSpec.regexActionList(), languageAlphabets);
    Out.debug("Language alphabets: " + languageAlphabets);
    Map<Range, Integer> alphabetIndex =
        LexUtils.createAlphabetsIndex(this.languageAlphabets.ranges());
    Nfa nfa =
        new NfaGenerator(lexSpec.regexActionList(), languageAlphabets, alphabetIndex).generate();
    Dfa dfa = new DfaGenerator(nfa).generate();
    dfa = new DfaMinimizer(dfa).minimize();
    LexClassGenerator lexClassGenerator =
        new TableBasedLexClassGenerator(
            dfa, lexSpec, Path.of(LexerOptions.outDir), LexerOptions.scannerClassTemplate);
    lexClassGenerator.generate();
  }

  /** Creates the output directory if it does not exist. */
  private void mkdirIfNotExists() {
    Path path = Path.of(LexerOptions.outDir);
    if (!path.toFile().exists()) {
      path.toFile().mkdirs();
    } else if (!path.toFile().isDirectory()) {
      Out.error("The path %s is not a directory", path.toAbsolutePath());
      throw GeneratorException.error(ErrorType.ERR_OUT_DIR_INVALID);
    }
  }
}
