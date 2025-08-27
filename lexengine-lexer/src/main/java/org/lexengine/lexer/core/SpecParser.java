/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.lexengine.commons.error.ErrorType;
import org.lexengine.commons.error.GeneratorException;
import org.lexengine.commons.logging.Out;

/** A parser for lexer spec files that extracts regular expressions and actions from the file. */
public class SpecParser {

  /** Divider string used to separate sections within the lexer spec file. */
  private static final String DIVIDER = "---";

  /** Comment prefix used to ignore lines starting with it. */
  private static final char COMMENT = '#';

  private final File specFile;
  private final LexSpec.Builder specBuilder;
  private int lineCount;

  /** Currently activated line parser */
  private LineParser lineParser;

  /** Number of dividers encountered during spec-file's line reading */
  private int dividerCount;

  /** Array of line parsers at divider Index */
  private final LineParser[] lineParsers;

  /**
   * Constructs a new SpecParser instance for the specified lexer spec file.
   *
   * @param specFile the lexer specification file to parse
   */
  SpecParser(File specFile) {
    this.specFile = specFile;
    this.specBuilder = LexSpec.builder();
    this.lineParsers = new LineParser[] {new MetadataLineParser(), new RegexLineParser()};
    switchLineParser();
  }

  /**
   * Switches the current line parser to the next one based on the divider count.
   *
   * <p>If the divider count exceeds the number of available line parsers, it throws a
   * GeneratorException with an error type indicating an invalid spec file.
   *
   * @throws GeneratorException if the divider count exceeds the number of available line parsers
   */
  private void switchLineParser() {
    if (dividerCount >= lineParsers.length) {
      Out.error("Invalid spec file! Unexpected divider found at line %d", lineCount);
      throw GeneratorException.error(ErrorType.ERR_LEX_SPEC_FILE_INVALID);
    }
    lineParser = lineParsers[dividerCount++];
  }

  /**
   * Parses the lexer spec file and extracts regular expressions and actions.
   *
   * @throws GeneratorException if an error occurs during parsing
   */
  public LexSpec parse() {
    Out.debug("Parsing the Lexer Spec file %s", specFile);
    try (Stream<String> lines = Files.lines(specFile.toPath())) {
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
      if (specBuilder.regexActionList().isEmpty()) {
        Out.error("No regex entries found in the Lexer Spec file %s", specFile);
        throw GeneratorException.error(ErrorType.ERR_LEX_REGEX_NO_ENTRY);
      }
    } catch (IOException e) {
      Out.error("Error reading the Lexer Spec file %s", specFile);
      throw GeneratorException.error(ErrorType.ERR_LEX_SPEC_FILE_READ);
    }
    return specBuilder.build();
  }

  /** For parsing line for various sections in the spec file. */
  private interface LineParser {
    void parseLine(String line);
  }

  private class MetadataLineParser implements LineParser {

    /**
     * Parses a property line from the Lexer Spec file.
     *
     * @param line the line to parse
     * @throws GeneratorException if the line is invalid
     */
    @Override
    public void parseLine(String line) {
      int eqIdx = line.indexOf('=');
      if (eqIdx == -1) {
        Out.error("Invalid property line: '%s' in the lexer spec file at line %d", line, lineCount);
        throw GeneratorException.error(ErrorType.ERR_LEX_PROPERTY_ERR);
      }
      String propName = line.substring(0, eqIdx).trim();
      String propValue = line.substring(eqIdx + 1).trim();
      if (propName.isEmpty() || propValue.isEmpty()) {
        Out.error("Invalid property line: '%s' in the lexer spec file at line %d", line, lineCount);
        throw GeneratorException.error(ErrorType.ERR_LEX_PROPERTY_ERR);
      }
      switch (propName) {
        case "class" -> specBuilder.lexClassName(propValue);
        case "package" -> specBuilder.lexPackageName(propValue);
        case "methodName" -> specBuilder.methodName(propValue);
        case "returnType" -> specBuilder.returnType(propValue);
        default -> {
          Out.error(
              "Invalid property line: '%s' in the lexer spec file at line %d!", line, lineCount);
          throw GeneratorException.error(ErrorType.ERR_LEX_PROPERTY_ERR);
        }
      }
    }
  }

  private class RegexLineParser implements LineParser {

    private static final Pattern PATTERN = Pattern.compile("\"(.*?)\"\\s*\\{(.*?)}");

    /**
     * Parses a regular expression line from the lexer spec file.
     *
     * @param line the line to parse
     * @throws GeneratorException if the line is invalid
     */
    @Override
    public void parseLine(String line) {
      Matcher matcher = PATTERN.matcher(line);
      if (!matcher.matches()) {
        Out.error("Invalid regex line: '%s' in the lexer spec file at line %d!", line, lineCount);
        throw GeneratorException.error(ErrorType.ERR_LEX_REGEX_ERR);
      }
      String regexStr = matcher.group(1);
      Regex regex = Regex.fromString(regexStr);
      String actionStr = matcher.group(2);
      Action action = new Action("{" + actionStr + "}");
      specBuilder.addRegexAction(new RegexAction(regex, action));
    }
  }
}
