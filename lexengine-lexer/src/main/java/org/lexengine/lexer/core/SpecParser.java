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
import org.lexengine.lexer.error.ErrorType;
import org.lexengine.lexer.error.GeneratorException;
import org.lexengine.lexer.logging.Out;

/** A parser for lexer spec files that extracts regular expressions and actions from the file. */
public class SpecParser {

  /** Divider string used to separate sections within the lexer spec file. */
  private static final String DIVIDER = "---";

  /** Comment prefix used to ignore lines starting with it. */
  private static final char COMMENT = '#';

  private final File specFile;
  private final LexSpec.Builder specBuilder;

  /** Flag indicating whether we are currently parsing the regex section. */
  private boolean regex;

  /**
   * Constructs a new SpecParser instance for the specified lexer spec file.
   *
   * @param specFile the lexer specification file to parse
   */
  SpecParser(File specFile) {
    this.specFile = specFile;
    this.regex = false;
    this.specBuilder = new LexSpec.Builder();
  }

  /**
   * Parses the lexer spec file and extracts regular expressions and actions.
   *
   * @throws GeneratorException if an error occurs during parsing
   */
  public LexSpec parseSpec() {
    Out.debug("Parsing the Lexer Spec file %s", specFile);
    try (Stream<String> lines = Files.lines(specFile.toPath())) {
      lines
          .map(String::trim)
          .filter(Predicate.not(String::isEmpty))
          .forEach(
              line -> {
                if (DIVIDER.equals(line)) {
                  regex = true;
                } else if (COMMENT != line.charAt(0)) {
                  parseLine(line);
                }
              });
      if (specBuilder.regexActionList().isEmpty()) {
        Out.error("No regex entries found in the Lexer Spec file %s", specFile);
        throw GeneratorException.error(ErrorType.ERR_REGEX_NO_ENTRY);
      }
    } catch (IOException e) {
      Out.error("Error reading the Lexer Spec file %s", specFile);
      throw GeneratorException.error(ErrorType.ERR_SPEC_FILE_READ);
    }
    return specBuilder.build();
  }

  /**
   * Parses a single line from the Lexer Spec file.
   *
   * @param line the line to parse
   */
  private void parseLine(String line) {
    if (regex) {
      parseRegex(line);
    } else {
      parseProperty(line);
    }
  }

  /**
   * Parses a property line from the Lexer Spec file.
   *
   * @param line the line to parse
   * @throws GeneratorException if the line is invalid
   */
  private void parseProperty(String line) {
    int eqIdx = line.indexOf('=');
    if (eqIdx == -1) {
      Out.error("Invalid property line: '%s' in the lexer spec file!", line);
      throw GeneratorException.error(ErrorType.ERR_PROPERTY_ERR);
    }
    String propName = line.substring(0, eqIdx).trim();
    String propValue = line.substring(eqIdx + 1).trim();
    if (propName.isEmpty() || propValue.isEmpty()) {
      Out.error("Invalid property line: '%s' in the lexer spec file!", line);
      throw GeneratorException.error(ErrorType.ERR_PROPERTY_ERR);
    }
    switch (propName) {
      case "class" -> specBuilder.lexClassName(propValue);
      case "package" -> specBuilder.lexPackageName(propValue);
      case "methodName" -> specBuilder.methodName(propValue);
      case "returnType" -> specBuilder.returnType(propValue);
      default -> {
        Out.error("Invalid property line: '%s' in the lexer spec file!", line);
        throw GeneratorException.error(ErrorType.ERR_PROPERTY_ERR);
      }
    }
  }

  private static final Pattern REGEX_PATTERN = Pattern.compile("\".*\"");
  private static final Pattern ACTION_PATTERN = Pattern.compile("[{].*[}]");

  /**
   * Parses a regular expression line from the lexer spec file.
   *
   * @param line the line to parse
   * @throws GeneratorException if the line is invalid
   */
  private void parseRegex(String line) {
    Matcher regexMatcher = REGEX_PATTERN.matcher(line);
    if (!regexMatcher.find()) {
      Out.error("Invalid regex line: '%s' in the lexer spec file!", line);
      throw GeneratorException.error(ErrorType.ERR_REGEX_ERR);
    }
    String regexStr = regexMatcher.group();
    Regex regex = Regex.fromString(regexStr.substring(1, regexStr.length() - 1));
    Matcher actionMatcher = ACTION_PATTERN.matcher(line.substring(regexStr.length()));
    if (!actionMatcher.find()) {
      Out.error("Invalid action line: '%s' in the lexer spec file!", line);
      throw GeneratorException.error(ErrorType.ERR_REGEX_ERR);
    }
    Action action = new Action(actionMatcher.group());
    specBuilder.addRegexAction(new RegexAction(regex, action));
  }
}
