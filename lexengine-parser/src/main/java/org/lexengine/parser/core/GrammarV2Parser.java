package org.lexengine.parser.core;

import org.lexengine.commons.error.ErrorType;
import org.lexengine.commons.error.GeneratorException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class GrammarV2Parser {

  private static final String SECTION_SEPARATOR = "---";
  private static final String COMMENT_CHAR = "#";

  private final BufferedReader grammarReader;
  private String currentLine;
  private int linePos;
  private int lineCount;

  public GrammarV2Parser(InputStream inputStream) {
    this.grammarReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    this.lineCount = 0;
  }

  public GrammarV2 parse() {
    try (this.grammarReader) {
      GrammarV2.Metadata metadata = new MetadataParser().parse();
      GrammarV2.TokenDefinition tokenDefinition = new TokenDefinitionParser().parse();
      GrammarV2.GrammarDefinition grammarDefinition = new GrammarDefinitionParser().parse();
      return new GrammarV2(metadata, tokenDefinition, grammarDefinition);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private GeneratorException raiseError(String message) {
    return GeneratorException.create(ErrorType.ERR_GRAMMAR_INVALID, lineCount, linePos, message);
  }

  private boolean isCommentOrEmpty() {
    skipWhitespace();
    char peek = peek();
    return peek == '#' || peek == '\0';
  }

  private boolean isLineSeparator() {
    if (!SECTION_SEPARATOR.equals(currentLine)) {
      return false;
    }
    match("---");
    return true;
  }

  private void skipWhitespace() {
    char peek = peek();
    while (peek == ' ' || peek == '\t' || peek == '\r' || peek == '\n') {
      advance();
      peek = peek();
    }
  }

  private char peek() {
    if (currentLine == null || linePos >= currentLine.length())
      return '\0';
    return currentLine.charAt(linePos);
  }

  private void advance() {
    if (currentLine != null && linePos < currentLine.length()) {
      linePos++;
    }
  }

  private boolean match(char expected) {
    if (peek() == expected) {
      advance();
      return true;
    }
    return false;
  }

  private boolean match(String expected) {
    for (int i = 0; i < expected.length(); i++) {
      if (peek() == '\0') return false;
      if (peek() != expected.charAt(i)) {
        return false;
      }
      advance();
    }
    return true;
  }

  private boolean nextLine() throws IOException {
    while (true) {
      String line = grammarReader.readLine();
      if (line == null) {
        currentLine = null;
        return false;
      }
      lineCount++;
      // Remove comments
      int hash = line.indexOf('#');
      if (hash != -1) line = line.substring(0, hash);
      line = line.trim();
      if (!line.isEmpty()) {
        currentLine = line;
        linePos = 0;
        return true;
      }
    }
  }

  private String parseIdentifier() {
    skipWhitespace();
    StringBuilder sb = new StringBuilder();
    char ch = peek();
    while (!Character.isWhitespace(ch) && ch != ';' && (Character.isLetterOrDigit(ch) || ch == '_')) {
      sb.append(ch);
      advance();
      ch = peek();
    }
    return sb.toString();
  }

  private String parseDoubleQuoted() {
    if (!match('"')) {
      throw raiseError("Expected opening quote \"");
    }
    StringBuilder sb = new StringBuilder();
    while (true) {
      char ch = peek();
      if (ch == '\\') {
        advance();
        char next = peek();
        sb.append(next);
        advance();
      } else if (ch == '"') {
        match('"');
        break;
      } else if (ch == '\0') {
        throw raiseError("Unterminated double quoted string");
      } else {
        sb.append(ch);
        advance();
      }
    }
    return sb.toString();
  }

  private String parseSingleQuoted() {
    if (!match('\'')) {
      throw raiseError("Expected opening single quote");
    }
    StringBuilder sb = new StringBuilder();
    while (true) {
      char ch = peek();
      if (ch == '\\') {
        advance();
        char next = peek();
        sb.append(next);
        advance();
      } else if (ch == '\'') {
        match('\'');
        break;
      } else if (ch == '\0') {
        throw raiseError("Unterinated single quoted string");
      } else {
        sb.append(ch);
        advance();
      }
    }
    return sb.toString();
  }

  private class MetadataParser {
    private GrammarV2.Metadata parse() throws IOException {
      String line;
      StringBuilder compilationUnitBuilder = new StringBuilder();
      int bodyIndex = -1;
      while ((line = grammarReader.readLine()) != null) {
        if (line.startsWith(SECTION_SEPARATOR)) {
          break;
        }
        if (line.stripLeading().startsWith(COMMENT_CHAR)) {
          continue;
        }
        compilationUnitBuilder.append(line);
        if (bodyIndex == -1) {
          int indexOfFirstBrace = line.indexOf("{");
          if (indexOfFirstBrace != -1) {
            bodyIndex = indexOfFirstBrace + 1;
          }
        }
      }
      return new GrammarV2.Metadata(compilationUnitBuilder.toString(), bodyIndex);
    }
  }

  private class TokenDefinitionParser {

    public GrammarV2.TokenDefinition parse() throws IOException {
      List<GrammarV2.TokenDefinition.TokenRule> rules = new ArrayList<>();
      while (nextLine()) {
        skipWhitespace();
        if (isCommentOrEmpty()) continue;

        String key = parseIdentifier();
        skipWhitespace();
        if (!match(':')) {
          throw raiseError("Expected ':' after the key");
        }
        skipWhitespace();
        if (peek() == '{') {
          rules.addAll(parseGroupRules(key));
        } else {
          String value = parseValue();
          rules.add(new GrammarV2.TokenDefinition.TokenRule(key, key, value));
        }
      }
      return new GrammarV2.TokenDefinition(rules);
    }

    private List<GrammarV2.TokenDefinition.TokenRule> parseGroupRules(String group) {
      List<GrammarV2.TokenDefinition.TokenRule> groupRules = new ArrayList<>();
      if (!match('{')) {
        throw raiseError("Expected '{' at the  beginning of the group");
      }
      while (true) {
        skipWhitespace();
        if (peek() == '}') {
          match('}');
          break;
        }
        String name = parseIdentifier();
        skipWhitespace();
        if (!match(':')) {
          throw raiseError("Expected ':' after the key");
        }
        skipWhitespace();
        String regex = parseValue();
        groupRules.add(new GrammarV2.TokenDefinition.TokenRule(group, name, regex));
        skipWhitespace();
        if (peek() == ',') match(','); // Optional comma
        skipWhitespace();
      }
      return groupRules;
    }

    private String parseValue() {
      skipWhitespace();
      char ch = peek();
      if (ch == '"') return parseDoubleQuoted();
      // Allow unquoted value for flexibility (e.g., non-string regex)
      StringBuilder sb = new StringBuilder();
      while (ch != '\0' && ch != ',' && ch != '}' && !Character.isWhitespace(ch)) {
        sb.append(ch);
        advance();
        ch = peek();
      }
      return sb.toString().trim();
    }
  }

  private class GrammarDefinitionParser {
    private GrammarV2.GrammarDefinition parse() throws IOException {
      GrammarV2.NonTerminal startSymbol = null;
      GrammarV2.ProductionMap productionMap = new GrammarV2.ProductionMap();
      while (nextLine()) {
        skipWhitespace();
        if (isCommentOrEmpty()) continue;
        GrammarV2.NonTerminal lhs = GrammarV2.NonTerminal.of(parseIdentifier());
        skipWhitespace();
        if (!match("->")) {
          throw raiseError("Expected '->' at the  beginning of a production rule");
        }
        if (startSymbol == null) {
          startSymbol = lhs;
        }
        List<GrammarV2.Alternative> alternatives = parseAlternatives();
        productionMap.add(GrammarV2.ProductionRule.create(lhs, alternatives));
      }
      return new GrammarV2.GrammarDefinition(productionMap, startSymbol);
    }

    private List<GrammarV2.Alternative> parseAlternatives() {
      List<GrammarV2.Alternative> alternatives = new LinkedList<>();
      List<GrammarV2.Symbol> symbols = new LinkedList<>();
      String label = null;
      while (peek() != ';') {
        char peek = peek();
        skipWhitespace();
        if (peek == ';') {
          advance();
          alternatives.add(GrammarV2.Alternative.create(symbols, label));
          break;
        }
        if (peek == '|') {
          if (symbols.isEmpty()) {
            throw raiseError("Illegal char '|'");
          }
          advance();
          alternatives.add(GrammarV2.Alternative.create(symbols, label));
          alternatives.addAll(parseAlternatives());
          break;
        }
        if (peek == '\'') {
          String terminal = parseSingleQuoted();
          symbols.add(GrammarV2.Symbol.parse(terminal));
        } else if (peek == '$') {
          advance();
          label = parseIdentifier();
        } else if (Character.isLetter(peek)) {
          symbols.add(GrammarV2.Symbol.parse(parseIdentifier()));
        } else {
          throw raiseError("Illegal char '" + peek + "'");
        }
      }
      if (alternatives.isEmpty()) {
        throw raiseError("Alternative expected");
      }
      return alternatives;
    }
  }
}
