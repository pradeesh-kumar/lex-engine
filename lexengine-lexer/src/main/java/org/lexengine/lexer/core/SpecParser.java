/*
* Copyright (c) 2025 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import org.lexengine.commons.error.ErrorType;
import org.lexengine.commons.error.GeneratorException;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** A parser for lexer spec files that extracts regular expressions and actions from the file. */
public class SpecParser {

  private final BufferedReader specReader;
  private List<LexRule> lexRules;
  private int lineCount;

  /**
   * Constructs a new SpecParser instance for the specified lexer spec file.
   *
   * @param specReader the lexer specification reader
   */
  SpecParser(Reader specReader) {
    this.specReader = new BufferedReader(specReader);
    lexRules = new ArrayList<>();
  }
  /**
   * Parses the lexer spec file and extracts regular expressions and actions.
   *
   * @throws GeneratorException if an error occurs during parsing
   */
  public List<LexRule> parse() {
    var parser = new RegexLineParser();
    try (Stream<String> lines = specReader.lines()) {
      lines
          .map(String::trim)
          .filter(Predicate.not(String::isEmpty))
          .filter(line -> !line.stripLeading().startsWith("#"))
          .forEach(
              line -> {
                lineCount++;
                parser.parseLine(line);
              });
      if (lexRules.isEmpty()) {
        throw GeneratorException.create(ErrorType.ERR_LEX_REGEX_NO_ENTRY, "No regex entries found in the Lexer Spec");
      }
    }
    return lexRules;
  }

  private class RegexLineParser {

    private static final Pattern PATTERN = Pattern.compile("\"(.*?)\"\\s*\\{(.*?)}");

    /**
     * Parses a regular expression line from the lexer spec file.
     *
     * @param line the line to parse
     * @throws GeneratorException if the line is invalid
     */
    public void parseLine(String line) {
      Matcher matcher = PATTERN.matcher(line);
      if (!matcher.matches()) {
        throw GeneratorException.create(ErrorType.ERR_LEX_REGEX_ERR, "Invalid regex line: '%s' in the lexer specification at line %d", line, lineCount);
      }
      String regexStr = matcher.group(1);
      Regex regex = Regex.fromString(regexStr);
      String actionStr = matcher.group(2);
      LexRule.Action action = new LexRule.Action("{" + actionStr + "}");
      lexRules.add(new LexRule(null, null, regex, action));
    }
  }
}
