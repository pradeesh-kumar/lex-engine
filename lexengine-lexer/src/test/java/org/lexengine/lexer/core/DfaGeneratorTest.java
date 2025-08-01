/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class DfaGeneratorTest {

  @Test
  void testMatchesAndNonMatches() {
    Dfa dfa = new DfaGenerator(TestUtils.generateNfa("lexer-spec.spec")).generate();
    assertNotNull(dfa);
    assertAction(dfa.test("new"), "{ return Token.keyword(Token.Type.NEW); }");
    assertAction(dfa.test("int"), "{ return Token.keyword(Token.Type.INT); }");
    assertAction(dfa.test("float"), "{ return Token.keyword(Token.Type.FLOAT); }");
    assertAction(dfa.test("not"), "{ return Token.keyword(Token.Type.NOT); }");
    assertAction(dfa.test("0"), "{ return Token.integer(value); }");
    assertNull(dfa.test("00"));
    assertAction(dfa.test("9"), "{ return Token.integer(value); }");
    assertAction(dfa.test("123920310"), "{ return Token.integer(value); }");
    assertAction(dfa.test("D"), "{ return Token.identifier(value); }");
    assertAction(dfa.test("d"), "{ return Token.identifier(value); }");
    assertAction(dfa.test("alpha123_lfa123d"), "{ return Token.identifier(value); }");
    assertAction(dfa.test("{"), "{ return Token.keyword(Token.Type.LBRACE); }");
    assertAction(dfa.test("["), "{ return Token.keyword(Token.Type.LSQBRACKET); }");
    assertAction(dfa.test("cat"), "{ return Token.keyword(Token.Type.CATRAT); }");
    assertAction(dfa.test("rat"), "{ return Token.keyword(Token.Type.CATRAT); }");
    assertAction(dfa.test("<"), "{ return Token.keyword(Token.Type.LESSTHAN); }");
    assertAction(dfa.test("<="), "{ return Token.keyword(Token.Type.LESSTHANOREQ); }");
    assertAction(dfa.test("\"Hello World\""), "{ return Token.string(value()); }");
    assertAction(dfa.test("\n"), "{ /* do nothing */ }");
    assertNull(dfa.test("0121"));
    assertNull(dfa.test("0abc"));
    assertNull(dfa.test("$"));
    assertAction(dfa.test("/* hello world how are you */"), "{ return Token.comment(); }");
    assertAction(dfa.test("/** my comment ****/"), "{ return Token.comment(); }");
  }

  private void assertAction(Action action, String expected) {
    assertNotNull(action);
    assertEquals(expected, action.toString());
  }
}
