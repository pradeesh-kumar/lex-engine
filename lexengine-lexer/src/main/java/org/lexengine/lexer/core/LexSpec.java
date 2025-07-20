/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Lexer Specification definition used to generate a lexer.
 *
 * <p>A LexSpec object encapsulates metadata about the generated lexer class, including its name,
 * package, method name, and return type. It also stores a list of regular expression patterns and
 * their corresponding actions.
 *
 * @param lexClassName the name of the generated lexer class
 * @param lexPackageName the package name of the generated lexer class
 * @param methodName the name of the generated method
 * @param returnType the return type of the generated method
 * @param regexActionList the list of regular expressions and actions
 */
public record LexSpec(
    String lexClassName,
    String lexPackageName,
    String methodName,
    String returnType,
    List<RegexAction> regexActionList) {

  /** Default values for the metadata properties */
  public static final String DEFAULT_LEXER_CLASS_NAME = "Lexer";

  public static final String DEFAULT_LEXER_PACKAGE_NAME = "org.lexengine.lexer.generated";
  public static final String DEFAULT_METHOD_NAME = "nextToken";
  public static final String DEFAULT_RETURN_TYPE = "Token";

  public static Builder builder() {
    return new Builder();
  }

  /** A builder class for creating LexSpec instances. */
  public static class Builder {
    private String lexClassName = DEFAULT_LEXER_CLASS_NAME;
    private String lexPackageName = DEFAULT_LEXER_PACKAGE_NAME;
    private String methodName = DEFAULT_METHOD_NAME;
    private String returnType = DEFAULT_RETURN_TYPE;

    private final List<RegexAction> regexActionList = new ArrayList<>();

    public Builder lexClassName(String lexClassName) {
      this.lexClassName = lexClassName;
      return this;
    }

    public Builder lexPackageName(String lexPackageName) {
      this.lexPackageName = lexPackageName;
      return this;
    }

    public Builder methodName(String methodName) {
      this.methodName = methodName;
      return this;
    }

    public Builder returnType(String returnType) {
      this.returnType = returnType;
      return this;
    }

    public Builder addRegexAction(RegexAction regexAction) {
      this.regexActionList.add(regexAction);
      return this;
    }

    /**
     * Builds a new LexSpec instance based on the configured settings.
     *
     * <p>If any required fields are not set, default values will be used.
     *
     * @return the built LexSpec instance
     */
    public LexSpec build() {
      return new LexSpec(lexClassName, lexPackageName, methodName, returnType, regexActionList);
    }

    /**
     * Returns the current list of regular expressions and actions.
     *
     * @return the list of regex actions
     */
    public List<RegexAction> regexActionList() {
      return regexActionList;
    }
  }
}

/**
 * Represents an action that can be performed. This class encapsulates a string representing the
 * action.
 */
record Action(String action) {

  @Override
  public String toString() {
    return action;
  }
}

record RegexAction(Regex regex, Action action) {}
