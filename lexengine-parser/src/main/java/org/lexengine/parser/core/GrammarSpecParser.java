package org.lexengine.parser.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.lexengine.commons.logging.Out;
import org.lexengine.parser.error.ErrorType;
import org.lexengine.parser.error.ParserGeneratorException;

public class GrammarSpecParser {

  /** Divider string used to separate sections within the grammar spec file. */
  private static final String DIVIDER = "---";

  /** Comment prefix used to ignore lines starting with it. */
  private static final char COMMENT = '#';

  private final File grammarFile;
  private final GrammarSpec.Builder specBuilder;
  private LineParser[] lineParsers;
  private LineParser lineParser;
  private int lineCount;
  private int dividerCount;

  GrammarSpecParser(File grammarFile) {
    this.grammarFile = grammarFile;
    this.specBuilder = GrammarSpec.builder();
    this.lineParsers = new LineParser[] {new MetadataLineParser()};
    switchLineParser();
  }

  private void switchLineParser() {
    if (dividerCount >= lineParsers.length) {
      Out.error("Invalid grammar file! Unexpected divider found at line %d", lineCount);
      throw ParserGeneratorException.error(ErrorType.ERR_GRAMMAR_FILE_INVALID);
    }
    lineParser = lineParsers[dividerCount++];
  }

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

  private GrammarSpec validateGrammarSpec(GrammarSpec spec) {
    Objects.requireNonNull(spec.grammar(), "Grammar cannot be null");
    return spec;
  }

  /** For parsing line for various sections in the grammar file. */
  private interface LineParser {
    void parseLine(String line);
  }

  private class MetadataLineParser implements LineParser {

    @Override
    public void parseLine(String line) {}
  }
}
