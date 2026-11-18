#!/bin/sh
# Runs AUCA_ID (compiles first when "out" does not exist).
cd "$(dirname "$0")" || exit 1
[ -d out ] || ./compile.sh || exit 1
java -cp out com.auca.main.Main
