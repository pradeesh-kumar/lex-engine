/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.lexengine.lexer.error.ErrorType;
import org.lexengine.lexer.error.GeneratorException;
import org.lexengine.lexer.logging.Out;

public final class NfaGenerator {

  private final List<RegexAction> regexActions;
  private final DisjointIntSet languageAlphabets;
  private final Map<Interval, Integer> alphabetIndex;

  NfaGenerator(
      List<RegexAction> regexActions,
      DisjointIntSet languageAlphabets,
      Map<Interval, Integer> alphabetIndex) {
    Objects.requireNonNull(regexActions);
    Objects.requireNonNull(languageAlphabets);
    this.regexActions = regexActions;
    this.languageAlphabets = languageAlphabets;
    this.alphabetIndex = alphabetIndex;
  }

  public Nfa generate() {
    Nfa nfa = new Nfa(languageAlphabets.size());
    // regexActions.forEach(regexAction -> addRegexActionToNfa(regexAction, nfa));
    return nfa;
  }

  private class GeneratorInternal {

    private final RegexAction regexAction;
    private final Iterator<RegexToken> regexTknItr;
    private final Nfa nfa;

    public GeneratorInternal(RegexAction regexAction, Nfa nfa) {
      this.regexAction = regexAction;
      this.regexTknItr = regexAction.regex().iterator();
      this.nfa = nfa;
    }

    public Nfa.NfaState generate() {
      Nfa.NfaState current = null;
      while (regexTknItr.hasNext()) {
        RegexToken token = regexTknItr.next();
        switch (token.type()) {
          case RegexToken.Type.Literal -> {
            Nfa.NfaState state = nfa.new NfaState(alphabetIndex.get(token.interval()));
            if (current == null) {
              current = state;
            } else {
              current.concat(state);
            }
          }
          case RegexToken.Type.LParen -> {
            Nfa.NfaState state = generate();
            if (current == null) {
              current = state;
            } else {
              current.concat(state);
            }
          }
          case RegexToken.Type.RParen -> {
            return current;
          }
          case RegexToken.Type.CharClass -> {
            List<Interval> intersection = languageAlphabets.getIntersection(token.intervals());
            List<Nfa.NfaState> stateList =
                intersection.stream()
                    .map(alphabetIndex::get)
                    .map(index -> nfa.new NfaState(index))
                    .toList();
          }
          case RegexToken.Type.InvertedCharClass -> {}
          case RegexToken.Type.Bar -> {
            Nfa.NfaState state = generate();
            if (current == null) {
              Out.error(
                  "Invalid regex %s. Contains invalid escape sequence character",
                  regexAction.regex().toString());
              throw GeneratorException.error(ErrorType.ERR_REGEX_INVALID);
            }
            current.alternate(state);
          }
          case RegexToken.Type.Dollar -> {}
          case RegexToken.Type.Dot -> {}
        }
      }
      return current;
    }
  }
}
