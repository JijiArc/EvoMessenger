#!/usr/bin/env sh
# Runs the plain-Kotlin checks for the voice effects and the link scanner logic on a normal JVM.
# No Android SDK is needed. Requires `kotlinc` (https://kotlinlang.org/docs/command-line.html) and Java.
set -e
cd "$(dirname "$0")"
SRC=../../app/src/main/java/com/mathroda/messengerclone
OUT="$(mktemp -d)"

kotlinc "$SRC/voice/VoiceEffect.kt" "$SRC/voice/AudioEffects.kt" AudioEffectsCheck.kt \
  -include-runtime -d "$OUT/effects.jar"
java -cp "$OUT/effects.jar" AudioEffectsCheckKt

kotlinc "$SRC/scanner/UrlValidator.kt" "$SRC/scanner/ScanVerdict.kt" ScannerCheck.kt \
  -include-runtime -d "$OUT/scanner.jar"
java -cp "$OUT/scanner.jar" ScannerCheckKt

rm -rf "$OUT"
