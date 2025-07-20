/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.parser;

import org.lexengine.commons.logging.Out;
import org.lexengine.parser.utils.ParserOptions;

public class Main {

  public static void main(String[] args) {
    try {
      Out.printBanner();
      ParserOptions.loadDefaults();
      ParserOptions.overrideFromArgs(args);
      /*ParserGenerator parserGenerator = ParserGenerator.Factory.create(lexer, ParserOptions.grammarFile, ParserOptions.parserType);
      parserGenerator.generate();*/
    } catch (Exception e) {
      Out.error(e.getMessage());
      System.exit(1);
    }
  }
}
