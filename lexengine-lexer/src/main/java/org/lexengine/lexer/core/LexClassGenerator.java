/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import org.lexengine.lexer.error.ErrorType;
import org.lexengine.lexer.error.GeneratorException;
import org.lexengine.lexer.logging.Out;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface LexClassGenerator {
  void generate();
}

class TableBasedLexClassGenerator implements LexClassGenerator {

  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{(\\w+)}");
  private static final String COMMA = ", ";
  private static final char NEW_LINE = '\n';
  private static final String NEW_LINE_STR = System.lineSeparator();

  private final Dfa dfa;
  private final LexSpec lexSpec;
  private final Path outDir;
  private final Path scannerClassTemplate;

  public TableBasedLexClassGenerator(
      Dfa dfa, LexSpec lexSpec, Path outDir, Path scannerClassTemplate) {
    this.dfa = dfa;
    this.lexSpec = lexSpec;
    this.outDir = outDir;
    this.scannerClassTemplate = scannerClassTemplate;
  }

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
              outWriter.append(line, lastIndex, line.length()).append(NEW_LINE);
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

  private String getFinalStates() {
    return Arrays.stream(dfa.finalStates().toLongArray())
        .mapToObj(val -> String.format("%dL", val))
        .collect(Collectors.joining(COMMA));
  }

  private String getFinalStateSwitchCases() {
    Map<Integer, Action> actions = dfa.actions();
    String caseFormat = "        case %s -> %s";
    Set<Map.Entry<Action, List<Map.Entry<Integer, Action>>>> reverse =
        actions.entrySet().stream().collect(Collectors.groupingBy(Map.Entry::getValue)).entrySet();

    String cases = reverse.stream().map(e -> {
      String caseValues = e.getValue().stream().map(Map.Entry::getKey).map(String::valueOf).collect(Collectors.joining(", "));
      return String.format(caseFormat, caseValues, e.getKey().toString());
    }).collect(Collectors.joining(NEW_LINE_STR));
    return cases;
  }

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
