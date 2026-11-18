#!/bin/sh
# Compiles the whole AUCA_ID project into the "out" folder (needs a JDK 8 or newer).
cd "$(dirname "$0")" || exit 1
rm -rf out && mkdir out
find src -name '*.java' > sources.txt
javac -encoding UTF-8 -d out @sources.txt && echo "Compilation OK -> out/"
rm -f sources.txt
