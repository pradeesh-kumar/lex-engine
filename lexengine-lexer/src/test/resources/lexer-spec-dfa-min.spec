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
"fee" { return Token.keyword(Token.Type.FEE); }
"fie" { return Token.keyword(Token.Type.FIE); }
