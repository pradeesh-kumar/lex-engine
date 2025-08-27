/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import org.lexengine.commons.TemplateRenderer;
import org.lexengine.commons.error.ErrorType;
import org.lexengine.commons.error.GeneratorException;
import org.lexengine.commons.logging.Out;

/**
 * An interface representing a generator for lexical classes. Implementations of this interface
 * should provide functionality to generate lexical classes based on specific requirements or
 * configurations.
 */
public interface LexClassGenerator {

  /** Generates the lexical class according to the implementation's logic. */
  void generate();
}

/**
 * A generator for Lexer Class based on a deterministic finite automaton (DFA).
 *
 * <p>This class generates a Lexer Class based on the provided DFA and lexical specification. It
 * uses a table-based approach to represent the DFA's transition table and final states.
 */
class TableBasedLexClassGenerator implements LexClassGenerator {

  private static final String COMMA = ", ";
  private static final String NEW_LINE_STR = System.lineSeparator();

  /** The DFA used to generate the lexical class. */
  private final Dfa dfa;

  /** The lexical specification for the generated class. */
  private final LexSpec lexSpec;

  /** Output directory where the generated class will be written. */
  private final Path outDir;

  /** Template file for the scanner class. */
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

  /** Generates the Lexer Class based on the provided DFA and lexical specification. */
  public void generate() {
    try {
      Path outFile = outDir.resolve(lexSpec.lexClassName() + ".java");
      Out.info("Generating the class file at %s", outFile);
      TemplateRenderer renderer = new TemplateRenderer(scannerClassTemplate, prepareAttributes());
      renderer.renderToFile(outFile);
      Out.info("Generated lexer class file at %s", outFile);
    } catch (IOException e) {
      Out.error("Error creating the class file!", e);
      throw GeneratorException.error(ErrorType.ERR_CLASS_GENERATE);
    }
  }

  /**
   * Prepares a map of attributes required for generating the Lexer Class.
   *
   * <p>This method populates a map with various attributes derived from the DFA and LexSpec,
   * including class name, package name, return type, method name, compressed transition table,
   * final states, start state, states count, alphabets count, switch cases for final states, and
   * alphabet index.
   *
   * <p>The prepared attributes are used to render a template for the Lexer Class.
   *
   * @return a map containing the prepared attributes
   */
  private Map<String, String> prepareAttributes() {
    Map<String, String> attributes = new HashMap<>();
    attributes.put("className", lexSpec.lexClassName());
    attributes.put("package", lexSpec.lexPackageName());
    attributes.put("returnType", lexSpec.returnType());
    attributes.put("methodName", lexSpec.methodName());
    attributes.put("compressedTransitionTbl", getCompressedTransitionTbl());
    attributes.put("finalStates", getFinalStates());
    attributes.put("startState", String.valueOf(dfa.startState()));
    attributes.put("statesCount", String.valueOf(dfa.statesCount()));
    attributes.put("alphabetsCount", String.valueOf(dfa.alphabetSize()));
    attributes.put("switchCases", getFinalStateSwitchCases());
    attributes.put("alphabetIndex", getAlphabetIndex());
    return attributes;
  }

  /**
   * Compresses the DFA's transition table into a base64-encoded string.
   *
   * <p>This method first serializes the transition table into a byte array using {@link
   * LexUtils#serialize2DArray(int[][])}. Then, it compresses the serialized data using {@link
   * LexUtils#compress(byte[])}, and finally encodes the compressed data into a base64 string using
   * {@link Base64#getEncoder()}.
   *
   * @return the compressed transition table as a base64-encoded string
   */
  private String getCompressedTransitionTbl() {
    int[][] transitionTbl = dfa.transitionTbl();
    byte[] serializedData = LexUtils.serialize2DArray(transitionTbl);
    try {
      byte[] compressedData = LexUtils.compress(serializedData);
      return Base64.getEncoder().encodeToString(compressedData);
    } catch (IOException e) {
      Out.error("Error while compressing the transition table!", e);
      throw GeneratorException.error(ErrorType.ERR_CLASS_GENERATE);
    }
  }

  /**
   * Returns a comma-separated string representation of the final states in the DFA.
   *
   * <p>This method converts the final states bitset to an array of long integers, then formats each
   * value as a string and joins them together with commas.
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
   * <p>This method groups the actions by their values and constructs a string containing switch
   * cases for each group. Each case corresponds to a set of states that share the same action.
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
   * <p>This method iterates over the DFA's alphabet index entries and constructs a string
   * containing statements that populate a map with the alphabet characters and their corresponding
   * indices.
   *
   * @return a string containing the alphabet index mappings
   */
  private String getAlphabetIndex() {
    StringBuilder out = new StringBuilder();
    String format = "    map.put(%d, %d);\n";
    for (Map.Entry<Range, Integer> entry : dfa.alphabetIndex().entrySet()) {
      for (int c = entry.getKey().start(); c <= entry.getKey().end(); c++) {
        out.append(String.format(format, c, entry.getValue()));
      }
    }
    return out.toString();
  }
}
