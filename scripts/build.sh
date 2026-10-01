#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
export JAVA_HOME="$PWD/.tools/jdk/Contents/Home"
export ANDROID_HOME="$PWD/.tools/android-sdk"
export ANDROID_USER_HOME="$PWD/.tools/android-user"
export GRADLE_USER_HOME="$PWD/.tools/gradle-home"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
if [[ -f .signing/password ]]; then export CHESS_KEY_PASSWORD="$(cat .signing/password)"; fi
exec .tools/gradle-8.9/bin/gradle --no-daemon "$@"
