/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.parser.core.ll;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import org.lexengine.parser.core.*;

/** Emits a table-driven, iterative LL(1) parser. */
public class LL1ParserGenerator extends ParserGenerator {
  public LL1ParserGenerator(File file) {
    super(file);
  }

  public LL1ParserGenerator(File file, Path outDir, Path template) {
    super(file, outDir, template);
  }

  @Override
  public void generate() {
    Grammar grammar = ParserHelper.eliminateLeftRecursion(grammarSpec.grammar());
    PredictiveTable table = new PredictiveTable(grammar);
    ParserSource source = new ParserSource(grammarSpec, grammar);
    int[][] entries = new int[source.nonTerminals().size()][source.terminals().size()];
    for (int[] row : entries) Arrays.fill(row, -1);
    table
        .entries()
        .forEach(
            (key, value) ->
                entries[source.nonTerminal(key.nonTerminal())][source.terminal(key.terminal())] =
                    value);
    String members =
        "private static final int[][] TABLE = "
            + ParserSource.matrix(entries)
            + ";\n"
            + """
      private static class Builder {
        final String symbol, label;
        final java.util.List<Node> children = new java.util.ArrayList<>();
        Builder(String symbol, String label) { this.symbol = symbol; this.label = label; }
        Node build() { return new Node(symbol, label, null, children); }
      }
      private record Work(int symbol, Builder parent, Builder close) {}
      public ParseTree parse() {
        Builder root = new Builder("root", null);
        java.util.Deque<Work> stack = new java.util.ArrayDeque<>();
        stack.push(new Work(START_INDEX, root, null));
        while (!stack.isEmpty()) {
          Work work = stack.pop();
          if (work.close() != null) { work.parent().children.add(work.close().build()); continue; }
          int symbol = work.symbol();
          if (symbol < 0) { work.parent().children.add(match(-symbol - 1)); continue; }
          int production = TABLE[symbol][terminal()];
          if (production < 0) throw syntax(expected(TABLE[symbol], -1));
          Builder node = new Builder(NONTERMINALS[symbol], LABELS[production]);
          stack.push(new Work(0, work.parent(), node));
          for (int i = RHS[production].length - 1; i >= 0; i--) stack.push(new Work(RHS[production][i], node, null));
        }
        if (lookAhead() != null) throw syntax("end of input");
        return new ParseTree(root.children.getFirst());
      }
      """
                .replace(
                    "START_INDEX", Integer.toString(source.nonTerminal(grammar.startSymbol())));
    writeParser(grammar, ParserType.LL1, members, 0);
  }
}
