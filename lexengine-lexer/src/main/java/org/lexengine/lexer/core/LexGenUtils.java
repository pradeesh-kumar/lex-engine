/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Utility class providing helper methods for lexical analysis tasks. */
public final class LexGenUtils {

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
   * Creates an index mapping intervals to unique integers.
   *
   * @param intervals a list of intervals
   * @return a map where each interval is associated with a unique integer index
   */
  static Map<Interval, Integer> createAlphabetsIndex(List<Interval> intervals) {
    Map<Interval, Integer> alphabetIndex = new HashMap<>(intervals.size());
    int index = 0;
    for (var interval : intervals) {
      alphabetIndex.put(interval, index++);
    }
    return alphabetIndex;
  }

  public static byte[] serialize2DArray(int[][] array) {
    ByteBuffer buffer = ByteBuffer.allocate(array.length * array[0].length * 4);
    for (int[] row : array) {
      for (int value : row) {
        buffer.putInt(value);
      }
    }
    return buffer.array();
  }

  public static byte[] compress(byte[] data) throws IOException {
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream(data.length);
    try (GZIPOutputStream gzipOS = new GZIPOutputStream(byteStream)) {
      gzipOS.write(data);
    }
    return byteStream.toByteArray();
  }

  public int[][] decompress(String base64Data, int rows, int cols) {
    byte[] compressedData = Base64.getDecoder().decode(base64Data);
    try {
      byte[] decompressedData = decompress(compressedData);
      return deserialize2DArray(decompressedData, rows, cols);
    } catch (IOException e) {
      throw new RuntimeException("Failed to decompress the state", e);
    }
  }

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
