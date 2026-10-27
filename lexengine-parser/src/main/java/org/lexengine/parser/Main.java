/*
* Copyright (c) 2025 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.parser;

import org.lexengine.commons.logging.Out;
import org.lexengine.parser.core.ParserGenerator;
import org.lexengine.parser.utils.ParserOptions;

public class Main {

  public static void main(String[] args) {
    int status = run(args, System.out, System.err);
    if (status != 0) System.exit(status);
  }

  public static int run(String[] args, java.io.PrintStream out, java.io.PrintStream err) {
    try {
      if (args.length == 0 || java.util.Arrays.asList(args).contains("--help")) {
        out.println(
            "Usage: java org.lexengine.parser.Main --grammar <file> [--output-directory <dir>] [--parser-type RECURSIVE_DESCENT|LL1|LR1] [--template <file>] [--verbose]");
        return 0;
      }
      Out.printBanner();
      ParserOptions.loadDefaults();
      ParserOptions.overrideFromArgs(args);
      ParserGenerator parserGenerator =
          ParserGenerator.Factory.create(ParserOptions.grammarFile, ParserOptions.parserType);
      parserGenerator.generate();
      out.println("Generated " + parserGenerator.details().sourceFile());
      return 0;
    } catch (Exception e) {
      err.println(e.getMessage());
      return 1;
    }
  }
}
