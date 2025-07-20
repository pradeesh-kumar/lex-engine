/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.util;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import org.lexengine.commons.logging.Out;

/**
 * Represents options for the Lexer.
 *
 * <p>This class encapsulates various settings that can be configured through command-line
 * arguments.
 */
public class LexerOptions {

  private static final String DEFAULT_SPEC_FILE = "lexer-spec.spec";
  private static final String DEFAULT_OUT_DIR = ".";
  private static final String DEFAULT_SCANNER_CLASS_FILE = "scanner-class.template";

  /** Flag indicating whether to enable verbose mode. */
  public static boolean verbose;

  /** Character encoding used by the application. */
  public static Charset encoding;

  /** Output directory to save generated scanner java source file. */
  public static String outDir;

  /** Path to the lexerSpecFile file. */
  public static File lexerSpecFile;

  /** Scanner class template file. */
  public static Path scannerClassTemplate;

  /** Private constructor to prevent instantiation. */
  private LexerOptions() {}

  /**
   * Loads default values for all options.
   *
   * @see #verbose
   * @see #outDir
   * @see #encoding
   * @see #lexerSpecFile
   */
  public static void loadDefaults() {
    verbose = false;
    encoding = Charset.defaultCharset();
    lexerSpecFile =
        new File(
            Objects.requireNonNull(
                    Thread.currentThread().getContextClassLoader().getResource(DEFAULT_SPEC_FILE))
                .getFile());
    outDir = DEFAULT_OUT_DIR;
    scannerClassTemplate =
        Path.of(
            Objects.requireNonNull(
                    Thread.currentThread()
                        .getContextClassLoader()
                        .getResource(DEFAULT_SCANNER_CLASS_FILE))
                .getFile());
  }

  /**
   * Parses command-line arguments and updates option values accordingly.
   *
   * @param args array of command-line arguments
   */
  public static void overrideFromArgs(String[] args) {
    int i = 0;
    while (i < args.length) {
      switch (args[i]) {
        case "-v", "--verbose" -> {
          verbose = true;
          Out.enableDebug();
        }
        case "-d", "--output-directory" -> outDir = getNextArg(args, ++i);
        case "-sp", "--spec" -> lexerSpecFile = new File(getNextArg(args, ++i));
        case "-sc", "--scanner-class-file" ->
            scannerClassTemplate = Paths.get(getNextArg(args, ++i));
        default -> {
          Out.error("Unknown option: %x", args[i]);
          System.exit(1);
        }
      }
      i++;
    }
  }

  /**
   * Retrieves the next command-line argument from the given array.
   *
   * <p>This method checks if there is a next argument at the specified index. If so, it returns the
   * argument. Otherwise, it logs an error message indicating that no argument was specified after
   * the previous option and exits the program with a non-zero status code.
   *
   * @param args the array of command-line arguments
   * @param i the index of the next argument to retrieve
   * @return the next command-line argument, or null (though the method never returns due to
   *     System.exit)
   */
  private static String getNextArg(String[] args, int i) {
    if (i < args.length) {
      return args[i];
    } else {
      Out.error("No argument specified after the option %s", args[i - 1]);
      System.exit(1);
      return null;
    }
  }
}
