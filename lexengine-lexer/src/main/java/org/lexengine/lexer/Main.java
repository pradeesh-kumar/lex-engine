/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer;

import org.lexengine.commons.logging.Out;
import org.lexengine.lexer.core.LexerGenerator;
import org.lexengine.lexer.util.LexerOptions;

/** The Main class serves as the entry point for the application. */
public class Main {

  public static void main(String[] args) {
    try {
      Out.printBanner();
      LexerOptions.loadDefaults();
      LexerOptions.overrideFromArgs(args);
      LexerGenerator lexerGenerator = new LexerGenerator(LexerOptions.lexerSpecFile);
      lexerGenerator.generate();
    } catch (Exception e) {
      Out.error(e.getMessage());
      System.exit(1);
    }
  }
}
