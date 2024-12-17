package org.lexengine.lexer.core;

import org.lexengine.lexer.error.ErrorType;
import org.lexengine.lexer.error.GeneratorException;
import org.lexengine.lexer.logging.Out;

import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

public interface LexClassGenerator {
  void generate();
}

class TableBasedLexClassGenerator implements LexClassGenerator {

  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{(\\w+)}");
  private static final String COMMA = ", ";
  private static final char NEW_LINE = '\n';

  private final Dfa dfa;
  private final LexSpec lexSpec;
  private final Path outDir;
  private final Path scannerClassTemplate;

  public TableBasedLexClassGenerator(Dfa dfa, LexSpec lexSpec, Path outDir, Path scannerClassTemplate) {
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

    Path outFile = outDir.resolve(lexSpec.lexClassName() + ".java");
    try (FileWriter outWriter = new FileWriter(outFile.toFile());
         Stream<String> templateLines = Files.lines(scannerClassTemplate)) {
      templateLines.forEach(line -> {
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
    byte[] serializedData = serialize2DArray(transitionTbl);
    try {
      byte[] compressedData = compress(serializedData);
      return Base64.getEncoder().encodeToString(compressedData);
    } catch (IOException e) {
      Out.error("Error while compressing the transition table!", e);
      throw GeneratorException.error(ErrorType.ERR_SCANNER_CLASS_GENERATE);
    }
  }

  private static byte[] serialize2DArray(int[][] array) {
    ByteBuffer buffer = ByteBuffer.allocate(array.length * array[0].length * 4);
    for (int[] row : array) {
      for (int value : row) {
        buffer.putInt(value);
      }
    }
    return buffer.array();
  }

  private static byte[] compress(byte[] data) throws IOException {
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream(data.length);
    try (GZIPOutputStream gzipOS = new GZIPOutputStream(byteStream)) {
      gzipOS.write(data);
    }
    return byteStream.toByteArray();
  }

  private String getFinalStates() {
    return Arrays.stream(dfa.finalStates().toLongArray()).mapToObj(String::valueOf).collect(Collectors.joining(COMMA));
  }

  private String getFinalStateSwitchCases() {
    Map<Integer, Action> actions = dfa.actions();
    StringBuilder switchCases = new StringBuilder();
    String caseFormat = "      case %d:";
    String brk = "        break;";
    String actionIndent = "        ";
    Set<Map.Entry<Action, List<Map.Entry<Integer, Action>>>> reverse = actions.entrySet().stream().collect(Collectors.groupingBy(Map.Entry::getValue)).entrySet();
    for (Map.Entry<Action, List<Map.Entry<Integer, Action>>> entry : reverse) {
      String cases = entry.getValue().stream().map(val -> String.format(caseFormat, val.getKey())).collect(Collectors.joining("\n"));
      switchCases.append(cases).append(NEW_LINE).append(actionIndent).append(entry.getKey()).append(NEW_LINE).append(brk).append(NEW_LINE);
    }
    return switchCases.toString();
  }
}
