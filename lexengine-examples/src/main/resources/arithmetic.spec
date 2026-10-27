# Standalone DFA scanner; actions use the companion org.lexengine.examples.lexer.Token.
"[a-zA-Z_][a-zA-Z0-9_]*" { return Token.identifier(value); }
"[0-9]+" { return Token.number(value); }
"\+" { return Token.symbol("+", value); }
"-" { return Token.symbol("-", value); }
"\*" { return Token.symbol("*", value); }
"/" { return Token.symbol("/", value); }
"\(" { return Token.symbol("(", value); }
"\)" { return Token.symbol(")", value); }
"[ \t\r\n]+" { /* skip whitespace */ }
