/*
* Copyright (c) 2025 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.parser.core;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.lexengine.parser.utils.ParserOptions;

public class RecursiveDescentParserGeneratorTest {

  @Test
  void testGenerate() throws URISyntaxException {
    URL resource = this.getClass().getClassLoader().getResource("program.grammar");
    ParserOptions.outDir = ".";
    ParserOptions.parserClassTemplate =
        Path.of(this.getClass().getClassLoader().getResource("parser-class.template").toURI());
    ParserGenerator parserGenerator =
        ParserGenerator.Factory.create(
            new File(resource.toURI()), ParserGenerator.ParserType.RECURSIVE_DESCENT);
    parserGenerator.generate();
  }
}
