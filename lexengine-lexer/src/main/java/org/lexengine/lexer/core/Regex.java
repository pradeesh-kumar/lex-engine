/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.lexengine.lexer.error.ErrorType;
import org.lexengine.lexer.error.GeneratorException;
import org.lexengine.lexer.logging.Out;

public class Regex implements Iterable<RegexToken> {

  private static final Set<Character> META_CHARS =
      Set.of('|', '.', '$', '^', '*', '+', '?', '(', ')', '{', '}', '"');

  private static final Set<Character> QUANTIFIERS = Set.of('*', '+', '?', '{', '}');

  private final String val;

  private Regex(String val) {
    this.val = val;
  }

  public static Regex fromString(String regex) {
    return new Regex(regex);
  }

  public String toString() {
    return val;
  }

  public static boolean isMetaChar(char ch) {
    return META_CHARS.contains(ch);
  }

  public Iterator<RegexToken> iterator() {
    return new RegexIterator();
  }

  public List<Interval> extractAlphabets() {
    return new RegexAlphabetExtractor().extract();
  }

  private class RegexIterator implements Iterator<RegexToken> {

    private int pos;

    private RegexIterator() {
      this.pos = 0;
    }

    private char peek() {
      if (pos >= val.length()) {
        return '\0';
      }
      return val.charAt(pos);
    }

    private char nextChar() {
      return hasNext() ? val.charAt(pos++) : '\0';
    }

    @Override
    public boolean hasNext() {
      if (pos >= val.length()) {
        return false;
      }
      return true;
    }

    @Override
    public RegexToken next() {
      if (!hasNext()) {
        return null;
      }
      char literal = nextChar();
      if (literal == '[') {
        return convertCharClasses();
      }
      if (literal == '\'') {
        if (peek() == '\0') {
          Out.error("Invalid regex %s. Contains illegal escape sequence character", val);
          throw GeneratorException.error(ErrorType.ERR_REGEX_INVALID);
        } else if (!META_CHARS.contains(peek())) {
          Out.error("Invalid regex %s. Contains invalid escape sequence character", val);
          throw GeneratorException.error(ErrorType.ERR_REGEX_INVALID);
        }
        literal = nextChar();
      }
      return RegexToken.ofLiteral(literal, detectQuantifier());
    }

    private char detectQuantifier() {
      if (QUANTIFIERS.contains(peek())) {
        return nextChar();
      }
      return '\0';
    }

    private RegexToken convertCharClasses() {
      char literal = nextChar();
      boolean inverted = false;
      if (literal == '\0' || literal == ']') {
        Out.error("Invalid regex %s", val);
        throw GeneratorException.error(ErrorType.ERR_REGEX_INVALID);
      }
      if (peek() == '^') {
        inverted = true;
        nextChar();
      }
      List<Interval> intervals = new LinkedList<>();
      while (literal != '\0' && literal != ']') {
        if (peek() == '-') {
          intervals.add(parseRange(literal));
        } else {
          intervals.add(Interval.of(literal));
        }
        literal = nextChar();
      }
      return RegexToken.ofClass(intervals, inverted, detectQuantifier());
    }

    private Interval parseRange(char left) {
      next(); // Ignore '-'
      char right = nextChar();
      if (!(Character.isLetterOrDigit(left) && Character.isLetterOrDigit(right))) {
        Out.error(
            "Invalid char class in the regex %s Only letters and digits allowed in the range class",
            val);
        throw GeneratorException.error(ErrorType.ERR_REGEX_INVALID);
      }
      if (Character.isDigit(left) ^ Character.isDigit(right)) {
        Out.error(
            "Invalid char class in the regex %s Cannot mix digit and letter in range class", val);
        throw GeneratorException.error(ErrorType.ERR_REGEX_INVALID);
      }
      if (left >= right) {
        Out.error("Invalid char class in the regex %s range class values cannot be same", val);
        throw GeneratorException.error(ErrorType.ERR_REGEX_INVALID);
      }
      return Interval.of(left, right);
    }
  }

  private class RegexAlphabetExtractor {

    public List<Interval> extract() {
      Iterator<RegexToken> it = iterator();
      List<Interval> intervals = new LinkedList<>();
      while (it.hasNext()) {
        RegexToken token = it.next();
        if (token.type().isMetaChar()) {
          continue;
        }
        if (token.chVal() != '\0') {
          intervals.add(Interval.of(token.chVal()));
        } else {
          intervals.addAll(token.intervals());
        }
      }
      return intervals;
    }
  }
}
