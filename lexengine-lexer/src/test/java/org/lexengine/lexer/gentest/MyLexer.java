/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.gentest;

import java.io.*;
import java.io.FileReader;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.zip.GZIPInputStream;

public class MyLexer {

  private static final int STATES_COUNT = 112;
  private static final int ALPHABETS_COUNT = 50;
  private static final int START_STATE = 80;
  private static final int DEFAULT_BUFFER_SIZE = 1024;

  private final int[][] transitionTbl;
  private final BitSet finalStates;
  private final Map<Integer, Integer> alphabetIndex;
  private final DynamicCharBuffer buffer;
  private Token peek;
  private String value;
  private int curRow;
  private int curCol;

  public MyLexer(String sourcePath) throws FileNotFoundException {
    this(new FileReader(sourcePath));
  }

  public MyLexer(Reader reader) {
    this.buffer = new DynamicCharBuffer(reader, DEFAULT_BUFFER_SIZE);
    this.alphabetIndex = loadAlphabetIndex();
    this.finalStates = BitSet.valueOf(new long[] {-1073741824L, 281474976710655L});
    this.transitionTbl =
        decompress(
            "H4sIAAAAAAAA/+3V51MTURjF4VBEioB0UIpIEaSDWJAqRTqjAtJ7N+D//83DTBx3QiKb3Xtz37xzzsxvsrPsZPZhE4hEVK8e/fJx3Uv0IsH5uthrLapB1agKVaIKVI7K0HNUikpQMXqGilAhKkD56CnKQ09QLspB2SiLDjrooIMOOuiggw466KCDDjrooIMOOuiggw61Dr9rjJXsvO0kOl4JdjQHNElzmOq1EseBcEdrSJ8UR6LaBDraQ5qkOFJpW4kjvg4Fjk6f10l3eHurxPG3b0ocPwU5egzbXDlMderI0WfJk27HY/ULdwwEvD9pjmQNZrhj3dD9u3bE906JI0jDDhzvLZvS5TDZB0GO74Zt6XL0GrjXIc/xJ0eO+0YMWFw/j1T6nMGO0YBmV47dkPcrxWGicYeOCUumTH4ek4IdUyFt6XbMh7xfKY5Um1bieKxzQY5ZAx4JjvvmfFzzJQMcfjsR6Lj5z8/eoK4UfC4dJuM4juPcbQ2tohW0jJbQImpAm+gH+o2u0AXaQ3foEt2iKDpEO+gInUUe/p1v8hy3eI6/Jri2O/b60XNuDM0kuHYL7aONkL8DLf8HtTi0jM+DszF+rmRN6/O4tvjeNqf1eXDBt2DgPfi54rjk0/L90OKI37HF97Y5rc+D47h/4/dc1qKxOE7l/gDS+pw8gFcAAA==");
    this.curRow = -1;
    this.curCol = -1;
  }

  public boolean hasNext() {
    if (peek == null) {
      peek = next();
    }
    return peek != null;
  }

  public Token peek() {
    return this.peek;
  }

