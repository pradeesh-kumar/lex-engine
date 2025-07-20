package org.lexengine.parser.core;

import java.io.File;
import org.lexengine.commons.logging.Out;
import org.lexengine.parser.error.ErrorType;
import org.lexengine.parser.error.ParserGeneratorException;

public abstract class ParserGenerator {

  protected final Lexer lexer;
  protected final GrammarSpec grammarSpec;

  public ParserGenerator(Lexer lexer, File grammarFile) {
    this.lexer = lexer;
    this.grammarSpec = new GrammarSpecParser(grammarFile).parse();
  }

  public abstract ParserDetails generate();

  public enum ParserType {
    LL1,
    LR1,
    RECURSIVE_DESCENT;
  }

  public static class Factory {

    public static ParserGenerator create(Lexer lexer, File grammarFile, ParserType type) {
      switch (type) {
        case LL1 -> {
          Out.error("LL1 not implemented!");
          throw ParserGeneratorException.error(ErrorType.ERR_PARSER_UNIMPLEMENTED);
        }
        case LR1 -> {
          Out.error("LR1 not implemented!");
          throw ParserGeneratorException.error(ErrorType.ERR_PARSER_UNIMPLEMENTED);
        }
        case RECURSIVE_DESCENT -> {
          return new RecursiveDescentParserGenerator(lexer, grammarFile);
        }
        default -> {
          Out.error("Unknown parser type: !");
          throw ParserGeneratorException.error(ErrorType.ERR_PARSER_UNIMPLEMENTED);
        }
      }
    }
  }
}
