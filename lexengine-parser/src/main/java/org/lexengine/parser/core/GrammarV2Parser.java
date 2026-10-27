/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.parser.core;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import org.lexengine.commons.error.ErrorType;
import org.lexengine.commons.error.GeneratorException;

/** Reads a complete three-section .lg file, preserving quoted comments and regex escapes. */
public class GrammarV2Parser {
  private final Reader reader;

  public GrammarV2Parser(Reader reader) {
    this.reader = Objects.requireNonNull(reader);
  }

  public GrammarV2 parse() {
    try (reader) {
      StringWriter writer = new StringWriter();
      reader.transferTo(writer);
      String text = writer.toString();
      var separators = Pattern.compile("(?m)^[ \\t]*---[ \\t]*(?:#.*)?\\r?$").matcher(text);
      List<Integer> starts = new ArrayList<>();
      List<Integer> ends = new ArrayList<>();
      while (separators.find()) {
        starts.add(separators.start());
        ends.add(separators.end());
      }
      if (starts.size() != 2) throw error(1, 1, "Expected exactly two '---' section separators");
      String metadata = text.substring(0, starts.getFirst()).replaceAll("(?m)^[ \\t]*#.*$", "");
      Cursor tokens =
          new Cursor(text.substring(ends.get(0), starts.get(1)), lines(text, ends.get(0)));
      Cursor productions = new Cursor(text.substring(ends.get(1)), lines(text, ends.get(1)));
      return new GrammarV2(
          GrammarV2.Metadata.parse(metadata), parseTokens(tokens), parseGrammar(productions));
    } catch (IOException ex) {
      throw GeneratorException.create(ErrorType.ERR_GRAMMAR_FILE_READ, ex, "Cannot read grammar");
    } catch (IllegalArgumentException ex) {
      throw error(1, 1, ex.getMessage());
    }
  }

  private static int lines(String text, int end) {
    return 1 + (int) text.substring(0, end).chars().filter(c -> c == '\n').count();
  }

  private static GeneratorException error(int line, int column, String message) {
    return GeneratorException.create(ErrorType.ERR_GRAMMAR_INVALID, line, column, message);
  }

  private GrammarV2.TokenDefinition parseTokens(Cursor input) {
    List<GrammarV2.TokenDefinition.TokenRule> rules = new ArrayList<>();
    Set<String> names = new HashSet<>();
    while (!input.end()) {
      String group = input.identifier();
      input.expect(':');
      if (input.accept('{')) {
        while (!input.accept('}')) {
          if (input.end()) throw input.fail("Unterminated token group");
          String name;
          String regex;
          if (group.equals("TOKEN_IGNORE") && input.peek() == '"') {
            name = "TOKEN_IGNORE_" + rules.size();
            regex = input.quoted('"', true);
          } else {
            name = input.identifier();
            input.expect(':');
            regex = input.regex();
          }
          addToken(rules, names, group, name, regex, input);
          input.accept(',');
        }
      } else addToken(rules, names, group, group, input.regex(), input);
      input.accept(',');
      input.accept(';');
    }
    return new GrammarV2.TokenDefinition(rules);
  }

  private void addToken(
      List<GrammarV2.TokenDefinition.TokenRule> rules,
      Set<String> names,
      String group,
      String name,
      String regex,
      Cursor input) {
    if (!names.add(name)) throw input.fail("Duplicate token " + name);
    try {
      rules.add(new GrammarV2.TokenDefinition.TokenRule(group, name, regex));
    } catch (IllegalArgumentException ex) {
      throw input.fail("Invalid regex for " + name + ": " + ex.getMessage());
    }
  }

  private GrammarV2.GrammarDefinition parseGrammar(Cursor input) {
    Grammar.ProductionMap productions = new Grammar.ProductionMap();
    Set<String> reserved = new HashSet<>();
    var identifiers = Pattern.compile("[A-Z][A-Z0-9_]*'*").matcher(input.text);
    while (identifiers.find()) reserved.add(identifiers.group());
    List<Grammar.Symbol> repeated = new ArrayList<>();
    Grammar.NonTerminal start = null;
    while (!input.end()) {
      Grammar.NonTerminal lhs = Grammar.NonTerminal.of(input.identifier());
      if (start == null) start = lhs;
      input.expect('-');
      input.expect('>');
      List<Grammar.Alternative> alternatives =
          alternatives(input, ';', productions, reserved, repeated);
      input.expect(';');
      productions.add(Grammar.ProductionRule.create(lhs, alternatives));
    }
    Grammar grammar = new Grammar(productions, start);
    for (Grammar.Symbol item : repeated) {
      if (grammar.productions().firstOf(List.of(item)).contains(Grammar.Terminal.createEpsilon()))
        throw input.fail("A repeated EBNF expression must consume input");
    }
    return new GrammarV2.GrammarDefinition(productions, start);
  }

