#!/usr/bin/env bash
set -eu
cd "$(dirname "$0")/.."
mkdir -p core/build/classes
find core/src/main/java core/src/test/java -name '*.java' > core/build/sources.txt
java com.sun.tools.javac.Main --release 8 -encoding UTF-8 -d core/build/classes @core/build/sources.txt
java -cp core/build/classes br.com.coletafacil.core.CoreTests core/build/test-exports
