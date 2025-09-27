/*
* Copyright (c) 2025 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer;

import org.lexengine.commons.Options;
import org.lexengine.commons.logging.Out;
import org.lexengine.lexer.core.LexerGenerator;

import java.io.FileReader;
import java.io.Reader;

/** The Main class serves as the entry point for the application. */
public class Main {

  public static void main(String[] args) {
    try {
      Out.printBanner();
      var options = Options.parseFromArgs(args);
      Reader reader = new FileReader(String.valueOf(Thread.currentThread().getContextClassLoader().getResource(options.lexSpecFile())));
      LexerGenerator lexerGenerator = new LexerGenerator(options, reader);
      lexerGenerator.generate();
    } catch (Exception e) {
      Out.error(e.getMessage());
      System.exit(1);
    }
  }
}
