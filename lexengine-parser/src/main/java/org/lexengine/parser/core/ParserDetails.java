/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.parser.core;

import java.nio.file.Path;

/** Statistics and location of the most recently generated parser. */
public record ParserDetails(
    Path sourceFile, ParserGenerator.ParserType type, int productionCount, int stateCount) {}
