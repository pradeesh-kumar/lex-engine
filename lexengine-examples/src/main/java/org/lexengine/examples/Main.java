/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.examples;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Runs examples whose parsers and scanner were generated and compiled by Maven. */
public final class Main {
  private Main() {}

  public static void main(String[] args) {
    int status = run(args, System.out, System.err);
    if (status != 0) System.exit(status);
  }

  public static int run(String[] args, PrintStream out, PrintStream err) {
    try {
      if (args.length == 0
          || (args.length == 1 && (args[0].equals("--help") || args[0].equals("--list")))) {
        usage(out);
      } else if (args[0].equals("--all")) {
        if (args.length != 1)
          throw new IllegalArgumentException("--all does not accept additional arguments");
        for (var example : ExampleCatalog.examples()) print(example, example.sample(), out);
      } else {
        var example = ExampleCatalog.find(args[0]);
        String input = example.sample();
        if (args.length == 2) {
          if (args[1].equals("--input-file"))
            throw new IllegalArgumentException("Missing path after --input-file");
          input = args[1];
        } else if (args.length == 3 && args[1].equals("--input-file"))
          input = Files.readString(Path.of(args[2]));
        else if (args.length > 1)
          throw new IllegalArgumentException("Supply one input string or --input-file <path>");
        print(example, input, out);
      }
      return 0;
    } catch (Exception ex) {
      err.println(ex.getMessage());
      return 1;
    }
  }

  private static void print(ExampleCatalog.Example example, String input, PrintStream out) {
    String tree = example.render().apply(input);
    out.println("Accepted " + example.name() + " using " + example.algorithm());
    out.print(tree);
  }

  private static void usage(PrintStream out) {
    out.println("Usage: lexengine examples <name> [input | --input-file <path>]");
    out.println("       lexengine examples --all | --list");
    for (var example : ExampleCatalog.examples())
      out.printf("  %-12s %-18s %s%n", example.name(), example.algorithm(), example.description());
  }
}
