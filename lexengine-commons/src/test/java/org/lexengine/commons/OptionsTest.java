/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.commons;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.jar.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OptionsTest {
  @TempDir Path directory;

  @Test
  void toBuilderRetainsInputAndOutputPaths() {
    Options options =
        Options.parseFromArgs(
            new String[] {"-sp", "a spec.spec", "-d", "generated sources", "-sc", "template.tpl"});
    assertEquals(options, options.toBuilder().build());
    assertThrows(IllegalArgumentException.class, () -> Options.parseFromArgs(new String[] {"-d"}));
    assertThrows(
        IllegalArgumentException.class, () -> Options.parseFromArgs(new String[] {"--unknown"}));
  }

  @Test
  void opensClasspathResourcesInsideJars() throws Exception {
    Path jar = directory.resolve("resources.jar");
    try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
      output.putNextEntry(new JarEntry("example/template.txt"));
      output.write("resource with $ and \\".getBytes(StandardCharsets.UTF_8));
      output.closeEntry();
    }
    ClassLoader original = Thread.currentThread().getContextClassLoader();
    try (var loader = new URLClassLoader(new java.net.URL[] {jar.toUri().toURL()}, original)) {
      Thread.currentThread().setContextClassLoader(loader);
      try (var reader = Resources.reader("example/template.txt", StandardCharsets.UTF_8)) {
        java.io.StringWriter writer = new java.io.StringWriter();
        reader.transferTo(writer);
        assertEquals("resource with $ and \\", writer.toString());
      }
    } finally {
      Thread.currentThread().setContextClassLoader(original);
    }
  }
}
