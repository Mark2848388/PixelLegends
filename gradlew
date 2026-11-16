#!/bin/sh
# Gradle start script (CI will regenerate wrapper if needed)
DIR="$(cd "$(dirname "$0")" && pwd)"
if [ ! -f "$DIR/gradle/wrapper/gradle-wrapper.jar" ]; then
  echo "Downloading gradle wrapper jar..."
  curl -sL -o "$DIR/gradle/wrapper/gradle-wrapper.jar" \
    https://raw.githubusercontent.com/gradle/gradle/v8.9.0/gradle/wrapper/gradle-wrapper.jar \
    || curl -sL -o "$DIR/gradle/wrapper/gradle-wrapper.jar" \
    https://github.com/gradle/gradle/raw/v8.9.0/gradle/wrapper/gradle-wrapper.jar
fi
exec java -jar "$DIR/gradle/wrapper/gradle-wrapper.jar" "$@"
