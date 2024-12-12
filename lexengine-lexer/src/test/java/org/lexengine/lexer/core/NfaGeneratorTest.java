package org.lexengine.lexer.core;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.lexengine.lexer.util.Options;

public class NfaGeneratorTest {

    static {
        Options.verbose = false;
    }

    @Test
    void testMatchesAndNonMatches() {
        File testSpecFile = new File(getClass().getClassLoader().getResource("lexer-spec.spec").getFile());
        SpecParser parser = new SpecParser(testSpecFile);
        LexSpec lexSpec = parser.parseSpec();
        List<RegexAction> regexActionList = lexSpec.regexActionList();
        DisjointIntSet languageAlphabets = new DisjointIntSet();
        LexGenUtils.extractAlphabetsFromRegex(regexActionList, languageAlphabets);
        Map<Interval, Integer> alphabetsIndex = LexGenUtils.createAlphabetsIndex(languageAlphabets.intervals());
        NfaGenerator nfaGenerator = new NfaGenerator(regexActionList, languageAlphabets, alphabetsIndex);
        Nfa nfa = nfaGenerator.generate();
        assertNotNull(nfa);

        Action action = nfa.test("new");
        assertAction(action, "{ return Token.keyword(Token.Type.NEW); }");

        action = nfa.test("int");
        assertAction(action, "{ return Token.keyword(Token.Type.INT); }");

        action = nfa.test("float");
        assertAction(action, "{ return Token.keyword(Token.Type.FLOAT); }");

        action = nfa.test("not");
        assertAction(action, "{ return Token.keyword(Token.Type.NOT); }");

        action = nfa.test("0");
        assertAction(action, "{ return Token.integer(value); }");

        action = nfa.test("9");
        assertAction(action, "{ return Token.integer(value); }");

        action = nfa.test("123920310");
        assertAction(action, "{ return Token.integer(value); }");

        action = nfa.test("D");
        assertAction(action, "{ return Token.identifier(value); }");

        action = nfa.test("d");
        assertAction(action, "{ return Token.identifier(value); }");

        action = nfa.test("alpha123_lfa123d");
        assertAction(action, "{ return Token.identifier(value); }");

        action = nfa.test("{");
        assertAction(action, "{ return Token.keyword(Token.Type.LBRACE); }");

        action = nfa.test("[");
        assertAction(action, "{ return Token.keyword(Token.Type.LSQBRACKET); }");

        action = nfa.test("cat");
        assertAction(action, "{ return Token.keyword(Token.Type.CATRAT); }");

        action = nfa.test("rat");
        assertAction(action, "{ return Token.keyword(Token.Type.CATRAT); }");

        action = nfa.test("<");
        assertAction(action, "{ return Token.keyword(Token.Type.LESSTHAN); }");

        action = nfa.test("<=");
        assertAction(action, "{ return Token.keyword(Token.Type.LESSTHANOREQ); }");

        action = nfa.test("0121");
        assertNull(action);

        action = nfa.test("0abc");
        assertNull(action);

        action = nfa.test("$");
        assertNull(action);

        action = nfa.test("\n");
        assertAction(action, "{ // do nothing }");
    }

    private void assertAction(Action action, String expected) {
        assertNotNull(action);
        assertEquals(expected, action.toString());
    }
}
