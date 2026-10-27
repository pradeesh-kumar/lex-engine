/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.parser.core;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.lexengine.commons.error.ErrorType;
import org.lexengine.commons.error.GeneratorException;

/** A context-free grammar. Epsilon is represented by an empty alternative. */
public record Grammar(ProductionMap productions, NonTerminal startSymbol) {
  public Grammar {
    Objects.requireNonNull(productions, "productions");
    if (productions.isEmpty()) {
      throw GeneratorException.create(
          ErrorType.ERR_GRAMMAR_FILE_EMPTY_PRODUCTION, "Productions cannot be empty");
    }
    Objects.requireNonNull(startSymbol, "startSymbol");
    if (!productions.containsNonTerminal(startSymbol)) {
      throw GeneratorException.create(
          ErrorType.ERR_GRAMMAR_PRODUCTION_INVALID,
          "Start symbol %s has no production",
          startSymbol);
    }
    productions.validate();
    productions.startSymbol = startSymbol;
    productions.invalidate();
  }

  public record ProductionRule(int index, NonTerminal lhs, List<Alternative> alternatives) {
    private static final AtomicInteger NEXT_ID = new AtomicInteger();

    public ProductionRule {
      Objects.requireNonNull(lhs, "lhs");
      alternatives = List.copyOf(alternatives);
      if (alternatives.isEmpty())
        throw new IllegalArgumentException("A production needs an alternative");
    }

    public static ProductionRule create(NonTerminal lhs, List<Alternative> alternatives) {
      return new ProductionRule(NEXT_ID.getAndIncrement(), lhs, alternatives);
    }
  }

  public record Alternative(List<Symbol> symbols, String label) {
    public Alternative {
      symbols = List.copyOf(symbols);
      label = label == null ? "DefaultLabel" : label;
      long epsilons =
          symbols.stream().filter(s -> s instanceof Terminal t && t.isEpsilon()).count();
      if (epsilons > 0) {
        if (symbols.size() != 1)
          throw GeneratorException.create(
              ErrorType.ERR_PARSER_MULTI_EPSILON,
              "Epsilon must be the only symbol in an alternative");
        symbols = List.of();
      }
    }

    public static Alternative create() {
      return create(List.of());
    }

    public static Alternative create(List<Symbol> symbols) {
      return create(symbols, null);
    }

    public static Alternative create(List<Symbol> symbols, String label) {
      return new Alternative(symbols, label);
    }

    public boolean isEmpty() {
      return symbols.isEmpty();
    }

    public int size() {
      return symbols.size();
    }

    public Symbol first() {
      return symbols.getFirst();
    }

    public Symbol get(int index) {
      return symbols.get(index);
    }
  }

  /** Ordered productions with fixed-point FIRST/FOLLOW computation, including cyclic grammars. */
  public static class ProductionMap {
    private final Map<NonTerminal, ProductionRule> rules = new LinkedHashMap<>();
    private final Map<NonTerminal, Set<Terminal>> first = new LinkedHashMap<>();
    private final Map<NonTerminal, Set<Terminal>> follow = new LinkedHashMap<>();
    private NonTerminal startSymbol;

    public boolean isEmpty() {
      return rules.isEmpty();
    }

    public int size() {
      return rules.size();
    }

    public Set<NonTerminal> nonTerminals() {
      return Collections.unmodifiableSet(rules.keySet());
    }

    public Collection<ProductionRule> rules() {
      return Collections.unmodifiableCollection(rules.values());
    }

    public boolean containsNonTerminal(NonTerminal nonTerminal) {
      return rules.containsKey(nonTerminal);
    }

    public ProductionRule get(NonTerminal key) {
      return rules.get(key);
    }

    private void invalidate() {
      first.clear();
      follow.clear();
    }

    protected void validate() {
      for (ProductionRule rule : rules.values()) {
        for (Alternative alternative : rule.alternatives()) {
          for (Symbol symbol : alternative.symbols()) {
            if (symbol instanceof NonTerminal nt && !rules.containsKey(nt)) {
              throw GeneratorException.create(
                  ErrorType.ERR_GRAMMAR_PRODUCTION_INVALID, "No production for nonterminal %s", nt);
            }
            if (symbol instanceof Terminal t && t.equals(Terminal.endOfInput())) {
              throw GeneratorException.create(
                  ErrorType.ERR_GRAMMAR_PRODUCTION_INVALID, "Terminal name %s is reserved", t);
            }
          }
        }
      }
    }

    protected void add(ProductionRule rule) {
      ProductionRule old = rules.get(rule.lhs());
      if (old != null) {
        List<Alternative> alternatives = new ArrayList<>(old.alternatives());
        alternatives.addAll(rule.alternatives());
        rule = ProductionRule.create(rule.lhs(), alternatives);
      }
      rules.put(rule.lhs(), rule);
      invalidate();
    }

