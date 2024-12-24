/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.lexengine.lexer.error.ErrorType;
import org.lexengine.lexer.error.GeneratorException;
import org.lexengine.lexer.logging.Out;

/**
 * An interface representing a generator for lexical classes. Implementations of this interface should provide
 * functionality to generate lexical classes based on specific requirements or configurations.
 */
public interface LexClassGenerator {

  /**
   * Generates the lexical class according to the implementation's logic.
   */
  void generate();
}

/**
 * A generator for Lexer Class based on a deterministic finite automaton (DFA).
 *
 * This class generates a Lexer Class based on the provided DFA and lexical specification.
 * It uses a table-based approach to represent the DFA's transition table and final states.
 */
class TableBasedLexClassGenerator implements LexClassGenerator {

  /**
   * Regular expression pattern for matching placeholders in the template file.
   */
  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{(\\w+)}");

  private static final String COMMA = ", ";
  private static final String NEW_LINE_STR = System.lineSeparator();

  /**
   * The DFA used to generate the lexical class.
   */
  private final Dfa dfa;

  /**
   * The lexical specification for the generated class.
   */
  private final LexSpec lexSpec;

  /**
   * Output directory where the generated class will be written.
   */
  private final Path outDir;

  /**
   * Template file for the scanner class.
   */
  private final Path scannerClassTemplate;

  /**
   * Constructs a new TableBasedLexClassGenerator instance.
   *
   * @param dfa DFA used to generate the lexical class
   * @param lexSpec lexical specification for the generated class
   * @param outDir output directory where the generated class will be written
   * @param scannerClassTemplate template file for the scanner class
   */
  public TableBasedLexClassGenerator(
      Dfa dfa, LexSpec lexSpec, Path outDir, Path scannerClassTemplate) {
    this.dfa = dfa;
    this.lexSpec = lexSpec;
    this.outDir = outDir;
    this.scannerClassTemplate = scannerClassTemplate;
  }

  /**
   * Generates the Lexer Class based on the provided DFA and lexical specification.
   */
  public void generate() {
    Map<String, String> context = new HashMap<>();
    context.put("className", lexSpec.lexClassName());
    context.put("package", lexSpec.lexPackageName());
    context.put("returnType", lexSpec.returnType());
    context.put("methodName", lexSpec.methodName());
    context.put("compressedTransitionTbl", getCompressedTransitionTbl());
    context.put("finalStates", getFinalStates());
    context.put("startState", String.valueOf(dfa.startState()));
    context.put("statesCount", String.valueOf(dfa.statesCount()));
    context.put("alphabetsCount", String.valueOf(dfa.alphabetSize()));
    context.put("switchCases", getFinalStateSwitchCases());
    context.put("alphabetIndex", getAlphabetIndex());

    Path outFile = outDir.resolve(lexSpec.lexClassName() + ".java");
    try (FileWriter outWriter = new FileWriter(outFile.toFile());
        Stream<String> templateLines = Files.lines(scannerClassTemplate)) {
      templateLines.forEach(
          line -> {
            Matcher matcher = PLACEHOLDER_PATTERN.matcher(line);
            int lastIndex = 0;
            try {
              while (matcher.find()) {
                outWriter.append(line, lastIndex, matcher.start());
                String key = matcher.group(1);
                outWriter.append(context.getOrDefault(key, ""));
                lastIndex = matcher.end();
              }
              outWriter.append(line, lastIndex, line.length()).append(NEW_LINE_STR);
            } catch (IOException e) {
              Out.error("Error while generating scanner class!", e);
              throw GeneratorException.error(ErrorType.ERR_SCANNER_CLASS_GENERATE);
            }
          });
    } catch (IOException e) {
      Out.error("Error creating the scanner class file!", e);
      throw GeneratorException.error(ErrorType.ERR_SCANNER_CLASS_GENERATE);
    }
  }

  /**
   * Compresses the DFA's transition table into a base64-encoded string.
   *
   * This method first serializes the transition table into a byte array using
   * {@link LexGenUtils#serialize2DArray(int[][])}. Then, it compresses the serialized data using
   * {@link LexGenUtils#compress(byte[])}, and finally encodes the compressed data into a base64 string
   * using {@link Base64#getEncoder()}.
   *
   * @return the compressed transition table as a base64-encoded string
   */
  private String getCompressedTransitionTbl() {
    int[][] transitionTbl = dfa.transitionTbl();
    byte[] serializedData = LexGenUtils.serialize2DArray(transitionTbl);
    try {
      byte[] compressedData = LexGenUtils.compress(serializedData);
      return Base64.getEncoder().encodeToString(compressedData);
    } catch (IOException e) {
      Out.error("Error while compressing the transition table!", e);
      throw GeneratorException.error(ErrorType.ERR_SCANNER_CLASS_GENERATE);
    }
  }

  /**
   * Returns a comma-separated string representation of the final states in the DFA.
   *
   * This method converts the final states bitset to an array of long integers, then formats each value as a string
   * and joins them together with commas.
   *
   * @return a string containing the final states separated by commas
   */
  private String getFinalStates() {
    return Arrays.stream(dfa.finalStates().toLongArray())
        .mapToObj(val -> String.format("%dL", val))
        .collect(Collectors.joining(COMMA));
  }

  /**
   * Generates a string representation of switch cases for final states in the DFA.
   *
   * This method groups the actions by their values and constructs a string containing switch cases
   * for each group. Each case corresponds to a set of states that share the same action.
   *
   * @return a string containing the switch cases for final states
   */
  private String getFinalStateSwitchCases() {
    Map<Integer, Action> actions = dfa.actions();
    String caseFormat = "        case %s -> %s";
    Set<Map.Entry<Action, List<Map.Entry<Integer, Action>>>> reverse =
        actions.entrySet().stream().collect(Collectors.groupingBy(Map.Entry::getValue)).entrySet();

    return reverse.stream()
      .map(
          e -> {
            String caseValues =
                e.getValue().stream()
                    .map(Map.Entry::getKey)
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            return String.format(caseFormat, caseValues, e.getKey().toString());
          })
      .collect(Collectors.joining(NEW_LINE_STR));
  }

  /**
   * Returns a string representation of the alphabet index mappings.
   *
   * This method iterates over the DFA's alphabet index entries and constructs a string containing
   * statements that populate a map with the alphabet characters and their corresponding indices.
   *
   * @return a string containing the alphabet index mappings
   */
  private String getAlphabetIndex() {
    StringBuilder out = new StringBuilder();
    String format = "    map.put(%d, %d);\n";
    for (Map.Entry<Interval, Integer> entry : dfa.alphabetIndex().entrySet()) {
      for (int c = entry.getKey().start(); c <= entry.getKey().end(); c++) {
        out.append(String.format(format, c, entry.getValue()));
      }
    }
    return out.toString();
  }
}
