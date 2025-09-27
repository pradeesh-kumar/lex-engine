/*
* Copyright (c) 2025 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import org.lexengine.commons.error.ErrorType;
import org.lexengine.commons.error.GeneratorException;
import org.lexengine.commons.logging.Out;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Generates an NFA (Non-Deterministic Finite Automaton) from a list of regular expressions and
 * actions. This uses Thompson Construction Algorithm to create NFA from the regular expressions.
 */
public final class NfaGenerator {

  private final List<LexRule> lexRules;
  private final DisjointIntSet languageAlphabets;
  private final Map<Range, Integer> alphabetIndex;

  /**
   * Constructs an NfaGenerator instance with the given list of regular expressions and actions,
   * language alphabets, and alphabet index.
   *
   * @param lexRules the list of lexical rules containing regular expressions and its metadata
   * @param languageAlphabets the set of language alphabets
   * @param alphabetIndex the mapping of ranges to indices
   */
  NfaGenerator(
      List<LexRule> lexRules,
      DisjointIntSet languageAlphabets,
      Map<Range, Integer> alphabetIndex) {

    Objects.requireNonNull(lexRules, "lexRules");
    Objects.requireNonNull(languageAlphabets);
    this.lexRules = lexRules;
    this.languageAlphabets = languageAlphabets;
    this.alphabetIndex = alphabetIndex;
  }

  /**
   * Generates an NFA from the provided regular expressions and actions.
   *
   * @return the generated NFA
   */
  public Nfa generate() {
    Nfa nfa = new Nfa(languageAlphabets, alphabetIndex);
    Nfa.NfaState state =
        lexRules.stream()
            .map(lexRule -> new NfaStateGenerator(lexRule, nfa))
            .map(NfaStateGenerator::generate)
            .reduce(Nfa.NfaState::alternateWithoutNewAccept)
            .get();
    Out.info(
        "NFA generated. Num States: %d, Num Final States: %d",
        nfa.statesCount(), nfa.finalStatesCount());
    nfa.setStartState(state.start());
    return nfa;
  }

  /** Helper class for generating NFA states from regular expressions. */
  private class NfaStateGenerator {

    private final LexRule lexRule;
    private final Iterator<RegexToken> regexTknItr;
    private final Nfa nfa;

    /**
     * Constructs a new NfaStateGenerator instance.
     *
     * @param lexRule the action associated with the regular expression
     * @param nfa the NFA object used to create new states
     */
    private NfaStateGenerator(LexRule lexRule, Nfa nfa) {
      this.lexRule = lexRule;
      this.regexTknItr = lexRule.regex().iterator();
      this.nfa = nfa;
    }

    /**
     * Generates an NFA state based on the provided regular expression.
     *
     * @return the generated NFA state
     */
    Nfa.NfaState generate() {
      Out.debug(
          "Generating NFA state for regex \"%s\" and action %s",
          lexRule.regex(), lexRule.action());
      Nfa.NfaState state = generateInternal();
      state.registerAction(lexRule.action());
      return state;
    }

    /**
     * Recursively generates an NFA state by processing the remaining tokens in the regular
     * expression.
     *
     * @return the generated NFA state
     */
    private Nfa.NfaState generateInternal() {
      Nfa.NfaState current = null;
      while (regexTknItr.hasNext()) {
        RegexToken token = regexTknItr.next();
        switch (token.type()) {
          case RegexToken.Type.Literal -> current = applyLiteral(current, token);
          case RegexToken.Type.LParen -> current = applyLParen(current);
          case RegexToken.Type.RParen -> {
            applyQuantifierIfPresent(current, token);
            return current;
          }
          case RegexToken.Type.CharClass -> current = applyCharClass(current, token);
          case RegexToken.Type.InvertedCharClass ->
              current = applyInvertedCharClass(current, token);
          case RegexToken.Type.Dot -> current = applyDot(current, token);
          case RegexToken.Type.Bar -> applyAlternate(current);
          default -> {
            throw GeneratorException.create(ErrorType.ERR_LEX_REGEX_INVALID, "unrecognized regular expression token %s",  token.type());
          }
        }
      }
      return current;
    }

