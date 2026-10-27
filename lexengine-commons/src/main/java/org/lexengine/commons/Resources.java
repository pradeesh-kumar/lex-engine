/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.commons;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;

/** Opens user files first, falling back to classpath resources, including resources in JARs. */
public final class Resources {
  private Resources() {}

  public static Reader reader(String location, Charset encoding) throws IOException {
    Path file = Path.of(location);
    if (Files.isRegularFile(file)) return Files.newBufferedReader(file, encoding);
    String resource = location.startsWith("/") ? location.substring(1) : location;
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
    if (stream == null) throw new FileNotFoundException("File or resource not found: " + location);
    return new InputStreamReader(stream, encoding);
  }
}
