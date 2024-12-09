/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.error;

/** Enum representing different types of errors that can occur during processing. */
public enum ErrorType {

  /** Error occurred while reading spec file. */
  ERR_SPEC_FILE_READ,

  /** Error related to property handling. */
  ERR_PROPERTY_ERR,

  /** Error related to regular expression parsing or matching. */
  ERR_REGEX_ERR,

  /** No entry found in regular expressions section. */
  ERR_REGEX_NO_ENTRY,

  /** Invalid regular expression */
  ERR_REGEX_INVALID,
}
