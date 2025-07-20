/*
* Copyright (c) 2025 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.parser.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record GrammarSpec(String parserClassName, String parserPackageName, Grammar grammar) {

  /** Default values for the metadata properties */
  public static final String DEFAULT_PARSER_CLASS_NAME = "Parser";

  public static final String DEFAULT_PARSER_PACKAGE_NAME = "org.lexengine.parser.generated";

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String parserClassName = DEFAULT_PARSER_CLASS_NAME;
    private String parserPackageName = DEFAULT_PARSER_PACKAGE_NAME;
    private Map<Grammar.NonTerminal, List<List<Grammar.Symbol>>> productions;

    public Builder() {}

    public Builder parserClassName(String parserClassName) {
      this.parserClassName = parserClassName;
      return this;
    }

    public Builder parserPackageName(String parserPackageName) {
      this.parserPackageName = parserPackageName;
      return this;
    }

    public Builder addProduction(
        Grammar.NonTerminal nonTerminal, List<List<Grammar.Symbol>> rules) {
      if (productions == null) {
        productions = new HashMap<>();
      }
      productions.put(nonTerminal, rules);
      return this;
    }

    public Builder addAllProductions(Map<Grammar.NonTerminal, List<List<Grammar.Symbol>>> productions) {
      if (productions == null) {
        productions = new HashMap<>();
      }
      this.productions.putAll(productions);
      return this;
    }

    public GrammarSpec build() {
      return new GrammarSpec(parserClassName, parserPackageName, new Grammar(productions));
    }
  }
}

/**
 * Grammar consists set of productions. A Symbol can be either Terminal or NonTerminal
 *
 * <p>Example:
 *
 * <p>EXPR -> EXPR + TERM | EXPR - TERM | TERM TERM -> TERM * FACTOR | TERM / FACTOR | FACTOR FACTOR
 * -> ident | num | (EXPR)
 *
 * @param productions
 */
record Grammar(Map<NonTerminal, List<List<Symbol>>> productions) {

  abstract static sealed class Symbol permits NonTerminal, Terminal {
    protected String name;

    Symbol(String name) {
      this.name = name;
    }

    public String name() {
      return name;
    }

    public static Symbol parse(String name) {
      return name.matches("[A-Z]+") ? NonTerminal.of(name) : Terminal.of(name);
    }

    @Override
    public String toString() {
      return name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Symbol that = (Symbol) o;
      return name.equals(that.name);
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }
  }

  static final class NonTerminal extends Symbol {

    private NonTerminal(String name) {
      super(name);
      if (!name.matches("[A-Z]+'?")) {
        throw new IllegalArgumentException(
            "Invalid nonterminal: " + name + ". NonTerminals must be strictly uppercase word");
      }
    }

    static NonTerminal of(String name) {
      return new NonTerminal(name);
    }

    @Override
    public String toString() {
      return super.toString();
    }

    @Override
    public boolean equals(Object o) {
      return super.equals(o);
    }

    @Override
    public int hashCode() {
      return super.hashCode();
    }

  }

  static final class Terminal extends Symbol {
    private Terminal(String name) {
      super(name);
      if (name.matches("[A-Z]+'?")) {
        throw new IllegalArgumentException(
            "Invalid terminal name " + name + " Terminal name cannot start");
      }
    }

    static Terminal of(String name) {
      return new Terminal(name);
    }

    @Override
    public String toString() {
      return super.toString();
    }

    @Override
    public boolean equals(Object o) {
      return super.equals(o);
    }

    @Override
    public int hashCode() {
      return super.hashCode();
    }
  }
}
