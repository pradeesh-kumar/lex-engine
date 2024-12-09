/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.util;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Objects;
import org.lexengine.lexer.logging.Out;

/**
 * Represents command-line options for an application.
 *
 * <p>This class encapsulates various settings that can be configured through command-line
 * arguments.
 */
public class Options {

  private static final String DEFAULT_SPEC_FILE = "lexer-spec.spec";
  private static final String DEFAULT_OUT_DIR = ".";

  /** Flag indicating whether to enable verbose mode. */
  public static boolean verbose;

  /** Character encoding used by the application. */
  public static Charset encoding;

  /** Output directory to save generated scanner java source file. */
  public static String outDir;

  /** Path to the lexerSpecFile file used by the application. */
  public static File lexerSpecFile;

  /** Private constructor to prevent instantiation. */
  private Options() {}

  /**
   * Loads default values for all options.
   *
   * @see #verbose
   * @see #encoding
   * @see #lexerSpecFile
   */
  public static void loadDefaults() {
    verbose = false;
    encoding = Charset.defaultCharset();
    lexerSpecFile =
        new File(Objects.requireNonNull(Options.class.getResource(DEFAULT_SPEC_FILE)).getFile());
    outDir = DEFAULT_OUT_DIR;
  }

  /**
   * Parses command-line arguments and updates option values accordingly.
   *
   * @param args array of command-line arguments
   */
  public static void loadFromArgs(String[] args) {
    int i = 0;
    while (i < args.length) {
      if (args[i].equals("-v")) {
        // Set verbose to true when -v flag is encountered
        verbose = true;
        i++;
      } else if (args[i].equals("-d")) {
        // Check if there's a directory path after the -d flag
        if (i + 1 < args.length) {
          outDir = args[++i];
        } else {
          Out.error("No output directory specified after -d flag.");
          System.exit(1);
        }
      } else {
        Out.error("Unknown option: %x", args[i]);
        System.exit(1);
      }
    }
  }
}
