#!/usr/bin/env bash
# Kompiluje rdzeń i uruchamia testy samym javac/java (bez Gradle, bez sieci).
set -euo pipefail
cd "$(dirname "$0")/../core"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
javac -Xlint:all -encoding UTF-8 -d "$OUT" $(find src/main/java src/test/java -name '*.java')
cp -r src/main/resources/* "$OUT"/
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp "$OUT" dev.mcgui.TestMain
