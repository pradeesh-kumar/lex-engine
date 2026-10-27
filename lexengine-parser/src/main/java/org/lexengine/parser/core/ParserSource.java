/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.parser.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import org.lexengine.commons.TemplateRenderer;
import org.lexengine.commons.error.ErrorType;
import org.lexengine.commons.error.GeneratorException;

/** Emits self-contained Java source and shares the token/parse-tree contract across algorithms. */
public final class ParserSource {
  private final GrammarSpec spec;
  private final Grammar grammar;
  private final List<Grammar.NonTerminal> nonTerminals;
  private final List<Grammar.Terminal> terminals;
  private final List<PredictiveTable.Rule> rules = new ArrayList<>();

  public ParserSource(GrammarSpec spec, Grammar grammar) {
    this.spec = spec;
    this.grammar = grammar;
    nonTerminals = List.copyOf(grammar.productions().nonTerminals());
    Set<Grammar.Terminal> terminalSet = new TreeSet<>(Comparator.comparing(Grammar.Symbol::name));
    for (Grammar.ProductionRule production : grammar.productions().rules()) {
      for (Grammar.Alternative alternative : production.alternatives()) {
        rules.add(new PredictiveTable.Rule(production.lhs(), alternative));
        for (Grammar.Symbol symbol : alternative.symbols())
          if (symbol instanceof Grammar.Terminal terminal) terminalSet.add(terminal);
      }
    }
    terminalSet.add(Grammar.Terminal.endOfInput());
    terminals = List.copyOf(terminalSet);
  }

  public List<Grammar.NonTerminal> nonTerminals() {
    return nonTerminals;
  }

  public List<Grammar.Terminal> terminals() {
    return terminals;
  }

  public List<PredictiveTable.Rule> rules() {
    return List.copyOf(rules);
  }

  public int nonTerminal(Grammar.NonTerminal nt) {
    return nonTerminals.indexOf(nt);
  }

  public int terminal(Grammar.Terminal terminal) {
    return terminals.indexOf(terminal);
  }

  public int symbol(Grammar.Symbol symbol) {
    return symbol instanceof Grammar.NonTerminal nt
        ? nonTerminal(nt)
        : -terminal((Grammar.Terminal) symbol) - 1;
  }

  public static String quote(String text) {
    StringBuilder result = new StringBuilder("\"");
    for (char ch : text.toCharArray())
      result.append(
          switch (ch) {
            case '\\' -> "\\\\";
            case '"' -> "\\\"";
            case '\n' -> "\\n";
            case '\r' -> "\\r";
            case '\t' -> "\\t";
            case '\b' -> "\\b";
            case '\f' -> "\\f";
            default -> ch < 32 ? String.format("\\%03o", (int) ch) : String.valueOf(ch);
          });
    return result.append('"').toString();
  }

  public static String ints(int[] values) {
    return Arrays.stream(values)
        .mapToObj(Integer::toString)
        .collect(Collectors.joining(",", "{", "}"));
  }

  public static String matrix(int[][] values) {
    return Arrays.stream(values)
        .map(ParserSource::ints)
        .collect(Collectors.joining(",\n", "{", "}"));
  }

  private static String strings(List<String> values) {
    return values.stream().map(ParserSource::quote).collect(Collectors.joining(",", "{", "}"));
  }

  public String grammarArrays() {
    int[] lhs = rules.stream().mapToInt(r -> nonTerminal(r.lhs())).toArray();
    int[][] rhs =
        rules.stream()
            .map(r -> r.alternative().symbols().stream().mapToInt(this::symbol).toArray())
            .toArray(int[][]::new);
    return "private static final String[] NONTERMINALS = "
        + strings(nonTerminals.stream().map(Grammar.Symbol::name).toList())
        + ";\n"
        + "private static final String[] TERMINALS = "
        + strings(terminals.stream().map(Grammar.Symbol::name).toList())
        + ";\n"
        + "private static final String[] LABELS = "
        + strings(rules.stream().map(r -> r.alternative().label()).toList())
        + ";\n"
        + "private static final int[] LHS = "
        + ints(lhs)
        + ";\n"
        + "private static final int[][] RHS = "
        + matrix(rhs)
        + ";\n";
  }