    /**
     * Applies a left parenthesis token to the current NFA state.
     *
     * @param current the current NFA state
     * @return the updated NFA state
     */
    private Nfa.NfaState applyLParen(Nfa.NfaState current) {
      Nfa.NfaState state = generateInternal();
      if (current == null) {
        return state;
      }
      current.concat(state);
      return current;
    }

    /**
     * Applies a literal token to the current NFA state.
     *
     * @param current the current NFA state
     * @param token the literal token
     * @return the updated NFA state
     */
    private Nfa.NfaState applyLiteral(Nfa.NfaState current, RegexToken token) {
      Nfa.NfaState state = nfa.new NfaState(alphabetIndex.get(token.range()));
      applyQuantifierIfPresent(state, token);
      if (current == null) {
        return state;
      }
      current.concat(state);
      return current;
    }

    /**
     * Applies a dot token to the current NFA state.
     *
     * @param current the current NFA state
     * @param token the dot token
     * @return the updated NFA state
     */
    private Nfa.NfaState applyDot(Nfa.NfaState current, RegexToken token) {
      List<Range> allRanges = languageAlphabets.ranges();
      return applyCharClass(current, token, allRanges);
    }

    /**
     * Applies a character class token to the current NFA state.
     *
     * @param current the current NFA state
     * @param token the character class token
     * @return the updated NFA state
     */
    private Nfa.NfaState applyCharClass(Nfa.NfaState current, RegexToken token) {
      List<Range> intersection = languageAlphabets.getIntersection(token.ranges());
      return applyCharClass(current, token, intersection);
    }

    /**
     * Applies an inverted character class token to the current NFA state.
     *
     * @param current the current NFA state
     * @param token the inverted character class token
     * @return the updated NFA state
     */
    private Nfa.NfaState applyInvertedCharClass(Nfa.NfaState current, RegexToken token) {
      List<Range> difference = languageAlphabets.getDifference(token.ranges());
      return applyCharClass(current, token, difference);
    }

    /**
     * Applies a character class token to the current NFA state using the specified ranges.
     *
     * @param current the current NFA state
     * @param token the character class token
     * @param ranges the ranges to use
     * @return the updated NFA state
     */
    private Nfa.NfaState applyCharClass(
        Nfa.NfaState current, RegexToken token, List<Range> ranges) {
      Iterator<Range> rangeItr = ranges.iterator();
      Range first = rangeItr.next();
      var firstState = nfa.new NfaState(alphabetIndex.get(first));
      while (rangeItr.hasNext()) {
        firstState.alternate(nfa.new NfaState(alphabetIndex.get(rangeItr.next())));
      }
      applyQuantifierIfPresent(firstState, token);
      if (current == null) {
        return firstState;
      }
      current.concat(firstState);
      return current;
    }

    /**
     * Applies an alternate token to the current NFA state.
     *
     * @param current the current NFA state
     */
    private void applyAlternate(Nfa.NfaState current) {
      if (current == null) {
        throw GeneratorException.create(ErrorType.ERR_LEX_REGEX_INVALID, "Invalid regex %s. Contains invalid escape sequence character", lexRule.action().toString());
      }
      Nfa.NfaState state = generateInternal();
      current.alternate(state);
    }

    /**
     * Applies a quantifier to the specified NFA state if present.
     *
     * @param state the NFA state to apply the quantifier to
     * @param token the token containing the quantifier information
     */
    private void applyQuantifierIfPresent(Nfa.NfaState state, RegexToken token) {
      if (!token.hasQuantifier()) {
        return;
      }
      switch (token.quantifier()) {
        case '*' -> state.closure();
        case '?' -> state.zeroOrOne();
        case '+' -> state.oneOrMore();
        default -> {
          throw GeneratorException.create(ErrorType.ERR_LEX_REGEX_ERR, "Unrecognized quantifier %s", token.quantifier());
        }
      }
    }
  }
}
