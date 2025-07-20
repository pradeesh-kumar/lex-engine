/*
* Copyright (c) 2025 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.parser.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.lexengine.commons.logging.Out;
import org.lexengine.parser.error.ErrorType;
import org.lexengine.parser.error.ParserGeneratorException;

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
  GrammarSpecParser(File grammarFile) {
    this.grammarFile = grammarFile;
    this.specBuilder = GrammarSpec.builder();
    this.lineParsers = new LineParser[] {new MetadataLineParser(), new ProductionRuleLineParser()};
    switchLineParser();
  }

  /** Switches to the next LineParser based on the divider count. */
  private void switchLineParser() {
    if (dividerCount >= lineParsers.length) {
      Out.error("Invalid grammar file! Unexpected divider found at line %d", lineCount);
      throw ParserGeneratorException.error(ErrorType.ERR_GRAMMAR_FILE_INVALID);
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
    try (Stream<String> lines = Files.lines(grammarFile.toPath())) {
      lines
          .map(String::trim)
          .filter(Predicate.not(String::isEmpty))
          .forEach(
              line -> {
                lineCount++;
                if (DIVIDER.equals(line)) {
                  switchLineParser();
                } else if (COMMENT != line.charAt(0)) {
                  this.lineParser.parseLine(line);
                }
              });
    } catch (IOException e) {
      Out.error("Error reading the Grammar file %s", grammarFile);
      throw ParserGeneratorException.error(ErrorType.ERR_GRAMMAR_FILE_READ);
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
    Objects.requireNonNull(spec.grammar(), "Grammar cannot be null");
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
        Out.error("Invalid property line: '%s' in the grammar file at line %d", line, lineCount);
        throw ParserGeneratorException.error(ErrorType.ERR_PROPERTY_ERR);
      }
      String propName = line.substring(0, eqIdx).trim();
      String propValue = line.substring(eqIdx + 1).trim();
      if (propName.isEmpty() || propValue.isEmpty()) {
        Out.error("Invalid property line: '%s' in the grammar file at line %d", line, lineCount);
        throw ParserGeneratorException.error(ErrorType.ERR_PROPERTY_ERR);
      }
      switch (propName) {
        case "class" -> specBuilder.parserClassName(propValue);
        case "package" -> specBuilder.parserPackageName(propValue);
        default -> {
          Out.error("Invalid property line: '%s' in the grammar file at line %d!", line, lineCount);
          throw ParserGeneratorException.error(ErrorType.ERR_PROPERTY_ERR);
        }
      }
    }
  }

  /** LineParser implementation for parsing production rule lines. */
  private class ProductionRuleLineParser implements LineParser {

    private static final Pattern PATTERN = Pattern.compile("([^\\s]+)\\s*->\\s*(.*)");

    @Override
    public void parseLine(String line) {
      Matcher matcher = PATTERN.matcher(line);
      if (!matcher.matches()) {
        Out.error("Invalid syntax line: '%s' in the syntax file at line %d!", line, lineCount);
        throw ParserGeneratorException.error(ErrorType.ERR_PRODUCTION_RULE_INVALID);
      }
      // Map<Grammar.NonTerminal, List<List<Grammar.Symbol>>> productions = new HashMap<>();
      Grammar.NonTerminal leftHandSymbol = Grammar.NonTerminal.of(matcher.group(1));
      List<List<Grammar.Symbol>> rule = parseRule(matcher.group(2));
      specBuilder.addProduction(leftHandSymbol, rule);
    }

    private List<List<Grammar.Symbol>> parseRule(String rule) {
      // Further parse the rule into alternatives and symbols/terminals
      String[] alternatives = rule.split("\\|");
      List<List<Grammar.Symbol>> rules = new ArrayList<>();
      for (String alternative : alternatives) {
        List<Grammar.Symbol> symbolList = new ArrayList<>();
        alternative = alternative.trim();
        // Further parse the alternative into symbols/terminals
        String[] symbols = alternative.split("\\s+");
        for (String s : symbols) {
          symbolList.add(Grammar.Symbol.parse(s));
        }
        rules.add(symbolList);
      }
      return rules;
    }
  }
}
