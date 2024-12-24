package org.lexengine.lexer.gentest;

import java.io.FileReader;
import java.util.Stack;
import java.util.BitSet;
import java.util.Map;
import java.util.HashMap;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

/**
 * A lexer implementation that tokenizes input from a given source path or reader.
 */
public class MyLexer {

  /**
   * Number of states used in the finite state machine.
   */
  private static final int STATES_COUNT = 112;

  /**
   * Number of alphabets used in the finite state machine.
   */
  private static final int ALPHABETS_COUNT = 50;

  /**
   * Starting state of the finite state machine.
   */
  private static final int START_STATE = 96;

  /**
   * Default buffer size for reading input.
   */
  private static final int DEFAULT_BUFFER_SIZE = 1024;

  private final int[][] transitionTbl;

  /**
   * Set of final states in the finite state machine.
   */
  private final BitSet finalStates;

  /**
   * Mapping of characters to their corresponding indices in the alphabet.
   */
  private final Map<Integer, Integer> alphabetIndex;

  /**
   * Buffer for reading input.
   */
  private final DynamicCharBuffer buffer;

  /**
   * Peeked token.
   */
  private Token peek;

  /**
   * Value associated with the current token.
   */
  private String value;

  /**
   * Current row number in the source.
   */
  private int curRow;

  /**
   * Current column number in the source.
   */
  private int curCol;

  /**
   * Constructs a new lexer instance from a file path.
   *
   * @param sourcePath path to the input file
   * @throws FileNotFoundException if the file does not exist
   */
  public MyLexer(String sourcePath) throws FileNotFoundException {
    this(new FileReader(sourcePath));
  }

  /**
   * Constructs a new lexer instance from a reader.
   *
   * @param reader input reader
   */
  public MyLexer(Reader reader) {
    this.buffer = new DynamicCharBuffer(reader, DEFAULT_BUFFER_SIZE);
    this.alphabetIndex = loadAlphabetIndex();
    this.finalStates = BitSet.valueOf(new long[] {-1073741824L, 281474976710655L});
    this.transitionTbl = decompress("H4sIAAAAAAAA/+3aaU8TYRiF4bLIKrIKIouAgICCqCA7CMoioCCr7EEQUAH//zdP05JMyhSmnXfmfebxnOROJ2UCczEN7QdiMdVrRGcezmtAT12er08+PkF1qBY9RjWoGlWhSlSBytEjVIYeolJUgopRESpEBegBykd5KBfl0EEHHXTQQQcddNBBBx100EEHHXTQQQcddNCh1uF1vcnSPR90dOh0cBzHRWVa/u52JeNkTMvrig46pDteC3b0Z2mS5jDVGyWOV8Id73z6pDjcGhDoGPRpkuLIpGdKHKkNKXAMezxPusPZiBLHTT+VOH4JcowbttlymOqFJcdkQJ6wHfc1JdzxIcvrk+ZI13TEHceGrt+2I7WPShzZ9MmCYzZgU1gOk80JcvQYtoXlmDBwrTOO4wVLjnifDVhs349MWoywYylLsy1Hs8/rleIw0ReLjq8BmaJ8P1YEO1Z92sJ2nPu8XimOTFtT4rivl4IcGwY8EhzxNj2c8y0CDq+dCnS03PG192g0Ax/HcRzHcYkdoUN0gPbRHtpFTWgb7aAT1I4u0HfUiVpRdyzx/7vP0V90iX7Hbr/v9jmO3zqOt1zOHUs+zjueW0brLudeoz/oyufvIKzPV/xc8n9Ny+tKi0PLeD9kTev9aAvwe2vfD9sXwHEe12HrB/8DJUn5a4BXAAA=");
    this.curRow = -1;
    this.curCol = -1;
  }

  /**
   * Checks if there are more tokens available.
   *
   * @return true if there are more tokens, false otherwise
   */
  public boolean hasNext() {
    if (peek == null) {
      peek = next();
    }
    return peek != null;
  }

