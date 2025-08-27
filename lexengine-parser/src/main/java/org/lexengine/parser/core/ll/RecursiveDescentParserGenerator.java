/*
* Copyright (c) 2025 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.parser.core.ll;

import org.lexengine.commons.TemplateRenderer;
import org.lexengine.commons.error.ErrorType;
import org.lexengine.commons.error.GeneratorException;
import org.lexengine.commons.logging.Out;
import org.lexengine.parser.core.Grammar;
import org.lexengine.parser.core.ParserGenerator;
import org.lexengine.parser.core.ParserHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** This class generates a parser class using Recursive Descent algorithm.
 * Recursive Descent is a top-down parser that works with LL(1) grammar.
 * This class converts the given grammar to right-recursive form */
public class RecursiveDescentParserGenerator extends ParserGenerator {

  private static final String SYMBOL_CLASS = """
        class ${className} {
          ${memberDeclarations}
        }
      
      """;

  private static final String CONDITION = """
      if (input.equals("${terminalSymbol}")) {
        accept("${terminalSymbol}");
        index++;
        ${conditionBody}
      }
      """;

  private static final String CONDITION_FAILURE = """
      throw new ParserException("Invalid Symbol ${terminalSymbol}");
      """;

  private static final String MATCHER_METHOD = """
        void ${methodName}() {
          ${body}
        }
      
      """;

  private final Path outDir;
  private final Path parserClassTemplate;
  private Grammar grammar;

  public RecursiveDescentParserGenerator(File grammarFile, Path outDir, Path parserClassTemplate) {
    super(grammarFile);
    this.outDir = outDir;
    this.parserClassTemplate = parserClassTemplate;
  }

  @Override
  public void generate() {
    try {
      this.grammar = ParserHelper.eliminateLeftRecursion(super.grammarSpec.grammar());
      Path outFile = outDir.resolve(grammarSpec.parserClassName() + ".java");
      Out.info("Generating the class file at %s", outFile);
      TemplateRenderer renderer = new TemplateRenderer(parserClassTemplate, prepareAttributes());
      renderer.renderToFile(outFile);
      Out.info("Generated lexer class file at %s", outFile);
    } catch (IOException e) {
      Out.error("Error creating the class file!", e);
      throw GeneratorException.error(ErrorType.ERR_CLASS_GENERATE);
    }
  }

  private Map<String, String> prepareAttributes() {
    Map<String, String> attributes = new HashMap<>();
    attributes.put("className", grammarSpec.parserClassName());
    attributes.put("package", grammarSpec.parserPackageName());
    attributes.put("symbolClassDeclarations", generateSymbolClasses());
    attributes.put("symbolMatcherMethods", generateMatcherMethods());
    return attributes;
  }

  private String generateSymbolClasses() {
    return grammar.productions().nonTerminals()
        .stream()
        .map(symbol -> TemplateRenderer.render(SYMBOL_CLASS, Map.of("className", symbol.className())))
        .collect(Collectors.joining());
  }

  private String generateMatcherMethods() {
    return grammar.productions().rules().stream()
        .map(rule -> generateMatcherMethod(rule.lhs(), rule.alternatives()))
        .collect(Collectors.joining());
  }

  private String generateMatcherMethod(
      Grammar.NonTerminal lhs, List<Grammar.Alternative> alternatives) {
    String body = generateMethodBody(alternatives);
    return TemplateRenderer.render(
        MATCHER_METHOD, Map.of("methodName", lhs.methodName(), "body", body));
  }

  private String generateMethodBody(List<Grammar.Alternative> alternatives) {
    return "";
  }
}
