package org.lexengine.parser.core;

import org.lexengine.commons.error.ErrorType;
import org.lexengine.commons.error.GeneratorException;
import org.lexengine.commons.logging.Out;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.lexengine.commons.Validations.requireNonBlank;

public record GrammarV2(Metadata metadata, TokenDefinition tokenDefinition, GrammarDefinition grammarDefinition) {

  public GrammarV2 {
    Objects.requireNonNull(metadata, "Metadata cannot be null");
    Objects.requireNonNull(tokenDefinition, "Token Definitions cannot be null");
    Objects.requireNonNull(grammarDefinition, "Grammar Definitions cannot be null");
  }

  public record Metadata(String compilableUnit, int bodyIndex) {
    public Metadata {
      requireNonBlank(compilableUnit, "Compilation Unit");
      if  (bodyIndex < 0) {
        throw new IllegalArgumentException("body index cannot be negative");
      }
    }
  }

  public record TokenDefinition(List<TokenRule> tokenRules) {
    public record TokenRule(String group, String name, String regex) {}
  }

  public record GrammarDefinition(ProductionMap productions, NonTerminal startSymbol) {
  }

  public record ProductionRule(int index, NonTerminal lhs, List<Alternative> alternatives) {
    private static int idCounter = 0;

    public static ProductionRule create(NonTerminal lhs, List<Alternative> alternatives) {
      Objects.requireNonNull(lhs, "lhs");
      Objects.requireNonNull(alternatives, "alternatives");
      return new ProductionRule(idCounter++, lhs, alternatives);
    }
  }

  public record Alternative(List<Symbol> symbols, String label) {

    public static Alternative create() {
      return new Alternative(List.of(), "DefaultLabel");
    }

