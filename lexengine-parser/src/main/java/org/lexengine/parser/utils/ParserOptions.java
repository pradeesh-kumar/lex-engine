package org.lexengine.parser.utils;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import org.lexengine.commons.logging.Out;
import org.lexengine.parser.core.ParserGenerator;

/**
 * Represents options for the Parser.
 *
 * <p>This class encapsulates various settings that can be configured through command-line
 * arguments.
 */
public class ParserOptions {

  private static final String DEFAULT_GRAMMAR_FILE = "program.grammar";
  private static final String DEFAULT_OUT_DIR = ".";
  private static final String DEFAULT_PARSER_CLASS_FILE = "parser-class.template";
  private static final ParserGenerator.ParserType DEFAULT_PARSER_TYPE =
      ParserGenerator.ParserType.RECURSIVE_DESCENT;

  /** Flag indicating whether to enable verbose mode. */
  public static boolean verbose;

  /** Output directory to save generated parser java source file. */
  public static String outDir;

  /** Path to the Grammar file. */
  public static File grammarFile;

  /** Parser class template file. */
  public static Path parserClassTemplate;

  /** Implementation Type of parser to generate. */
  public static ParserGenerator.ParserType parserType;

  /** Private constructor to prevent instantiation. */
  private ParserOptions() {}

  /**
   * Loads default values for all options.
   *
   * @see #verbose
   * @see #outDir
   * @see #grammarFile
   * @see #parserClassTemplate
   */
  public static void loadDefaults() {
    verbose = false;

    grammarFile =
        new File(
            Objects.requireNonNull(ParserOptions.class.getResource(DEFAULT_GRAMMAR_FILE))
                .getFile());
    outDir = DEFAULT_OUT_DIR;
    parserClassTemplate =
        Path.of(
            Objects.requireNonNull(ParserOptions.class.getResource(DEFAULT_PARSER_CLASS_FILE))
                .getFile());
    parserType = DEFAULT_PARSER_TYPE;
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
        case "-v" -> {
          verbose = true;
          Out.enableDebug();
        }
        case "-d", "--output-directory" -> outDir = getNextArg(args, ++i);
        case "-p", "--parser-type" ->
            parserType = ParserGenerator.ParserType.valueOf(getNextArg(args, ++i));
        default -> {
          Out.error("Unknown option: %s", args[i]);
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