  public Path write(Path outDir, Path templatePath, String parserMembers) {
    try {
      String header =
          (spec.parserPackageName().isEmpty() ? "" : "package " + spec.parserPackageName() + ";\n")
              + "public class "
              + spec.parserClassName()
              + " {";
      String footer = "}";
      if (spec.definition() != null) {
        var metadata = spec.definition().metadata();
        header = metadata.compilableUnit().substring(0, metadata.bodyIndex());
        footer = metadata.compilableUnit().substring(metadata.bodyIndex());
      }
      String template;
      if (templatePath != null) template = Files.readString(templatePath);
      else
        try (InputStream input =
            ParserSource.class.getResourceAsStream("/recursive-descent-parser-class.template")) {
          if (input == null) throw new IOException("Missing parser template");
          template = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
      Map<String, String> attributes =
          Map.of(
              "classHeader",
              header,
              "classFooter",
              footer,
              "runtimeMembers",
              runtime(),
              "parserMembers",
              grammarArrays() + parserMembers,
              "lexerMembers",
              lexer());
      Files.createDirectories(outDir);
      Path path = outDir.resolve(spec.parserClassName() + ".java");
      Files.writeString(
          path, TemplateRenderer.render(template, attributes), StandardCharsets.UTF_8);
      return path;
    } catch (IOException ex) {
      throw GeneratorException.create(
          ErrorType.ERR_CLASS_GENERATE, ex, "Cannot generate parser %s", spec.parserClassName());
    }
  }

  private String runtime() {
    return """
      public interface Lexer {
        boolean hasNext();
        Token peek();
        Token next();
        interface Token { String name(); String value(); }
      }
      public record Token(String name, String value) implements Lexer.Token {
        public Token { java.util.Objects.requireNonNull(name); java.util.Objects.requireNonNull(value); }
      }
      public record Node(String symbol, String label, String value, java.util.List<Node> children) {
        public Node { children = java.util.List.copyOf(children); }
      }
      public record ParseTree(Node root) {}
      public static class ParserException extends RuntimeException {
        public ParserException(String message) { super(message); }
      }
      private static Lexer.Token makeToken(String name, String value) { return new Token(name, value); }
      private final Lexer lexer;
      private Lexer.Token curToken;
      private boolean loaded;
      private int tokenIndex;
      public CLASS_NAME(Lexer lexer) { this.lexer = java.util.Objects.requireNonNull(lexer); }
      public CLASS_NAME(java.util.List<? extends Lexer.Token> tokens) { this(new ListLexer(tokens)); }
      private static class ListLexer implements Lexer {
        private final java.util.List<? extends Lexer.Token> tokens;
        private int index;
        ListLexer(java.util.List<? extends Lexer.Token> tokens) { this.tokens = java.util.List.copyOf(tokens); }
        public boolean hasNext() { return index < tokens.size(); }
        public Lexer.Token peek() { return hasNext() ? tokens.get(index) : null; }
        public Lexer.Token next() { if (!hasNext()) throw new java.util.NoSuchElementException(); return tokens.get(index++); }
      }
      private Lexer.Token lookAhead() {
        if (!loaded) { curToken = lexer.hasNext() ? java.util.Objects.requireNonNull(lexer.next(), "Lexer returned null token") : null; loaded = true; }
        return curToken;
      }
      private void advance() { lookAhead(); loaded = false; curToken = null; tokenIndex++; }
      private int terminal() {
        Lexer.Token token = lookAhead();
        if (token == null) return EOF_INDEX;
        for (int i = 0; i < TERMINALS.length; i++) if (i != EOF_INDEX && TERMINALS[i].equals(token.name())) return i;
        for (int i = 0; i < TERMINALS.length; i++) if (i != EOF_INDEX && TERMINALS[i].equals(token.value())) return i;
        throw syntax("a grammar terminal");
      }
      private Node match(int expected) {
        if (lookAhead() == null || terminal() != expected) throw syntax(TERMINALS[expected]);
        Node result = new Node(TERMINALS[expected], null, lookAhead().value(), java.util.List.of());
        advance();
        return result;
      }
      private ParserException syntax(String expected) {
        Lexer.Token token = lookAhead();
        return new ParserException("At token " + tokenIndex + ": expected " + expected + ", found " + (token == null ? "end of input" : token.name() + " (" + token.value() + ")"));
      }
      private String expected(int[] row, int empty) {
        java.util.List<String> names = new java.util.ArrayList<>();
        for (int i = 0; i < row.length; i++) if (row[i] != empty) names.add(TERMINALS[i]);
        return names.toString();
      }
      """
        .replace("CLASS_NAME", spec.parserClassName())
        .replace("EOF_INDEX", Integer.toString(terminal(Grammar.Terminal.endOfInput())));
  }

  private String lexer() {
    if (spec.definition() == null) return "";
    var tokens = spec.definition().tokenDefinition().tokenRules();
    String names = strings(tokens.stream().map(GrammarV2.TokenDefinition.TokenRule::name).toList());
    String patterns =
        tokens.stream()
            .map(t -> "java.util.regex.Pattern.compile(" + quote(t.regex()) + ")")
            .collect(Collectors.joining(",", "{", "}"));
    String ignored =
        tokens.stream()
            .map(t -> Boolean.toString(t.ignored()))
            .collect(Collectors.joining(",", "{", "}"));
    return "public "
        + spec.parserClassName()
        + "(String source) { this(new TextLexer(source)); }\n"
        + "private static final String[] TOKEN_NAMES = "
        + names
        + ";\n"
        + "private static final java.util.regex.Pattern[] TOKEN_PATTERNS = "
        + patterns
        + ";\n"
        + "private static final boolean[] TOKEN_IGNORED = "
        + ignored
        + ";\n"
        + """
        private static class TextLexer implements Lexer {
          private final String source;
          private int offset;
          private Lexer.Token next;
          TextLexer(String source) { this.source = java.util.Objects.requireNonNull(source); }
          public boolean hasNext() { prepare(); return next != null; }
          public Lexer.Token peek() { prepare(); return next; }
          public Lexer.Token next() {
            prepare();
            if (next == null) throw new java.util.NoSuchElementException();
            Lexer.Token token = next; next = null; return token;
          }
          private void prepare() {
            while (next == null && offset < source.length()) {
              int best = -1, length = -1;
              for (int i = 0; i < TOKEN_PATTERNS.length; i++) {
                var matcher = TOKEN_PATTERNS[i].matcher(source).region(offset, source.length());
                if (matcher.lookingAt()) {
                  int size = matcher.end() - offset;
                  if (size == 0) throw new ParserException("Token " + TOKEN_NAMES[i] + " matched empty input at character " + offset);
                  if (size > length) { best = i; length = size; }
                }
              }
              if (best < 0) throw new ParserException("No token matches at character " + offset);
              String value = source.substring(offset, offset + length);
              offset += length;
              if (!TOKEN_IGNORED[best]) next = makeToken(TOKEN_NAMES[best], value);
            }
          }
        }
        """;
  }
}
