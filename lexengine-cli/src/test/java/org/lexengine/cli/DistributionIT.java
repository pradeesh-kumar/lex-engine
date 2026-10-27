/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Runs the packaged distribution after package, including resources inside the executable JAR. */
class DistributionIT {
  @TempDir Path directory;

  private record Result(int status, String out, String err) {}

  private Path distribution() {
    return Path.of(System.getProperty("lexengine.distributionDirectory"))
        .toAbsolutePath()
        .normalize();
  }

  private Result run(List<String> command, Path workingDirectory) throws Exception {
    return run(command, workingDirectory, true);
  }

  private Result run(List<String> command, Path workingDirectory, boolean javaHome)
      throws Exception {
    Path out = Files.createTempFile(directory, "out", ".txt");
    Path err = Files.createTempFile(directory, "err", ".txt");
    ProcessBuilder builder =
        new ProcessBuilder(command)
            .directory(workingDirectory.toFile())
            .redirectOutput(out.toFile())
            .redirectError(err.toFile());
    if (javaHome) builder.environment().put("JAVA_HOME", System.getProperty("java.home"));
    else builder.environment().remove("JAVA_HOME");
    Process process = builder.start();
    if (!process.waitFor(30, TimeUnit.SECONDS)) {
      process.destroyForcibly();
      fail("Command timed out: " + command);
    }
    return new Result(process.exitValue(), Files.readString(out), Files.readString(err));
  }

  private Result jar(String... args) throws Exception {
    List<String> command =
        new ArrayList<>(
            List.of(
                Path.of(System.getProperty("java.home"), "bin", windows() ? "java.exe" : "java")
                    .toString(),
                "-jar",
                distribution().resolve("lex-engine.jar").toString()));
    command.addAll(List.of(args));
    return run(command, directory);
  }

  private static boolean windows() {
    return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
  }

  private void success(Result result) {
    assertEquals(0, result.status(), result.out() + result.err());
  }

  @Test
  void executableJarContainsTheCompiledExamplesAndVersion() throws Exception {
    Result version = jar("--version");
    success(version);
    assertTrue(version.out().startsWith("Lex-Engine "));
    assertFalse(version.out().contains("development"));
    Result examples = jar("examples", "--all");
    success(examples);
    for (String name : List.of("arithmetic", "list", "config", "json", "legacy", "lr1"))
      assertTrue(examples.out().contains("Accepted " + name + " using"));
    Result bad = jar("unknown");
    assertEquals(1, bad.status());
    assertTrue(bad.err().contains("Unknown command"));
  }

  @Test
  void launcherSelectsAnInstalledRuntimeWithoutJavaHome() throws Exception {
    List<String> command = new ArrayList<>();
    if (windows()) command.addAll(List.of("cmd", "/d", "/c"));
    command.add(distribution().resolve(windows() ? "lexengine.cmd" : "lexengine").toString());
    command.add("--version");
    Result result = run(command, directory, false);
    success(result);
    assertTrue(result.out().startsWith("Lex-Engine "));
  }

  @Test
  void launcherSurvivesRelocationAndPreservesQuotedArguments() throws Exception {
    Path moved = Files.createDirectories(directory.resolve("portable bundle with spaces"));
    for (String name : List.of("lex-engine.jar", "lexengine", "lexengine.cmd"))
      Files.copy(distribution().resolve(name), moved.resolve(name));
    Path launcher = moved.resolve(windows() ? "lexengine.cmd" : "lexengine");
    if (!windows()) assertTrue(launcher.toFile().setExecutable(true));
    List<String> command = new ArrayList<>();
    if (windows()) command.addAll(List.of("cmd", "/d", "/c"));
    command.add(launcher.toString());
    command.addAll(List.of("examples", "list", "[1, 2, 3]"));
    Result result = run(command, directory);
    success(result);
    assertTrue(result.out().contains("Accepted list using LL1"));
  }

  @Test
  void packagedParserProducesIndependentCompilableSource() throws Exception {
    Path grammar = directory.resolve("list grammar.lg");
    try (var input = getClass().getResourceAsStream("/list.lg")) {
      assertNotNull(input);
      Files.copy(input, grammar);
    }
    Path generated = directory.resolve("generated source");
    success(
        jar(
            "parser",
            "--grammar",
            grammar.toString(),
            "--parser-type",
            "LL1",
            "--output-directory",
            generated.toString()));
    Path classes = Files.createDirectory(directory.resolve("parser classes"));
    var diagnostics = new java.io.ByteArrayOutputStream();
    assertEquals(
        0,
        ToolProvider.getSystemJavaCompiler()
            .run(
                null,
                diagnostics,
                diagnostics,
                "--release",
                "21",
                "-encoding",
                "UTF-8",
                "-d",
                classes.toString(),
                generated.resolve("ListParser.java").toString()),
        diagnostics.toString(StandardCharsets.UTF_8));
    try (var loader =
        new URLClassLoader(
            new java.net.URL[] {classes.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
      var parser = loader.loadClass("org.lexengine.examples.generated.ListParser");
      Object tree =
          parser
              .getMethod("parse")
              .invoke(parser.getConstructor(String.class).newInstance("[1, 2, 3]"));
      assertNotNull(tree);
    }
  }

  @Test
  void packagedLexerReadsTheBundledTemplateAndWritesSource() throws Exception {
    Path spec = directory.resolve("tokens.spec");
    Files.writeString(spec, "\"[0-9]+\" { return Token.number(value); }\n");
    Path generated = directory.resolve("lexer source");
    success(
        jar(
            "lexer",
            "--lex-spec-file",
            spec.toString(),
            "--lexer-class",
            "NumberLexer",
            "--lexer-package",
            "generated",
            "--output-directory",
            generated.toString()));
    Path token = generated.resolve("Token.java");
    Files.writeString(
        token,
        "package generated; public record Token(String value) { public static Token number(String value) { return new Token(value); } }");
    var diagnostics = new java.io.ByteArrayOutputStream();
    assertEquals(
        0,
        ToolProvider.getSystemJavaCompiler()
            .run(
                null,
                diagnostics,
                diagnostics,
                "--release",
                "21",
                "-encoding",
                "UTF-8",
                "-d",
                generated.toString(),
                generated.resolve("NumberLexer.java").toString(),
                token.toString()),
        diagnostics.toString(StandardCharsets.UTF_8));
  }
}
