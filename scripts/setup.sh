#!/bin/bash
# macOS Apple Silicon local toolchain; no system package manager or global config changes.
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p .tools/downloads .tools/jdk .tools/android-sdk/cmdline-tools
if [[ ! -x .tools/jdk/Contents/Home/bin/java ]]; then
    curl -fL --retry 2 -o .tools/downloads/jdk.tar.gz 'https://api.adoptium.net/v3/binary/version/jdk-17.0.20.1%2B1/mac/aarch64/jdk/hotspot/normal/eclipse'
    tar -xzf .tools/downloads/jdk.tar.gz -C .tools/jdk --strip-components=1
fi
if [[ ! -x .tools/gradle-8.9/bin/gradle ]]; then
    curl -fL --retry 2 -o .tools/downloads/gradle.zip https://services.gradle.org/distributions/gradle-8.9-bin.zip
    unzip -q .tools/downloads/gradle.zip -d .tools
fi
if [[ ! -x .tools/android-sdk/cmdline-tools/latest/bin/sdkmanager ]]; then
    curl -fL --retry 2 -o .tools/downloads/commandlinetools.zip https://dl.google.com/android/repository/commandlinetools-mac-11076708_latest.zip
    unzip -q .tools/downloads/commandlinetools.zip -d .tools/android-sdk/cmdline-tools
    mv .tools/android-sdk/cmdline-tools/cmdline-tools .tools/android-sdk/cmdline-tools/latest
fi
export JAVA_HOME="$PWD/.tools/jdk/Contents/Home"
export ANDROID_USER_HOME="$PWD/.tools/android-user"
.tools/android-sdk/cmdline-tools/latest/bin/sdkmanager --sdk_root="$PWD/.tools/android-sdk" 'platform-tools' 'platforms;android-35' 'build-tools;34.0.0'
