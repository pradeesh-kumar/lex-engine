/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.parser.core.ll;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import org.lexengine.parser.core.*;

/** Emits one predictive recursive method per nonterminal, with FIRST/FOLLOW conflict checks. */
public class RecursiveDescentParserGenerator extends ParserGenerator {
  public RecursiveDescentParserGenerator(File file, Path outDir, Path template) {
    super(file, outDir, template);
  }

  public RecursiveDescentParserGenerator(File file) {
    super(file);
  }

  @Override
  public void generate() {
    Grammar grammar = ParserHelper.eliminateLeftRecursion(grammarSpec.grammar());
    PredictiveTable table = new PredictiveTable(grammar);
    ParserSource source = new ParserSource(grammarSpec, grammar);
    StringBuilder members =
        new StringBuilder(
            "public ParseTree parse() { Node root = parseN"
                + source.nonTerminal(grammar.startSymbol())
                + "(); if (lookAhead() != null) throw syntax(\"end of input\"); return new ParseTree(root); }\n");
    for (Grammar.NonTerminal nt : source.nonTerminals()) {
      members
          .append("private Node parseN")
          .append(source.nonTerminal(nt))
          .append("() {\nswitch (terminal()) {\n");
      List<String> expected = new ArrayList<>();
      for (int i = 0; i < table.rules().size(); i++) {
        var rule = table.rules().get(i);
        if (!rule.lhs().equals(nt)) continue;
        int ruleIndex = i;
        List<Grammar.Terminal> lookaheads =
            table.entries().entrySet().stream()
                .filter(e -> e.getValue() == ruleIndex)
                .map(e -> e.getKey().terminal())
                .sorted(Comparator.comparing(Grammar.Symbol::name))
                .toList();
        if (lookaheads.isEmpty()) continue;
        lookaheads.forEach(t -> expected.add(t.name()));
        members
            .append("case ")
            .append(
                lookaheads.stream()
                    .map(t -> Integer.toString(source.terminal(t)))
                    .collect(Collectors.joining(",")))
            .append(" -> { return new Node(")
            .append(ParserSource.quote(nt.name()))
            .append(',')
            .append(ParserSource.quote(rule.alternative().label()))
            .append(",null,java.util.List.of(");
        members.append(
            rule.alternative().symbols().stream()
                .map(
                    symbol ->
                        symbol instanceof Grammar.NonTerminal nonTerminal
                            ? "parseN" + source.nonTerminal(nonTerminal) + "()"
                            : "match(" + source.terminal((Grammar.Terminal) symbol) + ")")
                .collect(Collectors.joining(",")));
        members.append(")); }\n");
      }
      members
          .append("default -> throw syntax(")
          .append(ParserSource.quote(expected.toString()))
          .append(");\n}}\n");
    }
    writeParser(grammar, ParserType.RECURSIVE_DESCENT, members.toString(), 0);
  }
}
