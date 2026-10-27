#!/bin/sh
set -eu
lex_engine_root=$(CDPATH= cd -P "$(dirname "$0")/.." && pwd)
exec mvn -f "$lex_engine_root/pom.xml" -pl lexengine-cli -am "$@" package
