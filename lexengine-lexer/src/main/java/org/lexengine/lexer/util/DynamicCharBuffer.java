/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.util;

import java.io.IOException;
import java.io.Reader;

public class DynamicCharBuffer {

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
    return index < length || !eof;
  }

  public char next() {
    loadBufferIfRequired();
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
    return buffer[index--];
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
    }
    this.startIndex = 0;
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
