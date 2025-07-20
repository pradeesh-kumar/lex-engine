# This is a Lexer Specification file to generate the Scanner
# This file consists of 2 parts: config values and regex definitions
# The config value contains properties related to generated scanner such as name of the scanner class, method name etc
# The section is divided by the string '---'

class=MyLexer
package=org.lexengine.lexer.gentest
methodName=next
returnType=Token

---

# Keywords
"package" { return Token.of(Token.Type.PACKAGE); }
"import" { return Token.of(Token.Type.IMPORT); }
"public" { return Token.of(Token.Type.PUBLIC); }
"class" { return Token.of(Token.Type.CLASS); }
"private" { return Token.of(Token.Type.PRIVATE); }
"static" { return Token.of(Token.Type.STATIC); }
"final" { return Token.of(Token.Type.FINAL); }
"int" { return Token.of(Token.Type.INT); }
"if" { return Token.of(Token.Type.IF); }
"throw" { return Token.of(Token.Type.THROW); }
"this" { return Token.of(Token.Type.THIS); }
"new" { return Token.of(Token.Type.NEW); }

"\." { return Token.of(Token.Type.DOT); }
"\(" { return Token.of(Token.Type.OPEN_PAREN); }
"\)" { return Token.of(Token.Type.CLOSE_PAREN); }
"\{" { return Token.of(Token.Type.OPEN_BRACE); }
"\}" { return Token.of(Token.Type.CLOSE_BRACE); }
";" { return Token.of(Token.Type.SEMICOLON); }
"\"[a-zA-Z0-9 ]*\"" { return Token.string(value()); }

"=" { return Token.of(Token.Type.EQ); }
"\|" { return Token.of(Token.Type.OR); }
"\|\|" { return Token.of(Token.Type.DOUBLE_OR); }
"<" { return Token.of(Token.Type.LESS); }
"<=" { return Token.of(Token.Type.LESSEQ); }
">" { return Token.of(Token.Type.GREATER); }
">=" { return Token.of(Token.Type.GREATEREQ); }
"*" { return Token.of(Token.Type.MUL); }
"+" { return Token.of(Token.Type.ADD); }
"-" { return Token.of(Token.Type.SUB); }
"/" { return Token.of(Token.Type.DIV); }
"%" { return Token.of(Token.Type.PERCENTAGE); }


# Identifier
"[a-zA-Z_][a-zA-Z0-9_]*" { return Token.identifier(value()); }

# Integer
"[0-9]|[1-9][0-9]*" { return Token.integer(value()); }

"[\ \t\b\f\r\n]+" { /* do nothing */ }
