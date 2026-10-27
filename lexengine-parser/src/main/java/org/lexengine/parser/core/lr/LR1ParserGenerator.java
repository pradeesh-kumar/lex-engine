/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.parser.core.lr;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import org.lexengine.commons.error.ErrorType;
import org.lexengine.commons.error.GeneratorException;
import org.lexengine.parser.core.*;

/** Emits a canonical LR(1) shift/reduce parser; ambiguous table entries are rejected. */
public class LR1ParserGenerator extends ParserGenerator {
  public LR1ParserGenerator(File file) {
    super(file);
  }

  public LR1ParserGenerator(File file, Path outDir, Path template) {
    super(file, outDir, template);
  }

  @Override
  public void generate() {
    Grammar grammar = grammarSpec.grammar();
    ParserSource source = new ParserSource(grammarSpec, grammar);
    CanonicalCollection collection = new CanonicalCollection(grammar);
    int[][] actions = new int[collection.states().size()][source.terminals().size()];
    int[][] gotos = new int[collection.states().size()][source.nonTerminals().size()];
    for (int[] row : gotos) Arrays.fill(row, -1);
    collection
        .transitions()
        .forEach(
            (transition, next) -> {
              if (transition.symbol() instanceof Grammar.NonTerminal nt)
                gotos[transition.state()][source.nonTerminal(nt)] = next;
              else
                put(
                    actions,
                    transition.state(),
                    source.terminal((Grammar.Terminal) transition.symbol()),
                    next + 1,
                    source);
            });
    for (int state = 0; state < collection.states().size(); state++) {
      for (CanonicalCollection.Item item : collection.states().get(state)) {
        var production = collection.productions().get(item.production());
        if (item.dot() == production.alternative().size())
          put(
              actions,
              state,
              source.terminal(item.lookahead()),
              item.production() == 0 ? Integer.MAX_VALUE : -item.production(),
              source);
      }
    }
    String members =
        "private static final int[][] ACTION = "
            + ParserSource.matrix(actions)
            + ";\n"
            + "private static final int[][] GOTO = "
            + ParserSource.matrix(gotos)
            + ";\n"
            + """
          public ParseTree parse() {
            java.util.Deque<Integer> states = new java.util.ArrayDeque<>();
            java.util.Deque<Node> nodes = new java.util.ArrayDeque<>();
            states.push(0);
            while (true) {
              int state = states.peek();
              int lookahead = terminal();
              int action = ACTION[state][lookahead];
              if (action == Integer.MAX_VALUE) return new ParseTree(nodes.pop());
              if (action > 0) {
                nodes.push(match(lookahead));
                states.push(action - 1);
              } else if (action < 0) {
                int production = -action - 1;
                java.util.LinkedList<Node> children = new java.util.LinkedList<>();
                for (int i = 0; i < RHS[production].length; i++) { states.pop(); children.addFirst(nodes.pop()); }
                int lhs = LHS[production];
                nodes.push(new Node(NONTERMINALS[lhs], LABELS[production], null, children));
                int next = GOTO[states.peek()][lhs];
                if (next < 0) throw new IllegalStateException("Missing LR goto");
                states.push(next);
              } else throw syntax(expected(ACTION[state], 0));
            }
          }
          """;
    writeParser(grammar, ParserType.LR1, members, collection.states().size());
  }

  private void put(int[][] actions, int state, int terminal, int action, ParserSource source) {
    int previous = actions[state][terminal];
    if (previous != 0 && previous != action)
      throw GeneratorException.create(
          ErrorType.ERR_GRAMMAR_INVALID,
          "LR(1) %s conflict in state %d on %s (actions %d and %d)",
          previous > 0 || action > 0 ? "shift/reduce" : "reduce/reduce",
          state,
          source.terminals().get(terminal),
          previous,
          action);
    actions[state][terminal] = action;
  }
}