  public Token next() {
    if (peek != null) {
      Token t = peek;
      peek = null;
      return t;
    }
    int state = 0;
    do {
      state = advance();
      switch (state) {
        case 30,
            31,
            32,
            33,
            34,
            35,
            36,
            37,
            38,
            39,
            40,
            41,
            42,
            43,
            44,
            45,
            46,
            47,
            48,
            49,
            50,
            51,
            52,
            53,
            54,
            55,
            56,
            57,
            58,
            59,
            60,
            61,
            62,
            63,
            64,
            65,
            66,
            67,
            68,
            69,
            70,
            71 -> {
          return Token.identifier(value());
        }
        case 74, 75, 76, 77, 78, 79, 80 -> {
          /* do nothing */
        }
        case 73 -> {
          return Token.of(Token.Type.DOUBLE_OR);
        }
        case 72 -> {
          return Token.of(Token.Type.STATIC);
        }
        case 83 -> {
          return Token.of(Token.Type.OPEN_PAREN);
        }
        case 82 -> {
          return Token.of(Token.Type.PRIVATE);
        }
        case 81 -> {
          return Token.of(Token.Type.IMPORT);
        }
        case 84 -> {
          return Token.of(Token.Type.PACKAGE);
        }
        case 85 -> {
          return Token.of(Token.Type.CLOSE_BRACE);
        }
        case 86 -> {
          return Token.of(Token.Type.PERCENTAGE);
        }
        case 87 -> {
          return Token.of(Token.Type.INT);
        }
        case 88 -> {
          return Token.of(Token.Type.OPEN_BRACE);
        }
        case 90 -> {
          return Token.of(Token.Type.LESS);
        }
        case 89 -> {
          return Token.of(Token.Type.FINAL);
        }
        case 91 -> {
          return Token.of(Token.Type.PUBLIC);
        }
        case 92 -> {
          return Token.of(Token.Type.SUB);
        }
        case 93 -> {
          return Token.of(Token.Type.OR);
        }
        case 94 -> {
          return Token.of(Token.Type.CLASS);
        }
        case 95 -> {
          return Token.of(Token.Type.SEMICOLON);
        }
        case 96 -> {
          return Token.of(Token.Type.EQ);
        }
        case 98 -> {
          return Token.of(Token.Type.THIS);
        }
        case 97 -> {
          return Token.of(Token.Type.GREATEREQ);
        }
        case 100 -> {
          return Token.of(Token.Type.GREATER);
        }
        case 99 -> {
          return Token.of(Token.Type.NEW);
        }
        case 101 -> {
          return Token.of(Token.Type.THROW);
        }
        case 102 -> {
          return Token.of(Token.Type.ADD);
        }
        case 104 -> {
          return Token.of(Token.Type.MUL);
        }
        case 103 -> {
          return Token.of(Token.Type.DIV);
        }
        case 105 -> {
          return Token.of(Token.Type.LESSEQ);
        }
        case 106 -> {
          return Token.of(Token.Type.IF);
        }
        case 110 -> {
          return Token.of(Token.Type.DOT);
        }
        case 108, 109 -> {
          return Token.integer(value());
        }
        case 107 -> {
          return Token.string(value());
        }
        case 111 -> {
          return Token.of(Token.Type.CLOSE_PAREN);
        }
        case -1 -> {
          return null;
        }
        default -> throw new LexerException("Unrecognized state " + state);
      }
    } while (state != -1);
    return null;
  }

  public String value() {
    return this.value;
  }

  public int currentRow() {
    return this.curRow;
  }

  public int currentCol() {
    return this.curCol;
  }

  private int advance() {
    if (!buffer.hasNext()) {
      return -1;
    }
    int curSt = START_STATE;
    Stack<Integer> stStack = new Stack<>();
    stStack.push(curSt);
    boolean foundFinalState = finalStates.get(curSt);
    while (buffer.hasNext()) {
      char curCh = buffer.next();
      if (curCh == '\n' || curCh == '\r') {
        curRow++;
      } else {
        curCol++;
      }
      Integer index = alphabetIndex.get((int) curCh);
      if (index == null) {
        throw new LexerException(
            String.format("Invalid character '%c' found in the source", curCh));
      }
      int nextSt = transitionTbl[curSt][index];
      stStack.push(nextSt);
      if (nextSt == 0) {
        return lookupFinalState(stStack, foundFinalState);
      }
      if (finalStates.get(nextSt)) {
        foundFinalState = true;
      }
      curSt = nextSt;
    }
    return lookupFinalState(stStack, foundFinalState);
  }

  private int lookupFinalState(Stack<Integer> stStack, boolean foundFinalState) {
    if (!foundFinalState) {
      throw new LexerException(
          String.format("Cannot resolve symbol '%s'", buffer.getStringTillCurrent()));
    }
    while (!finalStates.get(stStack.peek())) {
      stStack.pop();
      buffer.prev();
    }
    this.value = buffer.getStringTillCurrent();
    buffer.clearTillCurrent();
    return stStack.peek();
  }

  private int[][] decompress(String base64Data) {
    byte[] compressedData = Base64.getDecoder().decode(base64Data);
    try {
      byte[] decompressedData = decompress(compressedData);
      return deserialize2DArray(decompressedData, STATES_COUNT, ALPHABETS_COUNT);
    } catch (IOException e) {
      throw new LexerException("Failed to decompress the state", e);
    }
  }

  private static byte[] decompress(byte[] data) throws IOException {
    ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
    try (GZIPInputStream gzipIS = new GZIPInputStream(byteStream);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[1024];
      int len;
      while ((len = gzipIS.read(buffer)) != -1) {
        outputStream.write(buffer, 0, len);
      }
      return outputStream.toByteArray();
    }
  }

