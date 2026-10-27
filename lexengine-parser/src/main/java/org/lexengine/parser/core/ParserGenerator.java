/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.parser.core;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import org.lexengine.parser.core.ll.LL1ParserGenerator;
import org.lexengine.parser.core.ll.RecursiveDescentParserGenerator;
import org.lexengine.parser.core.lr.LR1ParserGenerator;
import org.lexengine.parser.utils.ParserOptions;

public abstract class ParserGenerator {
  protected final GrammarSpec grammarSpec;
  protected final Path outDir;
  protected final Path parserClassTemplate;
  private ParserDetails details;

  protected ParserGenerator(File grammarFile) {
    this(grammarFile, Path.of("."), null);
  }

  protected ParserGenerator(File grammarFile, Path outDir, Path parserClassTemplate) {
    grammarSpec = new GrammarSpecParser(grammarFile).parse();
    this.outDir = Objects.requireNonNull(outDir);
    this.parserClassTemplate = parserClassTemplate;
  }

  public abstract void generate();

  public GrammarSpec grammarSpec() {
    return grammarSpec;
  }

  public ParserDetails details() {
    if (details == null) throw new IllegalStateException("Parser has not been generated");
    return details;
  }

  protected void writeParser(Grammar grammar, ParserType type, String members, int states) {
    ParserSource source = new ParserSource(grammarSpec, grammar);
    Path path = source.write(outDir, parserClassTemplate, members);
    int count = grammar.productions().rules().stream().mapToInt(r -> r.alternatives().size()).sum();
    details = new ParserDetails(path, type, count, states);
  }

  public enum ParserType {
    LL1,
    LR1,
    RECURSIVE_DESCENT
  }

  public static final class Factory {
    private Factory() {}

    public static ParserGenerator create(File grammarFile, ParserType type) {
      return create(
          grammarFile,
          type,
          Path.of(ParserOptions.outDir == null ? "." : ParserOptions.outDir),
          ParserOptions.parserClassTemplate);
    }

    public static ParserGenerator create(
        File grammarFile, ParserType type, Path outDir, Path template) {
      return switch (type) {
        case RECURSIVE_DESCENT ->
            new RecursiveDescentParserGenerator(grammarFile, outDir, template);
        case LL1 -> new LL1ParserGenerator(grammarFile, outDir, template);
        case LR1 -> new LR1ParserGenerator(grammarFile, outDir, template);
      };
    }
  }
}
