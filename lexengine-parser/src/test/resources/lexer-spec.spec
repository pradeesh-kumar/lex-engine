# This is a Lexer Specification file to generate the Scanner
# This file consists of 2 parts: config values and regex definitions
# The config value contains properties related to generated scanner such as name of the scanner class, method name etc
# The section is divided by the string '---'

class=DefaultLexer
package=org.lexengine.parser
methodName=next
returnType=Token

---

# Keywords
"package" { return TestToken.of(TestToken.Type.PACKAGE); }
"import" { return TestToken.of(TestToken.Type.IMPORT); }
"public" { return TestToken.of(TestToken.Type.PUBLIC); }
"class" { return TestToken.of(TestToken.Type.CLASS); }
"private" { return TestToken.of(TestToken.Type.PRIVATE); }
"static" { return TestToken.of(TestToken.Type.STATIC); }
"final" { return TestToken.of(TestToken.Type.FINAL); }
"int" { return TestToken.of(TestToken.Type.INT); }
"if" { return TestToken.of(TestToken.Type.IF); }
"throw" { return TestToken.of(TestToken.Type.THROW); }
"this" { return TestToken.of(TestToken.Type.THIS); }
"new" { return TestToken.of(TestToken.Type.NEW); }

"\." { return TestToken.of(TestToken.Type.DOT); }
"\(" { return TestToken.of(TestToken.Type.OPEN_PAREN); }
"\)" { return TestToken.of(TestToken.Type.CLOSE_PAREN); }
"\{" { return TestToken.of(TestToken.Type.OPEN_BRACE); }
"\}" { return TestToken.of(TestToken.Type.CLOSE_BRACE); }
";" { return TestToken.of(TestToken.Type.SEMICOLON); }
"\"[a-zA-Z0-9 ]*\"" { return TestToken.string(value()); }

"=" { return TestToken.of(TestToken.Type.EQ); }
"\|" { return TestToken.of(TestToken.Type.OR); }
"\|\|" { return TestToken.of(TestToken.Type.DOUBLE_OR); }
"<" { return TestToken.of(TestToken.Type.LESS); }
"<=" { return TestToken.of(TestToken.Type.LESSEQ); }
">" { return TestToken.of(TestToken.Type.GREATER); }
">=" { return TestToken.of(TestToken.Type.GREATEREQ); }
"*" { return TestToken.of(TestToken.Type.MUL); }
"+" { return TestToken.of(TestToken.Type.ADD); }
"-" { return TestToken.of(TestToken.Type.SUB); }
"/" { return TestToken.of(TestToken.Type.DIV); }
"%" { return TestToken.of(TestToken.Type.PERCENTAGE); }


# Identifier
"[a-zA-Z_][a-zA-Z0-9_]*" { return TestToken.identifier(value()); }

# Integer
"[0-9]|[1-9][0-9]*" { return TestToken.integer(value()); }

"[\ \t\b\f\r\n]+" { /* do nothing */ }
