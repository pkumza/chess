#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
export ANDROID_USER_HOME="$PWD/.tools/android-user"
exec .tools/android-sdk/platform-tools/adb "$@"