    public void addAll(List<ProductionRule> productionRules) {
      productionRules.forEach(this::add);
    }

    public Set<Terminal> getFirstSet(NonTerminal nonTerminal) {
      computeFirst();
      if (!rules.containsKey(nonTerminal))
        throw new IllegalArgumentException("Unknown nonterminal " + nonTerminal);
      return Set.copyOf(first.get(nonTerminal));
    }

    public Set<Terminal> firstOf(List<? extends Symbol> sequence) {
      computeFirst();
      return Set.copyOf(firstOf(sequence, first));
    }

    private static Set<Terminal> firstOf(
        List<? extends Symbol> sequence, Map<NonTerminal, Set<Terminal>> sets) {
      Set<Terminal> result = new LinkedHashSet<>();
      for (Symbol symbol : sequence) {
        Set<Terminal> values =
            symbol instanceof Terminal t ? Set.of(t) : sets.get((NonTerminal) symbol);
        for (Terminal t : values) if (!t.isEpsilon()) result.add(t);
        if (!values.contains(Terminal.createEpsilon())) return result;
      }
      result.add(Terminal.createEpsilon());
      return result;
    }

    private void computeFirst() {
      if (!first.isEmpty()) return;
      nonTerminals().forEach(nt -> first.put(nt, new LinkedHashSet<>()));
      boolean changed;
      do {
        changed = false;
        for (ProductionRule rule : rules.values()) {
          for (Alternative alternative : rule.alternatives()) {
            changed |= first.get(rule.lhs()).addAll(firstOf(alternative.symbols(), first));
          }
        }
      } while (changed);
    }

    public Set<Terminal> getFollowSet(NonTerminal nonTerminal) {
      computeFirst();
      if (!rules.containsKey(nonTerminal))
        throw new IllegalArgumentException("Unknown nonterminal " + nonTerminal);
      if (follow.isEmpty()) {
        nonTerminals().forEach(nt -> follow.put(nt, new LinkedHashSet<>()));
        NonTerminal start = startSymbol == null ? rules.keySet().iterator().next() : startSymbol;
        follow.get(start).add(Terminal.endOfInput());
        boolean changed;
        do {
          changed = false;
          for (ProductionRule rule : rules.values()) {
            for (Alternative alternative : rule.alternatives()) {
              List<Symbol> symbols = alternative.symbols();
              for (int i = 0; i < symbols.size(); i++) {
                if (!(symbols.get(i) instanceof NonTerminal nt)) continue;
                Set<Terminal> suffix = firstOf(symbols.subList(i + 1, symbols.size()), first);
                for (Terminal terminal : suffix)
                  if (!terminal.isEpsilon()) changed |= follow.get(nt).add(terminal);
                if (suffix.contains(Terminal.createEpsilon()))
                  changed |= follow.get(nt).addAll(follow.get(rule.lhs()));
              }
            }
          }
        } while (changed);
      }
      return Set.copyOf(follow.get(nonTerminal));
    }
  }

  public abstract static sealed class Symbol permits NonTerminal, Terminal {
    protected final String name;

    Symbol(String name) {
      this.name = Objects.requireNonNull(name, "name");
      if (name.isEmpty()) throw new IllegalArgumentException("Symbol name cannot be empty");
    }

    public String name() {
      return name;
    }

    public String methodName() {
      return name.toLowerCase(Locale.ROOT).replace("'", "Prime");
    }

    public String className() {
      String method = methodName();
      return Character.toUpperCase(method.charAt(0)) + method.substring(1);
    }

    public static Symbol parse(String name) {
      return name.matches("[A-Z][A-Z0-9_]*'*") ? NonTerminal.of(name) : Terminal.of(name);
    }

    @Override
    public String toString() {
      return name;
    }

    @Override
    public boolean equals(Object obj) {
      return obj != null && obj.getClass() == getClass() && name.equals(((Symbol) obj).name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(getClass(), name);
    }
  }

  public static final class NonTerminal extends Symbol {
    private NonTerminal(String name) {
      super(name);
      if (!name.matches("[A-Z][A-Z0-9_]*'*"))
        throw new IllegalArgumentException("Nonterminals must be uppercase identifiers: " + name);
    }

    public static NonTerminal of(String name) {
      return new NonTerminal(name);
    }
  }

  public static final class Terminal extends Symbol {
    private static final Terminal EPSILON = new Terminal("ε");
    private static final Terminal EOF = new Terminal("$end");

    private Terminal(String name) {
      super(name);
    }

    public static Terminal of(String name) {
      return name.equals("ε") ? EPSILON : new Terminal(name);
    }

    public static Terminal createEpsilon() {
      return EPSILON;
    }

    public static Terminal endOfInput() {
      return EOF;
    }

    public boolean isEpsilon() {
      return name.equals(EPSILON.name);
    }
  }
}