  /**
   * Returns the next token without consuming it.
   *
   * @return the next token, or null if none available
   */
  public Token peek() {
    return this.peek;
  }

  /**
   * Returns the next token and consumes it.
   *
   * @return the next token, or null if none available
   */
  public Token next() {
    if (peek != null) {
      Token t = peek;
      peek = null;
      return t;
    }
    do {
      int state = advance();
      switch (state) {
        case 30 -> { return Token.of(Token.Type.PUBLIC); }
        case 33 -> { return Token.of(Token.Type.DIV); }
        case 32 -> { return Token.of(Token.Type.IF); }
        case 31 -> { return Token.of(Token.Type.FINAL); }
        case 35 -> { return Token.of(Token.Type.SEMICOLON); }
        case 34 -> { return Token.of(Token.Type.GREATEREQ); }
        case 36 -> { return Token.of(Token.Type.MUL); }
        case 38 -> { return Token.of(Token.Type.DOT); }
        case 37 -> { return Token.of(Token.Type.LESSEQ); }
        case 39 -> { return Token.of(Token.Type.NEW); }
        case 40, 41 -> { return Token.integer(value()); }
        case 42 -> { return Token.of(Token.Type.PRIVATE); }
        case 43 -> { return Token.of(Token.Type.THROW); }
        case 44 -> { return Token.of(Token.Type.CLASS); }
        case 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86 -> { return Token.identifier(value()); }
        case 87 -> { return Token.of(Token.Type.PERCENTAGE); }
        case 88 -> { return Token.of(Token.Type.SUB); }
        case 89 -> { return Token.of(Token.Type.OPEN_PAREN); }
        case 90, 91, 92, 93, 94, 95, 96 -> { /* do nothing */ }
        case 97 -> { return Token.of(Token.Type.PACKAGE); }
        case 98 -> { return Token.of(Token.Type.DOUBLE_OR); }
        case 99 -> { return Token.of(Token.Type.CLOSE_PAREN); }
        case 100 -> { return Token.of(Token.Type.THIS); }
        case 101 -> { return Token.of(Token.Type.IMPORT); }
        case 103 -> { return Token.of(Token.Type.STATIC); }
        case 102 -> { return Token.string(value()); }
        case 104 -> { return Token.of(Token.Type.ADD); }
        case 105 -> { return Token.of(Token.Type.INT); }
        case 106 -> { return Token.of(Token.Type.GREATER); }
        case 108 -> { return Token.of(Token.Type.EQ); }
        case 107 -> { return Token.of(Token.Type.OR); }
        case 110 -> { return Token.of(Token.Type.OPEN_BRACE); }
        case 109 -> { return Token.of(Token.Type.CLOSE_BRACE); }
        case 111 -> { return Token.of(Token.Type.LESS); }
        case -1 -> { return null; }
        default -> throw new LexerException("Unrecognized state " + state);
      }
    } while (true);
  }

  /**
   * Returns the value associated with the current token.
   *
   * @return the token value
   */
  public String value() {
    return this.value;
  }

  /**
   * Returns the current row number.
   *
   * @return the current row number
   */
  public int currentRow() {
    return this.curRow;
  }

