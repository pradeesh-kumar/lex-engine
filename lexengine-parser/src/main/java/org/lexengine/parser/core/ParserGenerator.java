/*
* Copyright (c) 2025 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.parser.core;

import java.io.File;
import java.nio.file.Path;
import org.lexengine.commons.error.ErrorType;
import org.lexengine.commons.error.GeneratorException;
import org.lexengine.commons.logging.Out;
import org.lexengine.parser.core.ll.RecursiveDescentParserGenerator;
import org.lexengine.parser.utils.ParserOptions;

public abstract class ParserGenerator {

  protected final GrammarSpec grammarSpec;

  public ParserGenerator(File grammarFile) {
    this.grammarSpec = new GrammarSpecParser(grammarFile).parse();
    Out.debug("Parsed Grammar: %s", this.grammarSpec.grammar().toString());
  }

  public abstract void generate();

  public enum ParserType {
    LL1,
    LR1,
    RECURSIVE_DESCENT;
  }

  public static class Factory {

    public static ParserGenerator create(File grammarFile, ParserType type) {
      switch (type) {
        case LL1 -> {
          Out.error("LL1 not implemented!");
          throw GeneratorException.error(ErrorType.ERR_PARSER_UNIMPLEMENTED);
        }
        case LR1 -> {
          Out.error("LR1 not implemented!");
          throw GeneratorException.error(ErrorType.ERR_PARSER_UNIMPLEMENTED);
        }
        case RECURSIVE_DESCENT -> {
          return new RecursiveDescentParserGenerator(
              grammarFile, Path.of(ParserOptions.outDir), ParserOptions.parserClassTemplate);
        }
        default -> {
          Out.error("Unknown parser type: !");
          throw GeneratorException.error(ErrorType.ERR_PARSER_UNIMPLEMENTED);
        }
      }
    }
  }
}
