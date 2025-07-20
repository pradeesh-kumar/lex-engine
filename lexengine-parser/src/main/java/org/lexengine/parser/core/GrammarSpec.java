package org.lexengine.parser.core;

public record GrammarSpec(String parserClassName, String parserPackageName, Grammar grammar) {

  /** Default values for the metadata properties */
  public static final String DEFAULT_PARSER_CLASS_NAME = "Parser";

  public static final String DEFAULT_PARSER_PACKAGE_NAME = "org.lexengine.parser.generated";

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String parserClassName = DEFAULT_PARSER_CLASS_NAME;
    private String parserPackageName = DEFAULT_PARSER_PACKAGE_NAME;
    private Grammar grammar;

    private final GrammarSpec.Builder specBuilder;

    public Builder() {
      this.specBuilder = new GrammarSpec.Builder();
    }

    public Builder parserClassName(String parserClassName) {
      this.parserClassName = parserClassName;
      return this;
    }

    public Builder parserPackageName(String parserPackageName) {
      this.parserPackageName = parserPackageName;
      return this;
    }

    public GrammarSpec.Builder grammar(Grammar grammar) {
      this.specBuilder.grammar = grammar;
      return this;
    }

    public GrammarSpec build() {
      return specBuilder.build();
    }
  }
}

record Grammar() {

  record Production() {}

  record Terminal() {}
}
