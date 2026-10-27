/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.parser.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.tools.ToolProvider;

/** Compiles generated Java in isolation, so missing runtime dependencies cannot be hidden. */
final class GeneratedParser implements AutoCloseable {
  final ParserGenerator generator;
  final Class<?> parserClass;
  private final URLClassLoader loader;

  private GeneratedParser(ParserGenerator generator, Class<?> parserClass, URLClassLoader loader) {
    this.generator = generator;
    this.parserClass = parserClass;
    this.loader = loader;
  }

  static GeneratedParser compile(
      Path directory, String grammar, ParserGenerator.ParserType type, boolean combined)
      throws Exception {
    Files.createDirectories(directory);
    Path file = directory.resolve(combined ? "input.lg" : "input.grammar");
    Files.writeString(file, grammar);
    ParserGenerator generator =
        ParserGenerator.Factory.create(
            file.toFile(), type, directory.resolve("generated sources"), null);
    generator.generate();
    var compiler = ToolProvider.getSystemJavaCompiler();
    assertNotNull(compiler, "Run tests with a JDK");
    ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
    int result =
        compiler.run(
            null,
            diagnostics,
            diagnostics,
            "--release",
            "21",
            "-encoding",
            "UTF-8",
            "-d",
            directory.toString(),
            generator.details().sourceFile().toString());
    assertEquals(0, result, diagnostics.toString(StandardCharsets.UTF_8));
    URLClassLoader loader =
        new URLClassLoader(
            new java.net.URL[] {directory.toUri().toURL()}, ClassLoader.getPlatformClassLoader());
    String packageName = generator.grammarSpec().parserPackageName();
    String name =
        (packageName.isEmpty() ? "" : packageName + ".")
            + generator.grammarSpec().parserClassName();
    return new GeneratedParser(generator, loader.loadClass(name), loader);
  }

  Object parseTokens(String... names) throws Exception {
    List<Object> tokens = new ArrayList<>();
    var tokenClass = loader.loadClass(parserClass.getName() + "$Token");
    for (String name : names)
      tokens.add(tokenClass.getConstructor(String.class, String.class).newInstance(name, name));
    return parse(parserClass.getConstructor(List.class).newInstance(tokens));
  }

  Object parseText(String text) throws Exception {
    return parse(parserClass.getConstructor(String.class).newInstance(text));
  }

  private Object parse(Object parser) throws Exception {
    try {
      return parserClass.getMethod("parse").invoke(parser);
    } catch (InvocationTargetException ex) {
      if (ex.getCause() instanceof Exception cause) throw cause;
      throw ex;
    }
  }

  static Object property(Object target, String name) throws Exception {
    return target.getClass().getMethod(name).invoke(target);
  }

  static Object root(Object tree) throws Exception {
    return property(tree, "root");
  }

  static List<String> leaves(Object tree) throws Exception {
    List<String> result = new ArrayList<>();
    collect(root(tree), result);
    return result;
  }

  private static void collect(Object node, List<String> values) throws Exception {
    if (property(node, "value") instanceof String value) values.add(value);
    for (Object child : (List<?>) property(node, "children")) collect(child, values);
  }

  @Override
  public void close() throws Exception {
    loader.close();
  }
}
