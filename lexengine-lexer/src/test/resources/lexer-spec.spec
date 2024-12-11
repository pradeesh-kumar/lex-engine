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

# Identifier
"[A-Z0-9]" { return Token.identifier(value); }
#"[a-zA-Z0-9_]*" { return Token.identifier(value); }