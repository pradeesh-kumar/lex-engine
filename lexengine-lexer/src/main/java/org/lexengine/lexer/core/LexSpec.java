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
 */
public class LexSpec {

  /** Default values for the metadata properties */
  private static final String DEFAULT_LEXER_CLASS_NAME = "Lexer";

  private static final String DEFAULT_LEXER_PACKAGE_NAME = "org.lexengine.lexer.generated";
  private static final String DEFAULT_METHOD_NAME = "nextToken";
  private static final String DEFAULT_RETURN_TYPE = "Token";

  /** Metadata for the generated class */
  private final String lexClassName;

  private final String lexPackageName;
  private final String methodName;
  private final String returnType;

  /** List if regular expressions and corresponding actions parsed from the Lexer spec file. */
  private final List<RegexAction> regexActionList;

  /**
   * Constructs a new LexSpec instance with the specified metadata and regex actions.
   *
   * @param lexClassName the name of the generated lexer class
   * @param lexPackageName the package name of the generated lexer class
   * @param methodName the name of the generated method
   * @param returnType the return type of the generated method
   * @param regexActionList the list of regular expressions and actions
   */
  LexSpec(
      String lexClassName,
      String lexPackageName,
      String methodName,
      String returnType,
      List<RegexAction> regexActionList) {
    this.lexClassName = lexClassName;
    this.lexPackageName = lexPackageName;
    this.methodName = methodName;
    this.returnType = returnType;
    this.regexActionList = regexActionList;
  }

  public String lexClassName() {
    return lexClassName;
  }

  public String lexPackageName() {
    return lexPackageName;
  }

  public String methodName() {
    return methodName;
  }

  public String returnType() {
    return returnType;
  }

  public List<RegexAction> regexActionList() {
    return regexActionList;
  }

  /** A builder class for creating LexSpec instances. */
  static class Builder {
    private String lexClassName;
    private String lexPackageName;
    private String methodName;
    private String returnType;

    private List<RegexAction> regexActionList = new ArrayList<>();

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
      if (lexClassName == null) {
        lexClassName = DEFAULT_LEXER_CLASS_NAME;
      }
      if (lexPackageName == null) {
        lexPackageName = DEFAULT_LEXER_PACKAGE_NAME;
      }
      if (methodName == null) {
        methodName = DEFAULT_METHOD_NAME;
      }
      if (returnType == null) {
        returnType = DEFAULT_RETURN_TYPE;
      }
      if (regexActionList == null) {
        regexActionList = new ArrayList<>();
      }
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
class Action {

  /** The string representation of the action. */
  private final String action;

  public Action(String action) {
    this.action = action;
  }

  public String action() {
    return action;
  }
}

record RegexAction(Regex regex, Action action) {}
