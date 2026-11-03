#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
if ! command -v java >/dev/null 2>&1; then
  echo "ERROR: Java is not installed. Install JDK 21 or newer." >&2
  exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2; exit}')
JAVA_MAJOR=$(printf '%s' "$JAVA_VERSION" | awk -F '[._+-]' '{print $1}')
case "$JAVA_MAJOR" in
  ''|*[!0-9]*) echo "ERROR: Could not determine Java version ($JAVA_VERSION)." >&2; exit 1 ;;
esac
if [ "$JAVA_MAJOR" -lt 21 ]; then
  echo "ERROR: JDK 21 or newer is required. Detected Java $JAVA_VERSION." >&2
  exit 1
fi
exec ./mvnw -U javafx:run
