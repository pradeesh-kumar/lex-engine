package org.lexengine.lexer.core;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.lexengine.lexer.util.Options;

public class NfaGeneratorTest {

    @Test
    void testGenerate() {
        Options.verbose = true;
        File testSpecFile = new File(getClass().getClassLoader().getResource("lexer-spec.spec").getFile());
        SpecParser parser = new SpecParser(testSpecFile);
        LexSpec lexSpec = parser.parseSpec();
        List<RegexAction> regexActionList = lexSpec.regexActionList();
        DisjointIntSet languageAlphabets = new DisjointIntSet();
        LexGenUtils.extractAlphabetsFromRegex(regexActionList, languageAlphabets);
        Map<Interval, Integer> alphabetsIndex = LexGenUtils.createAlphabetsIndex(languageAlphabets.intervals());
        NfaGenerator nfaGenerator = new NfaGenerator(regexActionList, languageAlphabets, alphabetsIndex);
        Nfa nfa = nfaGenerator.generate();
        System.out.println("Language alphabets: " + languageAlphabets);
        assertNotNull(nfa);

        Action action = nfa.test("new");
        assertNotNull(action);
        System.out.println(action);

        action = nfa.test("int");
        assertNotNull(action);
        System.out.println(action);

        action = nfa.test("float");
        assertNotNull(action);
        System.out.println(action);

        action = nfa.test("not");
        assertNotNull(action);
        System.out.println(action);

        action = nfa.test("0");
        assertNotNull(action);
        System.out.println(action);

        action = nfa.test("9");
        assertNotNull(action);
        System.out.println(action);

        action = nfa.test("D");
        assertNotNull(action);
        System.out.println(action);

        action = nfa.test("alphalfa");
        assertNotNull(action);
        System.out.println(action);
    }
}
