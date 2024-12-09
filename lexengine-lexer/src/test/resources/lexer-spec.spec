# This is a Lexer Specification file to generate the Scanner
# This file consists of 2 parts: config values and regex definitions
# The config value contains properties related to generated scanner such as name of the scanner class, function name etc
# The section is divided by the string '---'

class=MyLexer
package=com.pradeesh.comp.lex
function=next
returnType=Token

---

# Keywords
"int" { return Token.keyword(Token.Type.INT); }

# Identifier
"[a-zA-Z_][a-zA-Z0-9_]*" { return Token.identifier(value); }