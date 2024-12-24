package org.lexengine.lexer.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class DynamicCharBufferTest {

  @Test
  public void testConstructorWithCustomBufferSize() {
    InputStream inputStream = new ByteArrayInputStream("Hello".getBytes(StandardCharsets.UTF_8));
    Reader reader = new InputStreamReader(inputStream);
    int customBufferSize = 10;

    DynamicCharBuffer dynamicCharBuffer = new DynamicCharBuffer(reader, customBufferSize);
    assertEquals(customBufferSize, dynamicCharBuffer.capacity());
  }

  @Test
  public void testHasNextWhenThereIsDataAvailable() {
    InputStream inputStream = new ByteArrayInputStream("Hello".getBytes(StandardCharsets.UTF_8));
    Reader reader = new InputStreamReader(inputStream);
    DynamicCharBuffer dynamicCharBuffer = new DynamicCharBuffer(reader);

    assertTrue(dynamicCharBuffer.hasNext());
  }

  @Test
  public void testHasNextWhenNoMoreDataIsAvailable() {
    InputStream inputStream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
    Reader reader = new InputStreamReader(inputStream);
    DynamicCharBuffer dynamicCharBuffer = new DynamicCharBuffer(reader);

    Assertions.assertFalse(dynamicCharBuffer.hasNext());
  }

  @Test
  public void testNextWhenThereIsDataAvailable() {
    InputStream inputStream = new ByteArrayInputStream("H".getBytes(StandardCharsets.UTF_8));
    Reader reader = new InputStreamReader(inputStream);
    DynamicCharBuffer dynamicCharBuffer = new DynamicCharBuffer(reader);

    assertEquals('H', dynamicCharBuffer.next());
  }

  @Test
  public void testNextWhenNoMoreDataIsAvailable() {
    InputStream inputStream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
    Reader reader = new InputStreamReader(inputStream);
    DynamicCharBuffer dynamicCharBuffer = new DynamicCharBuffer(reader);

    assertEquals('\0', dynamicCharBuffer.next());
  }

  @Test
  public void testPeekWhenThereIsDataAvailable() {
    InputStream inputStream = new ByteArrayInputStream("H".getBytes(StandardCharsets.UTF_8));
    Reader reader = new InputStreamReader(inputStream);
    DynamicCharBuffer dynamicCharBuffer = new DynamicCharBuffer(reader);

    assertEquals('H', dynamicCharBuffer.peek());
  }

  @Test
  public void testPeekWhenNoMoreDataIsAvailable() {
    InputStream inputStream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
    Reader reader = new InputStreamReader(inputStream);
    DynamicCharBuffer dynamicCharBuffer = new DynamicCharBuffer(reader);

    assertEquals('\0', dynamicCharBuffer.peek());
  }

  @Test
  public void testRollback() {
    InputStream inputStream = new ByteArrayInputStream("Hi".getBytes(StandardCharsets.UTF_8));
    Reader reader = new InputStreamReader(inputStream);
    DynamicCharBuffer dynamicCharBuffer = new DynamicCharBuffer(reader);
    assertEquals('H', dynamicCharBuffer.next());

    dynamicCharBuffer.rollback();
    assertEquals('H', dynamicCharBuffer.peek());
  }

  @Test
  public void testClearTillCurrent() {
    InputStream inputStream = new ByteArrayInputStream("Hi".getBytes(StandardCharsets.UTF_8));
    Reader reader = new InputStreamReader(inputStream);
    DynamicCharBuffer dynamicCharBuffer = new DynamicCharBuffer(reader);
    Assertions.assertEquals('H', dynamicCharBuffer.next());
    dynamicCharBuffer.clearTillCurrent();

    String stringTillCurrent = dynamicCharBuffer.getStringTillCurrent();
    assertEquals("", stringTillCurrent);
  }

  @Test
  public void testGetStringTillCurrent() {
    InputStream inputStream = new ByteArrayInputStream("Hi".getBytes(StandardCharsets.UTF_8));
    Reader reader = new InputStreamReader(inputStream);
    DynamicCharBuffer dynamicCharBuffer = new DynamicCharBuffer(reader);
    Assertions.assertEquals('H', dynamicCharBuffer.next());

    String stringTillCurrent = dynamicCharBuffer.getStringTillCurrent();
    assertEquals("H", stringTillCurrent);
  }

  @Test
  public void testLoadBufferIfRequiredWhenEofIsReached() {
    InputStream inputStream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
    Reader reader = new InputStreamReader(inputStream);
    DynamicCharBuffer dynamicCharBuffer = new DynamicCharBuffer(reader);

    Assertions.assertFalse(dynamicCharBuffer.hasNext());
  }

  @Test
  public void testInvalidInitialCapacity() {
    InputStream inputStream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
    Reader reader = new InputStreamReader(inputStream);
    int invalidInitialCapacity = 0;
    assertThrows(IllegalArgumentException.class, () -> new DynamicCharBuffer(reader, invalidInitialCapacity));
  }
}