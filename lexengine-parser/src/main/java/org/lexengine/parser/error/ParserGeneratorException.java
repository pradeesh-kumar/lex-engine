/*
 * Copyright (c) 2024 lex-engine
 * Author: Pradeesh Kumar
 */
package org.lexengine.parser.error;

/**
 * Exception thrown when an error occurs during generation process.
 *
 * @see ErrorType for possible error types
 */
public class ParserGeneratorException extends RuntimeException {

  /** Type of error that caused this exception. */
  private final ErrorType errorType;

  /**
   * Constructs a new GeneratorException with the specified error type.
   *
   * @param errorType type of error that occurred
   */
  private ParserGeneratorException(ErrorType errorType) {
    super("Error: " + errorType);
    this.errorType = errorType;
  }

  /**
   * Creates a new GeneratorException instance with the specified error type.
   *
   * @param errorType type of error that occurred
   * @return a new GeneratorException instance
   */
  public static ParserGeneratorException error(ErrorType errorType) {
    return new ParserGeneratorException(errorType);
  }

  /**
   * Returns the type of error that caused this exception.
   *
   * @return error type
   */
  public ErrorType errorType() {
    return errorType;
  }
}
