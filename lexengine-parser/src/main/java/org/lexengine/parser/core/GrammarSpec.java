/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.parser.core;

import java.util.List;
import javax.lang.model.SourceVersion;

/**
 * Validated parser metadata and grammar; the fourth component is present for combined .lg files.
 */
public record GrammarSpec(
    String parserClassName, String parserPackageName, Grammar grammar, GrammarV2 definition) {
  public static final String DEFAULT_PARSER_CLASS_NAME = "Parser";
  public static final String DEFAULT_PARSER_PACKAGE_NAME = "org.lexengine.parser.generated";

  public GrammarSpec(String className, String packageName, Grammar grammar) {
    this(className, packageName, grammar, null);
  }

  public GrammarSpec {
    if (java.util.Set.of(
            "Lexer",
            "Token",
            "Node",
            "ParseTree",
            "ParserException",
            "ListLexer",
            "TextLexer",
            "Builder",
            "Work")
        .contains(parserClassName))
      throw new IllegalArgumentException(
          "Parser class name conflicts with generated runtime type: " + parserClassName);
    if (!SourceVersion.isIdentifier(parserClassName) || SourceVersion.isKeyword(parserClassName))
      throw new IllegalArgumentException("Invalid parser class " + parserClassName);
    if (!parserPackageName.isEmpty() && !SourceVersion.isName(parserPackageName))
      throw new IllegalArgumentException("Invalid parser package " + parserPackageName);
  }

  static Builder builder() {
    return new Builder();
  }

  static class Builder {
    private String parserClassName = DEFAULT_PARSER_CLASS_NAME;
    private String parserPackageName = DEFAULT_PARSER_PACKAGE_NAME;
    private Grammar.NonTerminal startSymbol;
    private final Grammar.ProductionMap productions = new Grammar.ProductionMap();

    Builder parserClassName(String value) {
      parserClassName = value;
      return this;
    }

    Builder parserPackageName(String value) {
      parserPackageName = value;
      return this;
    }

    Builder startSymbol(Grammar.NonTerminal value) {
      startSymbol = value;
      return this;
    }

    Builder addProduction(Grammar.NonTerminal nonTerminal, List<Grammar.Alternative> alternatives) {
      productions.add(Grammar.ProductionRule.create(nonTerminal, alternatives));
      if (startSymbol == null) startSymbol = nonTerminal;
      return this;
    }

    GrammarSpec build() {
      return new GrammarSpec(
          parserClassName, parserPackageName, new Grammar(productions, startSymbol));
    }
  }
}
