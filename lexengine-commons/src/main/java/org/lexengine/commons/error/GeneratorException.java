/*
* Copyright (c) 2025 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.commons.error;

/**
 * Exception thrown when an error occurs during the generation process.
 * <p>
 * This exception provides information about the type of error that occurred, 
 * as well as the line and column numbers where the error was encountered.
 *
 * @see ErrorType for possible error types
 */
public class GeneratorException extends RuntimeException {

  /** Type of error that caused this exception. */
  private final ErrorType errorType;
  /**
   * Line number where the error occurred.
   */
  private final int line;
  /**
   * Column number where the error occurred.
   */
  private final int column;

  /**
   * Constructs a new GeneratorException with the specified error type, 
   * line and column numbers, message, and cause.
   *
   * @param errorType type of error that occurred
   * @param line line number where the error occurred
   * @param column column number where the error occurred
   * @param message error message
   * @param cause underlying cause of the exception
   */
  private GeneratorException(ErrorType errorType, int line, int column, String message, Throwable cause) {
    super(message, cause);
    this.errorType = errorType;
    this.line = line;
    this.column = column;
  }

  /**
   * Creates a new GeneratorException instance with the specified error type, 
   * cause, and message. The message can be formatted using the provided arguments.
   *
   * @param errorType type of error that occurred
   * @param cause underlying cause of the exception
   * @param message error message
   * @param args arguments to be used in formatting the message
   * @return a new GeneratorException instance
   */
  public static GeneratorException create(ErrorType errorType, Throwable cause, String message, Object... args) {
    if (args.length > 0) {
      message = String.format(message, args);
    }
    return new GeneratorException(errorType, -1, -1, message, cause);
  }

  /**
   * Creates a new GeneratorException instance with the specified error type 
   * and message. The message can be formatted using the provided arguments.
   *
   * @param errorType type of error that occurred
   * @param message error message
   * @param args arguments to be used in formatting the message
   * @return a new GeneratorException instance
   */
  public static GeneratorException create(ErrorType errorType, String message, Object... args) {
    return create(errorType, null, message, args);
  }

  /**
   * Creates a new GeneratorException instance with the specified error type, 
   * line and column numbers, cause, and message. The message can be formatted 
   * using the provided arguments.
   *
   * @param errorType type of error that occurred
   * @param line line number where the error occurred
   * @param column column number where the error occurred
   * @param cause underlying cause of the exception
   * @param message error message
   * @param args arguments to be used in formatting the message
   * @return a new GeneratorException instance
   */
  public static GeneratorException create(ErrorType errorType, int line, int column, Throwable cause, String message, Object... args) {
    if (args.length > 0) {
      message = String.format(message, args);
    }
    message = String.format("%s At line %d and column %d", message, line, column);
    return new GeneratorException(errorType, line, column, message, cause);
  }

  /**
   * Creates a new GeneratorException instance with the specified error type, 
   * line and column numbers, and message.
   *
   * @param errorType type of error that occurred
   * @param line line number where the error occurred
   * @param column column number where the error occurred
   * @param message error message
   * @return a new GeneratorException instance
   */
  public static GeneratorException create(ErrorType errorType, int line, int column, String message) {
    return create(errorType, line, column, null, message);
  }

  public ErrorType errorType() {
    return this.errorType;
  }

  public int line() {
    return this.line;
  }
  
  public int column() {
    return this.column;
  }
}
