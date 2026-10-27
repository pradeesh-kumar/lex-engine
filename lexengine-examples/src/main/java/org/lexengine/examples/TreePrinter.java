/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.examples;

import java.util.ArrayDeque;
import java.util.List;
import java.util.function.Function;

/** Prints any of the independently generated Node types without a shared runtime dependency. */
public final class TreePrinter {
  private TreePrinter() {}

  private record Pending<N>(N node, int depth) {}

  public static <N> String format(
      N root,
      Function<N, String> symbol,
      Function<N, String> label,
      Function<N, String> value,
      Function<N, List<N>> children) {
    StringBuilder result = new StringBuilder();
    var pending = new ArrayDeque<Pending<N>>();
    pending.push(new Pending<>(root, 0));
    while (!pending.isEmpty()) {
      var current = pending.pop();
      N node = current.node();
      result.append("  ".repeat(current.depth())).append(symbol.apply(node));
      String alternative = label.apply(node);
      if (alternative != null && !alternative.equals("DefaultLabel"))
        result.append(" $").append(alternative);
      String text = value.apply(node);
      if (text != null)
        result
            .append(" = \"")
            .append(
                text.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t"))
            .append('"');
      result.append('\n');
      List<N> nodes = children.apply(node);
      for (int i = nodes.size() - 1; i >= 0; i--)
        pending.push(new Pending<>(nodes.get(i), current.depth() + 1));
    }
    return result.toString();
  }
}
