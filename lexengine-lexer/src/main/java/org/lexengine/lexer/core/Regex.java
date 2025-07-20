/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.lexengine.commons.logging.Out;
import org.lexengine.lexer.error.ErrorType;
import org.lexengine.lexer.error.GeneratorException;

/**
 * Represents a regular expression pattern that can be iterated over to produce individual tokens.
 */
public class Regex implements Iterable<RegexToken> {

  /**
   * A set of meta-characters used in regular expressions. Meta Chars
   *
   * <ul>
   *   <li>* matches zero or more occurrence of the preceding element
   *   <li>+ matches one or more occurrence of the preceding element
   *   <li>? matches zero or one occurrence of the preceding element
   *   <li>. matches any single char
   * </ul>
   */
  private static final Set<Character> META_CHARS =
      Set.of('|', '.', '^', '*', '+', '?', '(', ')', '{', '}', '[', ']');

  /** Escape sequence mappings. */
  private static final Map<Character, Character> ESCAPE_CHAR_MAP =
      Map.of(
          '\\', '\\',
          '"', '"',
          'n', '\n',
          'r', '\r',
          't', '\t',
          ' ', ' ',
          'b', '\b',
          'f', '\f');

  /** A set of quantifiers used in regular expressions. */
  private static final Set<Character> QUANTIFIERS = Set.of('*', '+', '?', '{', '}');

  /** The underlying string representation of the regular expression. */
  private final String val;

  /**
   * Constructs a new Regex instance from a given string.
   *
   * @param val the string representation of the regular expression
   */
  private Regex(String val) {
    this.val = val;
  }

  /**
   * Creates a new Regex instance from a given string.
   *
   * @param regex the string representation of the regular expression
   * @return a new Regex instance
   */
  public static Regex fromString(String regex) {
    return new Regex(regex);
  }

  /**
   * Returns the string representation of the regular expression.
   *
   * @return the string representation of the regular expression
   */
  public String toString() {
    return val;
  }

  /**
   * Returns an iterator over the individual tokens in the regular expression.
   *
   * @return an iterator over the tokens
   */
  public Iterator<RegexToken> iterator() {
    return new RegexIterator();
  }

  /**
   * Extracts all alphabetic ranges from the regular expression.
   *
   * @return a list of extracted ranges
   */
  public List<Range> extractAlphabets() {
    Iterator<RegexToken> itr = iterator();
    List<Range> ranges = new LinkedList<>();

    boolean widerRangeAdded = false;
    while (itr.hasNext()) {
      RegexToken token = itr.next();
      if (token.type().isMetaChar()) {
        if (token.type() == RegexToken.Type.Dot && !widerRangeAdded) {
          // From space till ~ symbol. This includes symbols, both lower and upper case alphabets
          // and digits.
          ranges.add(Range.of(32, 126));
          widerRangeAdded = true;
        }
        continue;
      }
      if (token.chVal() != '\0') {
        ranges.add(Range.of(token.chVal()));
      } else {
        ranges.addAll(token.ranges());
      }
    }
    return ranges;
  }

  /**
   * An iterator implementation for iterating over the individual tokens in the regular expression.
   */
  public class RegexIterator implements Iterator<RegexToken> {

    /** The current position within the regular expression string. */
    private int pos;

    private RegexIterator() {
      this.pos = 0;
    }

    /**
     * Peeks at the next character without advancing the position.
     *
     * @return the next character, or '\0' if at the end of the string
     */
    private char peek() {
      if (pos >= val.length()) {
        return '\0';
      }
      return val.charAt(pos);
    }

    /**
     * Advances to the next character and returns its value.
     *
     * @return the next character, or '\0' if at the end of the string
     */
    private char advance() {
      return hasNext() ? val.charAt(pos++) : '\0';
    }

    @Override
    public boolean hasNext() {
      return pos < val.length();
    }

    @Override
    public RegexToken next() {
      if (!hasNext()) {
        return null;
      }
      char literal = advance();
      if (literal == '[') {
        return convertCharClasses();
      }
      boolean escaped = false;
      if (literal == '\\') {
        char next = advance();
        if (next == '\0') {
          Out.error("Invalid regex \"%s\" Contains illegal escape sequence character", val);
          throw GeneratorException.error(ErrorType.ERR_REGEX_INVALID);
        }
        Character escapeLiteral = ESCAPE_CHAR_MAP.get(next);
        if (!META_CHARS.contains(next) && escapeLiteral == null) {
          Out.error("Invalid regex \"%s\" Contains invalid escape sequence character", val);
          throw GeneratorException.error(ErrorType.ERR_REGEX_INVALID);
        }
        literal = escapeLiteral != null ? escapeLiteral : next;
        escaped = true;
      }
      return RegexToken.ofLiteral(literal, detectQuantifier(), escaped);
    }

    /**
     * Detects whether a quantifier follows the current token.
     *
     * @return the detected quantifier, or '\0' if none found
     */
    private char detectQuantifier() {
      if (QUANTIFIERS.contains(peek())) {
        return advance();
      }
      return '\0';
    }

    /**
     * Converts a character class into a RegexToken instance.
     *
     * @return the converted RegexToken instance
     */
    private RegexToken convertCharClasses() {
      char literal = advance();
      boolean inverted = false;
      if (literal == '^') {
        inverted = true;
        literal = advance();
      }
      if (literal == '\0' || literal == ']' || literal == '-') {
        Out.error("Invalid regex %s", val);
        throw GeneratorException.error(ErrorType.ERR_REGEX_INVALID);
      }
      List<Range> ranges = new LinkedList<>();
      while (literal != '\0' && literal != ']') {
        if (peek() == '-') {
          ranges.add(parseRange(literal));
        } else if (literal == '\\') {
          literal = advance();
          Character escapedCh = ESCAPE_CHAR_MAP.get(literal);
          if (escapedCh == null) {
            Out.error("Invalid regex \"%s\"", val);
            throw GeneratorException.error(ErrorType.ERR_REGEX_INVALID);
          }
          literal = escapedCh;
          ranges.add(Range.of(literal));
        } else {
          ranges.add(Range.of(literal));
        }
        literal = advance();
      }
      return RegexToken.ofClass(ranges, inverted, detectQuantifier());
    }

    /**
     * Parses a range within a character class.
     *
     * @param left the starting point of the range
     * @return the parsed range instance
     */
    private Range parseRange(char left) {
      next(); // Ignore '-'
      char right = advance();
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
      return Range.of(left, right);
    }
  }
}
