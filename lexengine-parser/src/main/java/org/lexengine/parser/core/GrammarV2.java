/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.parser.core;

import java.util.*;
import java.util.regex.Pattern;
import org.lexengine.commons.Validations;

/** Combined Java metadata, lexer rules and EBNF lowered to the shared context-free grammar. */
public record GrammarV2(
    Metadata metadata, TokenDefinition tokenDefinition, GrammarDefinition grammarDefinition) {
  public GrammarV2 {
    Objects.requireNonNull(metadata, "metadata");
    Objects.requireNonNull(tokenDefinition, "tokenDefinition");
    Objects.requireNonNull(grammarDefinition, "grammarDefinition");
  }

  public record Metadata(String compilableUnit, int bodyIndex) {
    private static final Pattern CLASS =
        Pattern.compile("\\bpublic\\s+(?:final\\s+)?class\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\{");
    private static final Pattern PACKAGE = Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;");

    public Metadata {
      Validations.requireNonBlank(compilableUnit, "Compilation unit");
      if (bodyIndex < 1
          || bodyIndex > compilableUnit.length()
          || compilableUnit.charAt(bodyIndex - 1) != '{')
        throw new IllegalArgumentException("Invalid parser class body index");
    }

    public static Metadata parse(String text) {
      String masked = maskJava(text);
      var matcher = CLASS.matcher(masked);
      if (!matcher.find())
        throw new IllegalArgumentException(
            "Metadata must contain a public Java class declaration, for example public class MyParser {}");
      int depth = 1;
      for (int i = matcher.end(); i < masked.length(); i++) {
        if (masked.charAt(i) == '{') depth++;
        else if (masked.charAt(i) == '}' && --depth == 0) return new Metadata(text, matcher.end());
      }
      throw new IllegalArgumentException("Unclosed parser class body");
    }

    public String className() {
      var matcher = CLASS.matcher(maskJava(compilableUnit));
      if (!matcher.find()) throw new IllegalArgumentException("Missing parser class");
      return matcher.group(1);
    }

    public String packageName() {
      var matcher = PACKAGE.matcher(maskJava(compilableUnit));
      return matcher.find() ? matcher.group(1) : "";
    }

    /** Masks Java comments and literals so braces and declarations inside them are ignored. */
    private static String maskJava(String text) {
      char[] masked = text.toCharArray();
      int position = 0;
      while (position < text.length()) {
        int start = position;
        if (text.startsWith("//", position)) {
          while (position < text.length() && text.charAt(position) != '\n') position++;
        } else if (text.startsWith("/*", position)) {
          int end = text.indexOf("*/", position + 2);
          if (end < 0) throw new IllegalArgumentException("Unterminated Java comment in metadata");
          position = end + 2;
        } else if (text.startsWith("\"\"\"", position)) {
          position += 3;
          while (position < text.length() && !text.startsWith("\"\"\"", position)) {
            if (text.charAt(position++) == '\\' && position < text.length()) position++;
          }
          if (position == text.length())
            throw new IllegalArgumentException("Unterminated Java text block in metadata");
          position += 3;
        } else if (text.charAt(position) == '"' || text.charAt(position) == '\'') {
          char quote = text.charAt(position++);
          boolean closed = false;
          while (position < text.length()) {
            char next = text.charAt(position++);
            if (next == '\\' && position < text.length()) position++;
            else if (next == quote) {
              closed = true;
              break;
            }
          }
          if (!closed) throw new IllegalArgumentException("Unterminated Java literal in metadata");
        } else {
          position++;
          continue;
        }
        for (int i = start; i < position; i++)
          if (masked[i] != '\n' && masked[i] != '\r') masked[i] = ' ';
      }
      return new String(masked);
    }
  }

  public record TokenDefinition(List<TokenRule> tokenRules) {
    public TokenDefinition {
      tokenRules = List.copyOf(tokenRules);
    }

    public record TokenRule(String group, String name, String regex) {
      public TokenRule {
        Validations.requireNonBlank(name, "Token name");
        Objects.requireNonNull(regex, "regex");
        if (Pattern.compile(regex).matcher("").lookingAt())
          throw new IllegalArgumentException("Token " + name + " matches empty input");
        if (name.equals(Grammar.Terminal.endOfInput().name()))
          throw new IllegalArgumentException("Reserved token name " + name);
      }

      public boolean ignored() {
        return "TOKEN_IGNORE".equals(group) || "TOKEN_IGNORE".equals(name);
      }
    }
  }

  public record GrammarDefinition(
      Grammar.ProductionMap productions, Grammar.NonTerminal startSymbol) {
    public Grammar toGrammar() {
      return new Grammar(productions, startSymbol);
    }
  }

  public Grammar grammar() {
    return grammarDefinition.toGrammar();
  }

  public GrammarSpec toGrammarSpec() {
    return new GrammarSpec(metadata.className(), metadata.packageName(), grammar(), this);
  }
}
