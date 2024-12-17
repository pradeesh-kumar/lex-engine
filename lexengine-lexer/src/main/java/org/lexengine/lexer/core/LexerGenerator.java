/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

import org.lexengine.lexer.error.ErrorType;
import org.lexengine.lexer.error.GeneratorException;
import org.lexengine.lexer.logging.Out;
import org.lexengine.lexer.util.Options;

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
    mkdirIfNotExists();
    this.lexSpec = new SpecParser(lexerspecFile).parseSpec();
    LexGenUtils.extractAlphabetsFromRegex(lexSpec.regexActionList(), languageAlphabets);
    Out.debug("Language alphabets: " + languageAlphabets);
    this.alphabetIndex = LexGenUtils.createAlphabetsIndex(this.languageAlphabets.intervals());
    Nfa nfa = new NfaGenerator(lexSpec.regexActionList(), languageAlphabets, alphabetIndex).generate();
    Dfa dfa = new DfaGenerator(nfa).generate();
    this.dfa = new DfaMinimizer(dfa).minimize();
    LexClassGenerator lexClassGenerator = new TableBasedLexClassGenerator(dfa, lexSpec, Path.of(Options.outDir), Options.scannerClassTemplate);
    lexClassGenerator.generate();
  }

  private void mkdirIfNotExists() {
    Path path = Path.of(Options.outDir);
    if (!path.toFile().exists()) {
      path.toFile().mkdirs();
    } else if (!path.toFile().isDirectory()) {
      Out.error("The path %s is not a directory", path.toAbsolutePath());
      throw GeneratorException.error(ErrorType.ERR_OUT_DIR_INVALID);
    }
  }
}