  /**
   * Returns the current column number.
   *
   * @return the current column number
   */
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
        throw new LexerException(String.format("Invalid character '%c' found in the source", curCh));
      }
      int nextSt = transitionTbl[curSt][index];
      if (finalStates.get(nextSt)) stStack.clear();
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
      throw new LexerException(String.format("Cannot resolve symbol '%s'", buffer.getStringTillCurrent()));
    }
    while (!finalStates.get(stStack.peek())) {
      stStack.pop();
      buffer.rollback();
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


  /**
   * Exception thrown when an error occurs during lexing.
   */
  public static class LexerException extends RuntimeException {
    public LexerException(String message) {
      super(message);
    }

    public LexerException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}

/**
 * A dynamic character buffer that reads characters from an underlying {@link Reader} and stores them in a
 * dynamically-sized array. This allows for efficient reading and manipulation of large amounts of text data.
 */
class DynamicCharBuffer {

  /**
   * Default initial capacity of the buffer.
   */
  private static final int DEFAULT_BUFFER_SIZE = 256;

  /**
   * Underlying reader providing the source of characters.
   */
  private final Reader reader;

  /**
   * Current buffer holding the characters.
   */
  private char[] buffer;

  /**
   * Number of valid characters currently stored in the buffer.
   */
  private int length;

  /**
   * Index into the buffer where the next character will be returned from.
   */
  private int index;

  /**
   * Starting index within the buffer where the current "window" begins.
   */
  private int startIndex;

  /**
   * Flag indicating whether the end-of-file has been reached on the underlying reader.
   */
  private boolean eof;

  /**
   * Initial capacity specified when creating the buffer.
   */
  private final int initialCapacity;

  /**
   * Constructs a new DynamicCharBuffer instance with the default initial capacity.
   *
   * @param reader the underlying reader to read characters from
   */
  public DynamicCharBuffer(Reader reader) {
    this(reader, DEFAULT_BUFFER_SIZE);
  }

  /**
   * Constructs a new DynamicCharBuffer instance with the specified initial capacity.
   *
   * @param reader         the underlying reader to read characters from
   * @param initialCapacity the initial capacity of the buffer
   * @throws IllegalArgumentException if the initial capacity is less than or equal to zero
   */
  public DynamicCharBuffer(Reader reader, int initialCapacity) {
    if (initialCapacity <= 0) {
      throw new IllegalArgumentException("Capacity must be greater than 0");
    }
    this.initialCapacity = initialCapacity;
    this.reader = reader;
    this.buffer = null;
    this.index = 0;
    this.startIndex = 0;
    this.length = 0;
    this.eof = false;
    loadBufferIfRequired();
  }

  /**
   * Returns whether there are more characters available in the buffer.
   *
   * @return true if there are more characters available, false otherwise
   */
  public boolean hasNext() {
    loadBufferIfRequired();
    return index < length || !eof;
  }

  /**
   * Returns the next character from the buffer without removing it.
   *
   * @return the next character, or '\0' if no more characters are available
   */
  public char next() {
    if (!hasNext()) {
      return '\0';
    }
    return buffer[index++];
  }

  /**
   * Peeks at the next character in the buffer without advancing the index.
   *
   * @return the next character, or '\0' if no more characters are available
   */
  public char peek() {
    if (!hasNext()) {
      return '\0';
    }
    return buffer[index];
  }

  /**
   * Returns the number of valid characters currently stored in the buffer.
   *
   * @return the number of valid characters
   */
  public int size() {
    return length;
  }

  /**
   * Returns the current capacity of the buffer.
   *
   * @return the current capacity
   */
  public int capacity() {
    return buffer.length;
  }

  /**
   * Rolls back the index by one position, effectively undoing the last call to {@link #next()}.
   */
  public void rollback() {
    if (index >= startIndex) {
      --index;
    }
  }

  /**
   * Clears all characters up to the current index, resetting the start index.
   */
  public void clearTillCurrent() {
    this.startIndex = index;
  }

  /**
   * Returns a string containing all characters between the start index and the current index.
   *
   * @return the extracted string
   */
  public String getStringTillCurrent() {
    return new String(buffer, startIndex, index - startIndex);
  }

  /**
   * Loads more characters into the buffer if necessary.
   */
  private void loadBufferIfRequired() {
    if (eof || index < length) {
      return;
    }
    int newCapacity = length == 0 || startIndex > (length >> 1) ? initialCapacity : length * 2;
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
        reader.close();
        return;
      }
      buffer = newBuffer;
      length = length + readSize;
    } catch (IOException e) {
      throw new DynamicBufferException("Error while reading from buffer!", e);
    }
  }

  /**
   * Custom exception thrown when an error occurs during buffer operations.
   */
  public static class DynamicBufferException extends RuntimeException {

    /**
     * Constructs a new DynamicBufferException instance.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public DynamicBufferException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
