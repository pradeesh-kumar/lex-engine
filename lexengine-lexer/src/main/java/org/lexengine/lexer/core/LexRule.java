package org.lexengine.lexer.core;

public record LexRule(String group, String name, Regex regex, Action action) {

  public record Action(String action) {

    @Override public String toString() {
      return action;
    }
  }
}
