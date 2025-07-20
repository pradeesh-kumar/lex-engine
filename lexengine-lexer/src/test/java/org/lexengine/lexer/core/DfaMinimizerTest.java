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

public class DfaMinimizerTest {

  @Test
  void testMatchesAndNonMatches_case1() {
    Dfa dfa = new DfaGenerator(TestUtils.generateNfa("lexer-spec-dfa-min.spec")).generate();
    Dfa minDfa = new DfaMinimizer(dfa).minimize();
    assertAction(dfa.test("fie"), "{ return Token.keyword(Token.Type.FIE); }");
    assertAction(dfa.test("fee"), "{ return Token.keyword(Token.Type.FEE); }");

    assertAction(minDfa.test("fie"), "{ return Token.keyword(Token.Type.FIE); }");
    assertAction(minDfa.test("fee"), "{ return Token.keyword(Token.Type.FEE); }");
    assertNull(minDfa.test("new"));
    assertNull(minDfa.test("fees"));
    assertNull(minDfa.test("feee"));
    assertNull(minDfa.test("fiee"));
  }

  @Test
  void testMatchesAndNonMatches_case2() {
    LexerOptions.verbose = true;
    Dfa minDfa =
        new DfaMinimizer(new DfaGenerator(TestUtils.generateNfa("lexer-spec.spec")).generate())
            .minimize();
    assertAction(minDfa.test("new"), "{ return Token.keyword(Token.Type.NEW); }");
    assertAction(minDfa.test("int"), "{ return Token.keyword(Token.Type.INT); }");
    assertAction(minDfa.test("float"), "{ return Token.keyword(Token.Type.FLOAT); }");
    assertAction(minDfa.test("not"), "{ return Token.keyword(Token.Type.NOT); }");
    assertAction(minDfa.test("0"), "{ return Token.integer(value); }");
    assertNull(minDfa.test("00"));
    assertAction(minDfa.test("9"), "{ return Token.integer(value); }");
    assertAction(minDfa.test("123920310"), "{ return Token.integer(value); }");
    assertAction(minDfa.test("D"), "{ return Token.identifier(value); }");
    assertAction(minDfa.test("d"), "{ return Token.identifier(value); }");
    assertAction(minDfa.test("alpha123_lfa123d"), "{ return Token.identifier(value); }");
    assertAction(minDfa.test("{"), "{ return Token.keyword(Token.Type.LBRACE); }");
    assertAction(minDfa.test("["), "{ return Token.keyword(Token.Type.LSQBRACKET); }");
    assertAction(minDfa.test("cat"), "{ return Token.keyword(Token.Type.CATRAT); }");
    assertAction(minDfa.test("rat"), "{ return Token.keyword(Token.Type.CATRAT); }");
    assertAction(minDfa.test("<"), "{ return Token.keyword(Token.Type.LESSTHAN); }");
    assertAction(minDfa.test("<="), "{ return Token.keyword(Token.Type.LESSTHANOREQ); }");
    assertAction(minDfa.test("\n"), "{ /* do nothing */ }");
    assertAction(minDfa.test("\"Hello World\""), "{ return Token.string(value()); }");
    assertNull(minDfa.test("0abc"));
    assertNull(minDfa.test("$"));
    assertNull(minDfa.test("0121"));
    assertAction(minDfa.test("/* hello world how are you */"), "{ return Token.comment(); }");
    assertAction(minDfa.test("/** my comment ****/"), "{ return Token.comment(); }");
  }

  private void assertAction(Action action, String expected) {
    assertNotNull(action);
    assertEquals(expected, action.toString());
  }
}
