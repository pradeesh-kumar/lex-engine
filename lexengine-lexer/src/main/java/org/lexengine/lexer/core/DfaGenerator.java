/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

public final class DfaGenerator {

  public final Nfa nfa;

  public DfaGenerator(Nfa nfa) {
    this.nfa = nfa;
  }

  public Dfa generate() {
    throw new UnsupportedOperationException("Not supported yet.");
  }
}