  private List<Grammar.Alternative> alternatives(
      Cursor input,
      char end,
      Grammar.ProductionMap productions,
      Set<String> names,
      List<Grammar.Symbol> repeated) {
    List<Grammar.Alternative> result = new ArrayList<>();
    do {
      List<Grammar.Symbol> symbols = new ArrayList<>();
      String label = null;
      boolean epsilon = false;
      while (input.peek() != end && input.peek() != '|') {
        if (input.end()) throw input.fail("Expected '" + end + "' before end of input");
        if (input.accept('$')) {
          label = input.identifier();
          if (input.peek() != end && input.peek() != '|')
            throw input.fail("A label must end its alternative");
          break;
        }
        if (input.accept('ε')) {
          if (epsilon || !symbols.isEmpty())
            throw input.fail("Epsilon must be the only symbol in an alternative");
          epsilon = true;
          continue;
        }
        if (epsilon) throw input.fail("Epsilon must be the only symbol in an alternative");
        Grammar.Symbol symbol;
        if (input.accept('(')) {
          List<Grammar.Alternative> group = alternatives(input, ')', productions, names, repeated);
          input.expect(')');
          Grammar.NonTerminal helper = helper(names);
          productions.add(Grammar.ProductionRule.create(helper, group));
          symbol = helper;
        } else if (input.peek() == '\'') symbol = Grammar.Terminal.of(input.quoted('\'', false));
        else symbol = Grammar.Symbol.parse(input.identifier());
        char suffix = input.peek();
        if (suffix == '?' || suffix == '*' || suffix == '+') {
          input.take();
          Grammar.NonTerminal helper = helper(names);
          List<Grammar.Alternative> choices = new ArrayList<>();
          if (suffix == '?') choices.add(Grammar.Alternative.create(List.of(symbol)));
          else {
            repeated.add(symbol);
            choices.add(Grammar.Alternative.create(List.of(symbol, helper)));
          }
          choices.add(Grammar.Alternative.create());
          productions.add(Grammar.ProductionRule.create(helper, choices));
          if (suffix == '+') symbols.add(symbol);
          symbol = helper;
        }
        symbols.add(symbol);
      }
      if (symbols.isEmpty() && !epsilon)
        throw input.fail("Alternative expected; use ε for an empty production");
      result.add(Grammar.Alternative.create(symbols, label));
    } while (input.accept('|'));
    return result;
  }

  private Grammar.NonTerminal helper(Set<String> names) {
    int i = names.size();
    while (!names.add("EBNF_" + i)) i++;
    return Grammar.NonTerminal.of("EBNF_" + i);
  }

  private static final class Cursor {
    private final String text;
    private int position;
    private int line;
    private int column = 1;

    Cursor(String text, int line) {
      this.text = text;
      this.line = line;
    }

    GeneratorException fail(String message) {
      return error(line, column, message);
    }

    char raw() {
      return position == text.length() ? '\0' : text.charAt(position);
    }

    char take() {
      char ch = raw();
      if (ch == '\0') throw fail("Unexpected end of input");
      position++;
      if (ch == '\n') {
        line++;
        column = 1;
      } else column++;
      return ch;
    }

    void skip() {
      while (true) {
        while (Character.isWhitespace(raw())) take();
        if (raw() != '#') return;
        while (raw() != '\0' && raw() != '\n') take();
      }
    }

    char peek() {
      skip();
      return raw();
    }

    boolean end() {
      return peek() == '\0';
    }

    boolean accept(char expected) {
      if (peek() != expected) return false;
      take();
      return true;
    }

    void expect(char expected) {
      if (!accept(expected)) throw fail("Expected '" + expected + "'");
    }

    String identifier() {
      skip();
      if (!Character.isLetter(raw()) && raw() != '_') throw fail("Identifier expected");
      StringBuilder value = new StringBuilder();
      while (Character.isLetterOrDigit(raw()) || raw() == '_' || raw() == '\'')
        value.append(take());
      return value.toString();
    }

    String regex() {
      if (peek() == '"') return quoted('"', true);
      StringBuilder result = new StringBuilder();
      while (raw() != '\0'
          && !Character.isWhitespace(raw())
          && raw() != ','
          && raw() != '}'
          && raw() != ';'
          && raw() != '#') result.append(take());
      if (result.isEmpty()) throw fail("Token regex expected");
      return result.toString();
    }

    String quoted(char quote, boolean regex) {
      expect(quote);
      StringBuilder value = new StringBuilder();
      while (raw() != quote) {
        if (raw() == '\0' || raw() == '\n' || raw() == '\r')
          throw fail("Unterminated quoted string");
        char ch = take();
        if (ch == '\\') {
          if (raw() == '\0') throw fail("Unterminated escape");
          char next = take();
          if (next == quote || next == '\\') value.append(next);
          else if (regex) value.append('\\').append(next);
          else
            value.append(
                switch (next) {
                  case 'n' -> '\n';
                  case 'r' -> '\r';
                  case 't' -> '\t';
                  default -> next;
                });
        } else value.append(ch);
      }
      take();
      if (!regex && value.isEmpty()) throw fail("Empty terminal; use ε instead");
      return value.toString();
    }
  }
}