    public static Alternative create(List<Symbol> symbols, String label) {
      return new Alternative(symbols,  label);
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

  public static class ProductionMap {

    private final Map<NonTerminal, ProductionRule> rules;
    private final Map<NonTerminal, Set<Terminal>> firstSetMap;
    private final Map<NonTerminal, Set<Terminal>> followSetMap;

    public ProductionMap() {
      this.rules = new HashMap<>();
      this.firstSetMap = new HashMap<>();
      this.followSetMap = new HashMap<>();
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
          .filter(symbol -> symbol instanceof NonTerminal)
          .map(symbol -> (NonTerminal) symbol)
          .filter(Predicate.not(keys::contains))
          .collect(Collectors.toSet());
      if (!invalidNonTerminals.isEmpty()) {
        Out.error("Invalid non-terminals! Production rule not found for these non-terminal symbols: %s", invalidNonTerminals);
        throw GeneratorException.create(ErrorType.ERR_GRAMMAR_PRODUCTION_INVALID);
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

    public Set<Terminal> getFirstSet(NonTerminal nonTerminal) {
      if (firstSetMap.containsKey(nonTerminal)) {
        return Set.copyOf(firstSetMap.get(nonTerminal));
      }
      ProductionRule rule = get(nonTerminal);
      Set<Terminal> firstSet = new HashSet<>();
      List<Alternative> alternatives = rule.alternatives();
      for (Alternative alternative : alternatives) {
        boolean foundNonEpsilon = false;
        for (Symbol symbol : alternative.symbols) {
          if (symbol instanceof Terminal) {
            firstSet.add((Terminal) symbol);
            foundNonEpsilon = true;
            break;
          }
          Set<Terminal> firstOfA = getFirstSet((NonTerminal) symbol);
          if (!firstOfA.contains(Terminal.EPSILON_TERMINAL)) {
            foundNonEpsilon = true;
            break;
          }
          firstOfA.remove(Terminal.EPSILON_TERMINAL);
          firstSet.addAll(firstOfA);
        }
        if (!foundNonEpsilon) {
          firstSet.add(Terminal.EPSILON_TERMINAL);
        }
      }
      firstSetMap.put(nonTerminal, firstSet);
      return Set.copyOf(firstSet);
    }

    public Set<Terminal> getFollowSet(NonTerminal nonTerminal) {
      if (followSetMap.isEmpty()) {
        new FollowSetCompute().compute();
      }
      return Set.copyOf(followSetMap.get(nonTerminal));
    }

    private class FollowSetCompute {

      /** Map contains list of alternatives in which NonTerminal is present */
      private final Map<NonTerminal, List<FlattenedRule>> nonTerminalToAlternatives;
      /** Map contains indices of NonTerminal in Alternative */
      private final Map<Alternative, Map<NonTerminal, List<Integer>>> alternativeToNonTerminalIndies;

      public FollowSetCompute() {
        this.nonTerminalToAlternatives = new HashMap<>(rules.size());
        this.alternativeToNonTerminalIndies = new HashMap<>(rules.size());
      }

      private void compute() {
        initializeIndices();
        nonTerminals().forEach(nt -> followSetMap.put(nt, getFollowSet(nt)));
      }

      private Set<Terminal> getFollowSet(NonTerminal nonTerminal) {
        if (followSetMap.containsKey(nonTerminal)) {
          return Set.copyOf(followSetMap.get(nonTerminal));
        }
        Set<Terminal> followSet = new HashSet<>();
        List<FollowSetCompute.FlattenedRule> flattenedRules = nonTerminalToAlternatives.get(nonTerminal);
        for (FollowSetCompute.FlattenedRule flattenedRule : flattenedRules) {
          Alternative alternative = flattenedRule.alternative;
          List<Integer> indices = alternativeToNonTerminalIndies.get(alternative).get(nonTerminal);
          for (int i : indices) {
            if (i == alternative.size() - 1) {
              followSet.addAll(getFollowSet(flattenedRule.lhs));
              continue;
            }
            int nextIndex;
            for (nextIndex = i + 1; nextIndex < alternative.size(); nextIndex++) {
              Symbol next = alternative.get(i + 1);
              if (next instanceof Terminal) {
                followSet.add((Terminal) next);
                break;
              }
              Set<Terminal> firstSetOfNext = getFirstSet((NonTerminal) next);
              followSet.addAll(firstSetOfNext);
              if (!firstSetOfNext.contains(Terminal.EPSILON_TERMINAL)) {
                break;
              }
            }
            if (nextIndex == alternative.size()) {
              followSet.addAll(getFollowSet(flattenedRule.lhs));
            }
          }
        }
        followSetMap.put(nonTerminal, followSet);
        return Set.copyOf(followSet);
      }

      private void initializeIndices() {
        rules().forEach(rule -> {
          for (Alternative alternative : rule.alternatives()) {
            List<Symbol> symbols = alternative.symbols();
            for (int i = 0; i < symbols.size(); i++) {
              Symbol symbol = symbols.get(i);
              if (symbol instanceof Terminal) {
                continue;
              }
              NonTerminal nonTerminal = (NonTerminal) symbol;
              List<ProductionMap.FollowSetCompute.FlattenedRule> flattenedRule = nonTerminalToAlternatives.getOrDefault(nonTerminal, new ArrayList<>());
              flattenedRule.add(ProductionMap.FollowSetCompute.FlattenedRule.create(rule.lhs, alternative));
              nonTerminalToAlternatives.put(nonTerminal, flattenedRule);
              Map<NonTerminal, List<Integer>> nonTerminalIndices = alternativeToNonTerminalIndies.getOrDefault(alternative, new HashMap<>());
              nonTerminalIndices.getOrDefault(nonTerminal, new ArrayList<>()).add(i);
              alternativeToNonTerminalIndies.put(alternative, nonTerminalIndices);
            }
          }
        });
      }

      private record FlattenedRule(NonTerminal lhs, Alternative alternative) {
        public static FlattenedRule create(NonTerminal lhs, Alternative alternative) {
          return new FlattenedRule(lhs, alternative);
        }
      }
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

    public static Symbol literalTerminal(String literal) {
      return Terminal.of(literal);
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

    private static final Terminal EPSILON_TERMINAL = new Terminal("ε");

    private Terminal(String name) {
      super(name);
      if (name.matches("[A-Z]+'?")) {
        throw new IllegalArgumentException(
            "Invalid terminal name " + name + " Terminal name cannot start");
      }
    }

    public static Terminal of(String name) {
      if (EPSILON_TERMINAL.name().equals(name)) {
        return EPSILON_TERMINAL;
      }
      return new Terminal(name);
    }

    public static Terminal createEpsilon() {
      return EPSILON_TERMINAL;
    }

    public boolean isEpsilon() {
      return EPSILON_TERMINAL.name().equals(this.name);
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
