/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.lexengine.lexer.util.LexerOptions;

public class NfaGeneratorTest {

  static {
    LexerOptions.verbose = false;
  }

  @Test
  void testMatchesAndNonMatches() {
    Nfa nfa = TestUtils.generateNfa("lexer-spec.spec");

    assertAction(nfa.test("new"), "{ return Token.keyword(Token.Type.NEW); }");
    assertAction(nfa.test("int"), "{ return Token.keyword(Token.Type.INT); }");
    assertAction(nfa.test("float"), "{ return Token.keyword(Token.Type.FLOAT); }");
    assertAction(nfa.test("not"), "{ return Token.keyword(Token.Type.NOT); }");
    assertAction(nfa.test("0"), "{ return Token.integer(value); }");
    assertAction(nfa.test("9"), "{ return Token.integer(value); }");
    assertAction(nfa.test("123920310"), "{ return Token.integer(value); }");
    assertAction(nfa.test("D"), "{ return Token.identifier(value); }");
    assertAction(nfa.test("d"), "{ return Token.identifier(value); }");
    assertAction(nfa.test("alpha123_lfa123d"), "{ return Token.identifier(value); }");
    assertAction(nfa.test("{"), "{ return Token.keyword(Token.Type.LBRACE); }");
    assertAction(nfa.test("["), "{ return Token.keyword(Token.Type.LSQBRACKET); }");
    assertAction(nfa.test("cat"), "{ return Token.keyword(Token.Type.CATRAT); }");
    assertAction(nfa.test("rat"), "{ return Token.keyword(Token.Type.CATRAT); }");
    assertAction(nfa.test("<"), "{ return Token.keyword(Token.Type.LESSTHAN); }");
    assertAction(nfa.test("<="), "{ return Token.keyword(Token.Type.LESSTHANOREQ); }");
    assertAction(nfa.test("\"Hello World\""), "{ return Token.string(value()); }");
    assertNull(nfa.test("0121"));
    assertNull(nfa.test("0abc"));
    assertNull(nfa.test("$"));
    assertAction(nfa.test("\n"), "{ /* do nothing */ }");
    assertAction(nfa.test("/* hello world how are you */"), "{ return Token.comment(); }");
    assertAction(nfa.test("/** my comment ****/"), "{ return Token.comment(); }");
  }

  private void assertAction(Action action, String expected) {
    assertNotNull(action);
    assertEquals(expected, action.toString());
  }
}
