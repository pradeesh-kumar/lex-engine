/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.commons;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.lexengine.commons.logging.Out;

public record Options(
    boolean verbose,
    Charset encoding,
    String lexerClassName,
    String lexerPackageName,
    String lexerMethodName,
    String lexerReturnType,
    String scannerClassTemplate,
    String lexSpecFile,
    String outDir) {

  public Options(
      boolean verbose,
      Charset encoding,
      String lexerClassName,
      String lexerPackageName,
      String lexerMethodName,
      String lexerReturnType,
      String scannerClassTemplate,
      String lexSpecFile) {
    this(
        verbose,
        encoding,
        lexerClassName,
        lexerPackageName,
        lexerMethodName,
        lexerReturnType,
        scannerClassTemplate,
        lexSpecFile,
        ".");
  }

  private static Options defaultInstance = Options.builder().build();

  public Options.Builder toBuilder() {
    return new Builder()
        .encoding(encoding)
        .verbose(verbose)
        .lexerClassName(lexerClassName)
        .lexerPackageName(lexerPackageName)
        .lexerMethodName(lexerMethodName)
        .lexerReturnType(lexerReturnType)
        .scannerClassTemplate(scannerClassTemplate)
        .lexSpecFile(lexSpecFile)
        .outDir(outDir);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static Options defaultOptions() {
    return defaultInstance;
  }

  public static void setDefault(Options options) {
    defaultInstance = options;
  }

  public static Options parseFromArgs(String[] args) {
    int i = 0;
    Builder builder = builder();
    while (i < args.length) {
      switch (args[i]) {
        case "-v", "--verbose" -> {
          builder.verbose(true);
          Out.enableDebug();
        }
        case "-lc", "--lexer-class" -> builder.lexerClassName(getNextArg(args, ++i));
        case "-lm", "--lexer-method" -> builder.lexerMethodName(getNextArg(args, ++i));
        case "-lp", "--lexer-package" -> builder.lexerPackageName(getNextArg(args, ++i));
        case "-st", "-sc", "--scanner-template" ->
            builder.scannerClassTemplate(getNextArg(args, ++i));
        case "-sp", "--lex-spec-file" -> builder.lexSpecFile(getNextArg(args, ++i));
        case "-d", "--output-directory" -> builder.outDir(getNextArg(args, ++i));
        default -> throw new IllegalArgumentException("Unknown option: " + args[i]);
      }
      i++;
    }
    return builder.build();
  }

  /**
   * Retrieves the next command-line argument from the given array.
   *
   * <p>This method checks if there is a next argument at the specified index. If so, it returns the
   * argument. Otherwise, it logs an error message indicating that no argument was specified after
   * the previous option and exits the program with a non-zero status code.
   *
   * @param args the array of command-line arguments
   * @param i the index of the next argument to retrieve
   * @return the next command-line argument, or null (though the method never returns due to
   *     System.exit)
   */
  private static String getNextArg(String[] args, int i) {
    if (i < args.length) {
      return args[i];
    } else {
      throw new IllegalArgumentException("Missing value for " + args[i - 1]);
    }
  }

  public static class Builder {

    // Initialize with default values;
    private boolean verbose = false;
    private Charset encoding = StandardCharsets.UTF_8;
    private String lexerClassName = "Lexer";
    private String lexerPackageName = "org.lexengine.generator";
    private String lexerMethodName = "nextToken";
    private String lexerReturnType = "Token";
    private String scannerClassTemplate = "scanner-class.tpl";
    private String lexSpecFile = "lexer-spec.spec";
    private String outDir = ".";

    public Builder verbose(boolean verbose) {
      this.verbose = verbose;
      return this;
    }

    public Builder encoding(Charset encoding) {
      this.encoding = encoding;
      return this;
    }

    public Builder lexerClassName(String lexerClassName) {
      this.lexerClassName = lexerClassName;
      return this;
    }

    public Builder lexerPackageName(String lexPackageName) {
      this.lexerPackageName = lexPackageName;
      return this;
    }

    public Builder lexerMethodName(String methodName) {
      this.lexerMethodName = methodName;
      return this;
    }

    public Builder lexerReturnType(String lexerReturnType) {
      this.lexerReturnType = lexerReturnType;
      return this;
    }

    public Builder scannerClassTemplate(String scannerClassTemplate) {
      this.scannerClassTemplate = scannerClassTemplate;
      return this;
    }

    public Builder lexSpecFile(String lexSpecFile) {
      this.lexSpecFile = lexSpecFile;
      return this;
    }

    public Builder outDir(String outDir) {
      this.outDir = outDir;
      return this;
    }

    public Options build() {
      return new Options(
          verbose,
          encoding,
          lexerClassName,
          lexerPackageName,
          lexerMethodName,
          lexerReturnType,
          scannerClassTemplate,
          lexSpecFile,
          outDir);
    }
  }
}
