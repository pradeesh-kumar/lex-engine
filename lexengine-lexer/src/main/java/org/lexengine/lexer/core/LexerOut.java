package org.lexengine.lexer.core;

import org.lexengine.commons.error.ErrorType;
import org.lexengine.commons.error.GeneratorException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Represents output of a Lexer Generator */
public class LexerOut {

  private final Reader lexerClassReader;
  private final String lexerClassName;
  private final String packageName;

  protected LexerOut(Reader lexerClassReader,  String lexerClassName,  String packageName) {
    this.lexerClassReader = lexerClassReader;
    this.lexerClassName = lexerClassName;
    this.packageName = packageName;
  }

  /**
   * Writes the lexer class to a file in the specified directory.
   *
   * @param outDir the directory to write the lexer class to
   * @throws IOException if an I/O error occurs
   */
  public void writeToDirectory(String outDir) throws IOException {
    mkdirIfNotExists(outDir);
    Path filePath = Path.of(outDir, lexerClassName + ".java").normalize();
    try (this.lexerClassReader;
         BufferedWriter writer = Files.newBufferedWriter(
             filePath,
             StandardOpenOption.CREATE,
             StandardOpenOption.WRITE,
             StandardOpenOption.TRUNCATE_EXISTING)) {
      char[] buffer = new char[1024];
      int n;
      while ((n = lexerClassReader.read(buffer)) != -1) {
        writer.write(buffer, 0, n);
      }
    }
  }

  /**
   * Creates the specified directory if it does not exist.
   * If the path exists but is not a directory, an exception is thrown.
   *
   * @param outDir the directory to create
   */
  private static void mkdirIfNotExists(String outDir) {
    Path path = Path.of(outDir);
    if (!path.toFile().exists()) {
      path.toFile().mkdirs();
    } else if (!path.toFile().isDirectory()) {
      throw GeneratorException.create(ErrorType.ERR_LEX_OUT_DIR_INVALID, "The path %s is not a directory", path.toAbsolutePath());
    }
  }

  public String lexerClassName() {
    return lexerClassName;
  }

  public String packageName() {
    return packageName;
  }
}
