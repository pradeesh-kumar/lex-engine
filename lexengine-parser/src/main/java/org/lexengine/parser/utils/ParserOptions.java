/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.parser.utils;

import java.io.File;
import java.nio.file.Path;
import java.util.Locale;
import org.lexengine.commons.logging.Out;
import org.lexengine.parser.core.ParserGenerator;

/**
 * Command-line parser settings. Resources are read as streams by the generator, including in JARs.
 */
public final class ParserOptions {
  public static boolean verbose;
  public static String outDir;
  public static File grammarFile;
  public static Path parserClassTemplate;
  public static ParserGenerator.ParserType parserType;

  private ParserOptions() {}

  public static void loadDefaults() {
    verbose = false;
    outDir = ".";
    grammarFile = null;
    parserClassTemplate = null;
    parserType = ParserGenerator.ParserType.RECURSIVE_DESCENT;
  }

  public static void overrideFromArgs(String[] args) {
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "-v", "--verbose" -> {
          verbose = true;
          Out.enableDebug();
        }
        case "-d", "--output-directory" -> outDir = next(args, ++i);
        case "-g", "-sp", "--grammar" -> grammarFile = new File(next(args, ++i));
        case "-t", "--template" -> parserClassTemplate = Path.of(next(args, ++i));
        case "-p", "--parser-type" ->
            parserType =
                ParserGenerator.ParserType.valueOf(next(args, ++i).toUpperCase(Locale.ROOT));
        default -> throw new IllegalArgumentException("Unknown option: " + args[i]);
      }
    }
    if (grammarFile == null)
      throw new IllegalArgumentException("Specify a grammar with --grammar <file.grammar|file.lg>");
  }

  private static String next(String[] args, int index) {
    if (index >= args.length)
      throw new IllegalArgumentException("Missing value for " + args[index - 1]);
    return args[index];
  }
}
