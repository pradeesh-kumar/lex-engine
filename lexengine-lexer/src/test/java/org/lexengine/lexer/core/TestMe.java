/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

public class TestMe {

  @Test
  public void test() {
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    CharBuffer charBuffer = CharBuffer.allocate(10);

    Scanner sc = new Scanner(reader);

    //reader.read(charBuffer);
  }
}
