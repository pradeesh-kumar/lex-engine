/*
* Copyright (c) 2025 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.parser.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.lexengine.commons.error.ErrorType;
import org.lexengine.commons.error.GeneratorException;
import org.lexengine.commons.logging.Out;

/**
 * The GrammarSpecParser class is responsible for parsing a grammar specification file and returns a
 * GrammarSpec object.
 *
 * <p>The grammar file is expected to be in a specific format, with different sections separated by
 * a divider ("---"). The parser uses different LineParsers to parse each section.
 */
public class GrammarSpecParser {

  /** Divider string used to separate sections within the grammar spec file. */
  private static final String DIVIDER = "---";

  /** Comment prefix used to ignore lines starting with it. */
  private static final char COMMENT = '#';

  private final File grammarFile;
  private final GrammarSpec.Builder specBuilder;
  private final LineParser[] lineParsers;
  private LineParser lineParser;
  private int lineCount;
  private int dividerCount;

  /**
   * Constructs a new GrammarSpecParser instance with the given grammar file.
   *
   * @param grammarFile the grammar file to be parsed
   */
  public GrammarSpecParser(File grammarFile) {
    this.grammarFile = grammarFile;
    this.specBuilder = GrammarSpec.builder();
    this.lineParsers = new LineParser[] {new MetadataLineParser(), new ProductionRuleLineParser()};
    switchLineParser();
  }

  /** Switches to the next LineParser based on the divider count. */
  private void switchLineParser() {
    if (dividerCount >= lineParsers.length) {
      Out.error("Invalid grammar file! Unexpected divider found at line %d", lineCount);
      throw GeneratorException.create(
          ErrorType.ERR_GRAMMAR_FILE_INVALID, "Unexpected section separator at line %d", lineCount);
    }
    lineParser = lineParsers[dividerCount++];
  }

  /**
   * Parses the grammar file and returns a GrammarSpec object.
   *
   * @return the parsed GrammarSpec object
   */
  public GrammarSpec parse() {
    Out.debug("Parsing the Grammar file %s", grammarFile);
    try {
      String source = Files.readString(grammarFile.toPath());
      if (grammarFile.getName().endsWith(".lg")
          || source.lines().filter(line -> line.trim().equals("---")).count() == 2) {
        return new GrammarV2Parser(new java.io.StringReader(source)).parse().toGrammarSpec();
      }
    } catch (IOException e) {
      throw GeneratorException.create(
          ErrorType.ERR_GRAMMAR_FILE_READ, e, "Cannot read grammar %s", grammarFile);
    }
    try (Stream<String> lines = Files.lines(grammarFile.toPath())) {
      lines
          .map(String::trim)
          .forEach(
              line -> {
                lineCount++;
                if (DIVIDER.equals(line)) {
                  switchLineParser();
                } else if (!line.isEmpty() && COMMENT != line.charAt(0)) {
                  this.lineParser.parseLine(line);
                }
              });
    } catch (IOException e) {
      throw GeneratorException.create(
          ErrorType.ERR_GRAMMAR_FILE_READ, "Error reading the Grammar file %s", grammarFile);
    }
    return validateGrammarSpec(specBuilder.build());
  }

  /**
   * Validates the parsed GrammarSpec object.
   *
   * @param spec the GrammarSpec object to be validated
   * @return the validated GrammarSpec object
   */
  private GrammarSpec validateGrammarSpec(GrammarSpec spec) {
    Grammar grammar = spec.grammar();
    Objects.requireNonNull(grammar, "Grammar cannot be null");
    Objects.requireNonNull(grammar.startSymbol(), "Start symbol cannot be null");
    Grammar.ProductionMap productions = grammar.productions();
    Objects.requireNonNull(productions, "productions cannot be null");
    if (productions.isEmpty()) {
      throw GeneratorException.create(
          ErrorType.ERR_GRAMMAR_FILE_EMPTY_PRODUCTION, "Productions cannot be empty");
    }
    productions.validate();
    return spec;
  }

  /** For parsing line for various sections in the grammar file. */
  private interface LineParser {
    void parseLine(String line);
  }

  /** LineParser implementation for parsing metadata lines. */
  private class MetadataLineParser implements LineParser {

    @Override
    public void parseLine(String line) {
      int eqIdx = line.indexOf('=');
      if (eqIdx == -1) {
        throw GeneratorException.create(
            ErrorType.ERR_PARSER_PROPERTY_ERR,
            "Invalid property line: '%s' in the grammar file at line %d",
            line,
            lineCount);
      }
      String propName = line.substring(0, eqIdx).trim();
      String propValue = line.substring(eqIdx + 1).trim();
      if (propName.isEmpty() || propValue.isEmpty()) {
        throw GeneratorException.create(
            ErrorType.ERR_PARSER_PROPERTY_ERR,
            "Invalid property line: '%s' in the grammar file at line %d",
            line,
            lineCount);
      }
      switch (propName) {
        case "class" -> specBuilder.parserClassName(propValue);
        case "package" -> specBuilder.parserPackageName(propValue);
        default -> {
          throw GeneratorException.create(
              ErrorType.ERR_PARSER_PROPERTY_ERR,
              "Invalid property line: '%s' in the grammar file at line %d",
              line,
              lineCount);
        }
      }
    }
  }

  /** LineParser implementation for parsing production rule lines. */
  private class ProductionRuleLineParser implements LineParser {

    private static final Pattern PATTERN_PRODUCTION = Pattern.compile("([^\\s]+)\\s*->\\s*(.*)");
    private static final Pattern PATTERN_RULE = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*\'*|\\S");

    @Override
    public void parseLine(String line) {
      Matcher matcher = PATTERN_PRODUCTION.matcher(line);
      if (!matcher.matches()) {
        throw GeneratorException.create(
            ErrorType.ERR_PARSER_PRODUCTION_RULE_INVALID,
            "Invalid syntax line: '%s' in the grammar file at line %d!",
            line,
            lineCount);
      }
      Grammar.NonTerminal lhs = Grammar.NonTerminal.of(matcher.group(1));
      List<Grammar.Alternative> rule = parseRule(matcher.group(2));
      specBuilder.addProduction(lhs, rule);
    }

    private List<Grammar.Alternative> parseRule(String rule) {
      // Further parse the rule into alternatives and symbols/terminals
      String[] alternatives = rule.split("\\|", -1);
      List<Grammar.Alternative> rules = new ArrayList<>();
      for (String alternative : alternatives) {
        List<Grammar.Symbol> symbolList = new ArrayList<>();
        alternative = alternative.trim();
        // Further parse the alternative into symbols/terminals
        Matcher symbolMatcher = PATTERN_RULE.matcher(alternative);
        boolean foundEpsilon = false;
        while (symbolMatcher.find()) {
          Grammar.Symbol symbol = Grammar.Symbol.parse(symbolMatcher.group());
          if (symbol instanceof Grammar.Terminal terminal && terminal.isEpsilon()) {
            if (foundEpsilon) {
              throw GeneratorException.create(
                  ErrorType.ERR_PARSER_MULTI_EPSILON,
                  "Invalid production rule: '%s' in the grammar file at line %d! Multiple epsilons not allowed within an alternative",
                  rule,
                  lineCount);
            }
            foundEpsilon = true;
          }
          symbolList.add(symbol);
        }
        rules.add(Grammar.Alternative.create(symbolList));
      }
      return rules;
    }
  }
}
