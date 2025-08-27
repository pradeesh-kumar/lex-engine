/*
* Copyright (c) 2025 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.parser.core;

import java.util.List;

public record GrammarSpec(String parserClassName, String parserPackageName, Grammar grammar) {

  /** Default values for the metadata properties */
  public static final String DEFAULT_PARSER_CLASS_NAME = "Parser";

  public static final String DEFAULT_PARSER_PACKAGE_NAME = "org.lexengine.parser.generated";

  static Builder builder() {
    return new Builder();
  }

  static class Builder {
    private String parserClassName = DEFAULT_PARSER_CLASS_NAME;
    private String parserPackageName = DEFAULT_PARSER_PACKAGE_NAME;
    private Grammar.NonTerminal startSymbol;
    private final Grammar.ProductionMap productions = new Grammar.ProductionMap();

    Builder() {}

    Builder parserClassName(String parserClassName) {
      this.parserClassName = parserClassName;
      return this;
    }

    Builder parserPackageName(String parserPackageName) {
      this.parserPackageName = parserPackageName;
      return this;
    }

    Builder startSymbol(Grammar.NonTerminal startSymbol) {
      this.startSymbol = startSymbol;
      return this;
    }

    Builder addProduction(
        Grammar.NonTerminal nonTerminal, List<Grammar.Alternative> alternatives) {
      productions.add(Grammar.ProductionRule.create(nonTerminal, alternatives));
      if (startSymbol == null) {
        startSymbol = nonTerminal;
      }
      return this;
    }

    GrammarSpec build() {
      return new GrammarSpec(parserClassName, parserPackageName, new Grammar(productions, startSymbol));
    }
  }
}

