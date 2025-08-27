package org.lexengine.parser.core;

import org.lexengine.commons.error.ErrorType;
import org.lexengine.commons.error.GeneratorException;
import org.lexengine.commons.logging.Out;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
public record Grammar(ProductionMap productions, NonTerminal startSymbol) {

  public Grammar {
    Objects.requireNonNull(startSymbol, "Start symbol cannot be null");
    Objects.requireNonNull(productions, "productions cannot be null");
    if (productions.isEmpty()) {
      throw GeneratorException.error(ErrorType.ERR_GRAMMAR_FILE_EMPTY_PRODUCTION);
    }
    productions.validate();
  }

  public record ProductionRule(int index, NonTerminal lhs, List<Alternative> alternatives) {
    private static int idCounter = 0;

    public static ProductionRule create(NonTerminal lhs, List<Alternative> alternatives) {
      Objects.requireNonNull(lhs, "lhs");
      Objects.requireNonNull(alternatives, "alternatives");
      return new ProductionRule(idCounter++, lhs, alternatives);
    }

  }

  public record Alternative(List<Symbol> symbols) {

    public static Alternative create() {
      return new Alternative(List.of());
    }

    public static Alternative create(List<Symbol> symbols) {
      return new Alternative(symbols);
    }

    public boolean isEmpty() {
      return symbols.isEmpty();
    }

    public int  size() {
      return symbols.size();
    }

    public Symbol first() {
      return symbols.getFirst();
    }

    public Symbol get(int index) {
      return symbols.get(index);
    }
  }

  public static class ProductionMap {

    private final Map<NonTerminal, ProductionRule> rules;

    public ProductionMap() {
      this.rules = new HashMap<>();
    }

    public boolean isEmpty() {
      return rules.isEmpty();
    }

    public int size() {
      return rules.size();
    }

    public Set<NonTerminal> nonTerminals() {
      return rules.keySet();
    }

    public Collection<ProductionRule> rules() {
      return rules.values();
    }

    public boolean containsNonTerminal(NonTerminal nonTerminal) {
      return rules.containsKey(nonTerminal);
    }

    protected void validate() {
      Set<NonTerminal> keys = nonTerminals();
      Set<NonTerminal> invalidNonTerminals = this.rules.values().stream()
          .map(ProductionRule::alternatives)
          .flatMap(List::stream)
          .flatMap(a -> a.symbols.stream())
          .filter(symbol -> symbol instanceof Grammar.NonTerminal)
          .map(symbol -> (Grammar.NonTerminal) symbol)
          .filter(Predicate.not(keys::contains))
          .collect(Collectors.toSet());
      if (!invalidNonTerminals.isEmpty()) {
        Out.error("Invalid non-terminals! Production rule not found for these non-terminal symbols: %s", invalidNonTerminals);
        throw GeneratorException.error(ErrorType.ERR_GRAMMAR_PRODUCTION_INVALID);
      }
    }

    protected void add(ProductionRule rule) {
      rules.put(rule.lhs(), rule);
    }

    public void addAll(List<ProductionRule> productionRules) {
      productionRules.forEach(this::add);
    }

    public ProductionRule get(NonTerminal key) {
      return rules.get(key);
    }
  }

  public abstract static sealed class Symbol permits NonTerminal, Terminal {
    protected String name;
    protected String methodName;

    Symbol(String name) {
      this.name = name;
      this.methodName = name.toLowerCase().replace("'", "Prime");
    }

    public String name() {
      return name;
    }

    public String methodName() {
      return this.methodName;
    }

    public String className() {
      return Character.toUpperCase(this.methodName.charAt(0)) + this.methodName.substring(1);
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

  public static final class NonTerminal extends Symbol {

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

  public static final class Terminal extends Symbol {
    private Terminal(String name) {
      super(name);
      if (name.matches("[A-Z]+'?")) {
        throw new IllegalArgumentException(
            "Invalid terminal name " + name + " Terminal name cannot start");
      }
    }

    public static Terminal of(String name) {
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
