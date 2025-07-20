# This is a Lexer Specification file to generate the Scanner.
# This file consists of 2 parts: config values and regex definitions.
# The config value contains properties related to generated scanner such as name of the scanner class, method name etc.
# Each section is separated by the delimiter '---'
# Line starts with '#' are treated as comments.

# Metadata Section
class=MyLexer
package=org.lexengine.lexer.generated;
methodName=next
returnType=Token

---
# Regular Expression and Action Section
# Keywords
"int" { return Token.keyword(Token.Type.INT); }
"byte" { return Token.keyword(Token.Type.BYTE); }
"for" { return Token.keyword(Token.Type.FOR); }
"new" { return Token.keyword(Token.Type.NEW); }
"not" { return Token.keyword(Token.Type.NOT); }

# Arithmetic Operators
"+" { return Token.operator(Token.Type.ADD); }
"-" { return Token.operator(Token.Type.SUB); }
"*" { return Token.operator(Token.Type.MUL); }
"/" { return Token.operator(Token.Type.DIV); }
"%" { return Token.operator(Token.Type.PER); }

# Identifier
"[a-zA-Z_][a-zA-Z0-9_]*" { return Token.identifier(value); }