/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.commons.error;

/** Enum representing different types of errors that can occur during processing. */
public enum ErrorType {

  /** Invalid spec file. */
  ERR_LEX_SPEC_FILE_INVALID,

  /** Error occurred while reading spec file. */
  ERR_LEX_SPEC_FILE_READ,

  /** Error related to property handling. */
  ERR_LEX_PROPERTY_ERR,

  /** Error related to regular expression parsing or matching. */
  ERR_LEX_REGEX_ERR,

  /** No entry found in regular expressions section. */
  ERR_LEX_REGEX_NO_ENTRY,

  /** Invalid regular expression */
  ERR_LEX_REGEX_INVALID,

  /** Invalid output directory specified */
  ERR_LEX_OUT_DIR_INVALID,

  /** Error while compressing the transition table */
  ERR_LEX_TRANSITION_TBL_COMPRESSION,

  /** Error while generating the class file */
  ERR_CLASS_GENERATE,

  /** Placeholder attribute missing during class generation */
  ERR_CLASS_GENERATE_ATTR_MISSING,

  ERR_LEX_TEMPLATE_FILE_READ,

  ERR_PARSER_UNIMPLEMENTED,
  ERR_GRAMMAR_FILE_READ,
  ERR_GRAMMAR_FILE_INVALID,
  ERR_GRAMMAR_PRODUCTION_INVALID,
  ERR_GRAMMAR_FILE_EMPTY_PRODUCTION,
  ERR_PARSER_PROPERTY_ERR,
  ERR_PARSER_PRODUCTION_RULE_INVALID,
  ERR_PARSER_PLACEHOLDER_NOT_FOUND,
}
