# This is a Lexer Specification file to generate the Scanner
# This file consists of 2 parts: config values and regex definitions
# The config value contains properties related to generated scanner such as name of the scanner class, method name etc
# The section is divided by the string '---'

class=MyLexer
package=org.lexengine.lexer.generated
methodName=next
returnType=Token

---

# Keywords
"int" { return Token.keyword(Token.Type.INT); }
"float" { return Token.keyword(Token.Type.FLOAT); }
"new" { return Token.keyword(Token.Type.NEW); }
"not" { return Token.keyword(Token.Type.NOT); }
"for" { return Token.keyword(Token.Type.FOR); }
"if" { return Token.keyword(Token.Type.IF); }
"\{" { return Token.keyword(Token.Type.LBRACE); }
"\[" { return Token.keyword(Token.Type.LSQBRACKET); }
"cat|rat" { return Token.keyword(Token.Type.CATRAT); }
"<" { return Token.keyword(Token.Type.LESSTHAN); }
"<=" { return Token.keyword(Token.Type.LESSTHANOREQ); }

# Identifier
"[a-zA-Z_][a-zA-Z0-9_]*" { return Token.identifier(value); }

# Integer
"[0-9]|[1-9][0-9]*" { return Token.integer(value); }

"[\ \t\b\f\r\n]+" { /* do nothing */ }
