/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.logging;

import java.io.PrintWriter;
import org.lexengine.lexer.util.Options;

/** A utility class providing methods for logging messages at different levels. */
public class Out {

  private static final String NEWLINE = System.lineSeparator();

  public static final String VERSION = "v1.0.0";

  private Out() {}

  private static final PrintWriter out = new PrintWriter(System.out, true);

  public static void info(String msg) {
    write(Level.INFO, msg);
  }

  public static void info(String msg, Object... args) {
    write(Level.INFO, msg, args);
  }

  public static void debug(String msg) {
    if (Options.verbose) write(Level.DEBUG, msg);
  }

  public static void debug(String msg, Object... args) {
    if (Options.verbose) write(Level.DEBUG, msg, args);
  }

  public static void warn(String msg) {
    write(Level.WARN, msg);
  }

  public static void error(String msg) {
    write(Level.ERROR, msg);
  }

  public static void error(String msg, Object... args) {
    write(Level.ERROR, msg, args);
  }

  private static void write(Level level, String msg) {
    out.write(String.format("%s: %s%s", level, msg, NEWLINE));
  }

  private static void write(Level level, String msg, Object... args) {
    out.format(String.format("%s: %s%s", level, msg, NEWLINE), args);
  }

  public static void printBanner() {
    out.write("Pradeesh Compiler Lexer Generator version: " + VERSION);
  }

  enum Level {
    INFO,
    DEBUG,
    WARN,
    ERROR;
  }
}
