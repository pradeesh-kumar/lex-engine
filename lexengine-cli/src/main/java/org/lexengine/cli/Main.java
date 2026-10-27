/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.cli;

import java.io.PrintStream;
import java.util.Arrays;

/** Single entry point for both generators and the precompiled feature examples. */
public final class Main {
  private Main() {}

  public static void main(String[] args) {
    int status = run(args, System.out, System.err);
    if (status != 0) System.exit(status);
  }

  public static int run(String[] args, PrintStream out, PrintStream err) {
    if (args.length == 0
        || (args.length == 1 && (args[0].equals("--help") || args[0].equals("-h")))) {
      usage(out);
      return 0;
    }
    if (args.length == 1 && args[0].equals("--version")) {
      String version = Main.class.getPackage().getImplementationVersion();
      out.println("Lex-Engine " + (version == null ? "development" : version));
      return 0;
    }
    String[] options = Arrays.copyOfRange(args, 1, args.length);
    return switch (args[0]) {
      case "parser" -> org.lexengine.parser.Main.run(options, out, err);
      case "lexer" ->
          org.lexengine.lexer.Main.run(
              options.length == 0 ? new String[] {"--help"} : options, out, err);
      case "examples", "example" -> org.lexengine.examples.Main.run(options, out, err);
      default -> {
        err.println("Unknown command: " + args[0] + ". Use lexengine --help.");
        yield 1;
      }
    };
  }

  private static void usage(PrintStream out) {
    out.println(
        """
        Lex-Engine — Java lexer and parser generators
        Usage: lexengine <command> [options]
          parser     Generate a Java parser from a .lg or .grammar file
          lexer      Generate a standalone DFA scanner from a .spec file
          examples   Run the compiled feature examples (--list or --all)
          --version  Print the distribution version
          --help     Print this help

        Examples:
          lexengine parser --grammar language.lg --parser-type LR1 --output-directory generated
          lexengine lexer --lex-spec-file tokens.spec --output-directory generated
          lexengine examples arithmetic "12 + 3 * (7 - 4)"
          lexengine examples json --input-file input.json
        """);
  }
}
