/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.util;

import java.io.IOException;
import java.io.Reader;

/**
 * A dynamic character buffer that reads characters from an underlying {@link Reader} and stores them in a
 * dynamically-sized array. This allows for efficient reading and manipulation of large amounts of text data.
 */
public class DynamicCharBuffer {

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
