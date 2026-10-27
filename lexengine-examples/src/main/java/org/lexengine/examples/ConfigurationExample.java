/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.examples;

import org.lexengine.examples.generated.ConfigParser;

/** A typed example backed by Java source generated and compiled during the Maven build. */
public final class ConfigurationExample {
  private ConfigurationExample() {}

  public static ConfigParser.ParseTree parse(String input) {
    return new ConfigParser(input).parse();
  }

  public static String format(String input) {
    return TreePrinter.format(
        parse(input).root(),
        ConfigParser.Node::symbol,
        ConfigParser.Node::label,
        ConfigParser.Node::value,
        ConfigParser.Node::children);
  }
}
