/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Utility class providing helper methods for lexical analysis tasks. */
public final class LexUtils {

  /**
   * Extracts alphabets from a list of regular expressions and adds them to a disjoint set of
   * language alphabets.
   *
   * @param regexActions a list of regular expression actions
   * @param languageAlphabets a disjoint set of language alphabets
   */
  static void extractAlphabetsFromRegex(
      List<RegexAction> regexActions, DisjointIntSet languageAlphabets) {
    regexActions.stream()
        .map(RegexAction::regex)
        .map(Regex::extractAlphabets)
        .flatMap(List::stream)
        .forEach(languageAlphabets::add);
  }

  /**
   * Creates an index mapping ranges to unique integers.
   *
   * @param ranges a list of ranges
   * @return a map where each range is associated with a unique integer index
   */
  static Map<Range, Integer> createAlphabetsIndex(List<Range> ranges) {
    Map<Range, Integer> alphabetIndex = new HashMap<>(ranges.size());
    int index = 0;
    for (var range : ranges) {
      alphabetIndex.put(range, index++);
    }
    return alphabetIndex;
  }

  /**
   * Serializes a 2D array into a byte array.
   *
   * This method takes a 2D array of integers as input and returns its serialized form as a byte array.
   * The serialization is done using a ByteBuffer, where each integer in the 2D array is written as four bytes.
   *
   * @param array the 2D array to be serialized
   * @return the serialized byte array representation of the input 2D array
   */
  public static byte[] serialize2DArray(int[][] array) {
    ByteBuffer buffer = ByteBuffer.allocate(array.length * array[0].length * 4);
    for (int[] row : array) {
      for (int value : row) {
        buffer.putInt(value);
      }
    }
    return buffer.array();
  }

  /**
   * Compresses the given byte array using GZIP compression.
   *
   * This method takes a byte array as input, compresses it using GZIP, and returns the compressed data as a new byte array.
   *
   * @param data the byte array to be compressed
   * @return the compressed byte array
   * @throws IOException if an I/O error occurs during compression
   */
  public static byte[] compress(byte[] data) throws IOException {
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream(data.length);
    try (GZIPOutputStream gzipOS = new GZIPOutputStream(byteStream)) {
      gzipOS.write(data);
    }
    return byteStream.toByteArray();
  }

  /**
   * Decompresses the given base64-encoded string representing a compressed 2D array.
   *
   * This method first decodes the base64 string into a byte array, then decompresses it using GZIP,
   * and finally deserializes the resulting byte array back into a 2D array.
   *
   * @param base64Data the base64-encoded string representing the compressed 2D array
   * @param rows the number of rows in the original 2D array
   * @param cols the number of columns in the original 2D array
   * @return the decompressed 2D array
   * @throws RuntimeException if an I/O error occurs during decompression or deserialization
   */
  public int[][] decompress(String base64Data, int rows, int cols) {
    byte[] compressedData = Base64.getDecoder().decode(base64Data);
    try {
      byte[] decompressedData = decompress(compressedData);
      return deserialize2DArray(decompressedData, rows, cols);
    } catch (IOException e) {
      throw new RuntimeException("Failed to decompress the state", e);
    }
  }

  /**
   * Decompresses the given byte array that was previously compressed using GZIP.
   *
   * This method reads the input byte array as a GZIP-compressed stream, decompresses it,
   * and returns the resulting uncompressed data as a new byte array.
   *
   * @param data the byte array containing the GZIP-compressed data
   * @return the decompressed byte array
   * @throws IOException if an I/O error occurs during decompression
   */
  public static byte[] decompress(byte[] data) throws IOException {
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

  /**
   * Deserializes a byte array into a 2D array of integers.
   *
   * This method assumes that the input byte array contains the serialized form of a 2D array,
   * where each integer is represented as four bytes. It uses a ByteBuffer to read the integers
   * from the byte array and constructs a new 2D array with the specified number of rows and columns.
   *
   * @param data the byte array containing the serialized 2D array
   * @param rows the expected number of rows in the 2D array
   * @param cols the expected number of columns in the 2D array
   * @return the deserialized 2D array
   * @throws BufferUnderflowException if the byte array does not contain enough data to fill the 2D array
   */
  public static int[][] deserialize2DArray(byte[] data, int rows, int cols) {
    int[][] array = new int[rows][cols];
    ByteBuffer buffer = ByteBuffer.wrap(data);
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        array[i][j] = buffer.getInt();
      }
    }
    return array;
  }
}
