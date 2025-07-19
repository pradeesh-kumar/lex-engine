/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer;

import org.lexengine.lexer.core.LexerGenerator;
import org.lexengine.lexer.logging.Out;
import org.lexengine.lexer.util.Options;

/** The Main class serves as the entry point for the application. */
public class Main {

  public static void main(String[] args) {
    Out.printBanner();
    Options.loadDefaults();
    Options.overrideFromArgs(args);
    try {
      LexerGenerator lexerGenerator = new LexerGenerator(Options.lexerSpecFile);
      lexerGenerator.generate();
    } catch (Exception e) {
      Out.error(e.getMessage());
      System.exit(1);
    }
  }
}
