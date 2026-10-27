/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer;

import java.io.Reader;
import java.util.Arrays;
import org.lexengine.commons.Options;
import org.lexengine.commons.Resources;
import org.lexengine.lexer.core.LexerGenerator;

/** Generates a standalone lexer source file from a lexical specification. */
public class Main {
  public static void main(String[] args) {
    int status = run(args, System.out, System.err);
    if (status != 0) System.exit(status);
  }

  public static int run(String[] args, java.io.PrintStream out, java.io.PrintStream err) {
    if (Arrays.asList(args).contains("--help")) {
      out.println(
          "Usage: java org.lexengine.lexer.Main [--lex-spec-file <file>] [--output-directory <dir>] [--lexer-class <name>] [--lexer-package <package>] [--scanner-template <file>] [--verbose]");
      return 0;
    }
    try {
      Options options = Options.parseFromArgs(args);
      try (Reader reader = Resources.reader(options.lexSpecFile(), options.encoding())) {
        new LexerGenerator(options, reader).generate().writeToDirectory(options.outDir());
      }
      return 0;
    } catch (Exception ex) {
      err.println(ex.getMessage());
      return 1;
    }
  }
}