  private static int[][] deserialize2DArray(byte[] data, int rows, int cols) {
    int[][] array = new int[rows][cols];
    ByteBuffer buffer = ByteBuffer.wrap(data);
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        array[i][j] = buffer.getInt();
      }
    }
    return array;
  }

  private static Map<Integer, Integer> loadAlphabetIndex() {
    Map<Integer, Integer> map = new HashMap<>();
    map.put(46, 13);
    map.put(10, 2);
    map.put(34, 6);
    map.put(42, 10);
    map.put(62, 20);
    map.put(98, 24);
    map.put(102, 28);
    map.put(106, 32);
    map.put(110, 36);
    map.put(114, 40);
    map.put(118, 44);
    map.put(95, 22);
    map.put(59, 17);
    map.put(43, 11);
    map.put(47, 14);
    map.put(99, 25);
    map.put(103, 29);
    map.put(107, 33);
    map.put(111, 37);
    map.put(115, 41);
    map.put(119, 45);
    map.put(123, 47);
    map.put(40, 8);
    map.put(32, 5);
    map.put(8, 0);
    map.put(12, 3);
    map.put(48, 15);
    map.put(60, 18);
    map.put(100, 26);
    map.put(104, 30);
    map.put(108, 34);
    map.put(112, 38);
    map.put(116, 42);
    map.put(124, 48);
    map.put(49, 16);
    map.put(50, 16);
    map.put(51, 16);
    map.put(52, 16);
    map.put(53, 16);
    map.put(54, 16);
    map.put(55, 16);
    map.put(56, 16);
    map.put(57, 16);
    map.put(41, 9);
    map.put(9, 1);
    map.put(13, 4);
    map.put(37, 7);
    map.put(45, 12);
    map.put(61, 19);
    map.put(97, 23);
    map.put(101, 27);
    map.put(105, 31);
    map.put(109, 35);
    map.put(113, 39);
    map.put(117, 43);
    map.put(125, 49);
    map.put(65, 21);
    map.put(66, 21);
    map.put(67, 21);
    map.put(68, 21);
    map.put(69, 21);
    map.put(70, 21);
    map.put(71, 21);
    map.put(72, 21);
    map.put(73, 21);
    map.put(74, 21);
    map.put(75, 21);
    map.put(76, 21);
    map.put(77, 21);
    map.put(78, 21);
    map.put(79, 21);
    map.put(80, 21);
    map.put(81, 21);
    map.put(82, 21);
    map.put(83, 21);
    map.put(84, 21);
    map.put(85, 21);
    map.put(86, 21);
    map.put(87, 21);
    map.put(88, 21);
    map.put(89, 21);
    map.put(90, 21);
    map.put(120, 46);
    map.put(121, 46);
    map.put(122, 46);

    return Map.copyOf(map);
  }

  public static class LexerException extends RuntimeException {
    public LexerException(String message) {
      super(message);
    }

    public LexerException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}

class DynamicCharBuffer {

  private static final int DEFAULT_BUFFER_SIZE = 256;

  private final Reader reader;
  private char[] buffer;
  private int length;
  private int index;
  private int startIndex;
  private boolean eof;

  public DynamicCharBuffer(Reader reader) {
    this(reader, DEFAULT_BUFFER_SIZE);
  }

  public DynamicCharBuffer(Reader reader, int initialCapacity) {
    if (initialCapacity <= 0) {
      throw new IllegalArgumentException("Capacity must be greater than 0");
    }
    this.reader = reader;
    this.buffer = null;
    this.index = 0;
    this.startIndex = 0;
    this.length = 0;
    this.eof = false;
    loadBufferIfRequired();
  }

  public boolean hasNext() {
    loadBufferIfRequired();
    return index < length || !eof;
  }

  public char next() {
    if (!hasNext()) {
      return '\0';
    }
    return buffer[index++];
  }

  public char peek() {
    if (!hasNext()) {
      return '\0';
    }
    return buffer[index];
  }

  public int size() {
    return length;
  }

  public char prev() {
    if (index < 0) {
      return '\0';
    }
    return buffer[--index];
  }

  public void clearTillCurrent() {
    this.startIndex = index;
  }

  public String getStringTillCurrent() {
    return new String(buffer, startIndex, index - startIndex);
  }

  private void loadBufferIfRequired() {
    if (eof || index < length) {
      return;
    }
    int newCapacity = length == 0 || startIndex > (length >> 1) ? DEFAULT_BUFFER_SIZE : length * 2;
    char[] newBuffer = new char[newCapacity];
    if (buffer == null) {
      buffer = newBuffer;
    } else {
      System.arraycopy(buffer, startIndex, newBuffer, 0, length - startIndex);
      length -= startIndex;
      index -= startIndex;
      startIndex = 0;
    }
    try {
      int readSize = reader.read(newBuffer, length, buffer.length - length);
      if (readSize == -1) {
        eof = true;
        return;
      }
      buffer = newBuffer;
      length = length + readSize;
    } catch (IOException e) {
      throw new DynamicBufferException("Error while reading from buffer!", e);
    }
  }

  public static class DynamicBufferException extends RuntimeException {
    public DynamicBufferException(String message, Exception e) {
      super(message, e);
    }
  }
}
