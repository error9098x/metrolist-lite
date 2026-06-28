#!/usr/bin/env bash
#
# Build a self-contained macOS .dmg for Metrolist Lite using jpackage.
# The bundled app includes a Java runtime; users still need `mpv` and `yt-dlp`
# on their PATH (brew install mpv yt-dlp).
#
# Usage: scripts/package-dmg.sh [version]   (default 1.0.0)
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

VERSION="${1:-1.0.0}"
APP_NAME="Metrolist Lite"
MAIN_CLASS="com.metrolist.desktop.MainKt"

echo "==> Building distribution (v$VERSION)"
./gradlew :desktop-lite:installDist -Pversion="$VERSION" --console=plain

LIB_DIR="desktop-lite/build/install/desktop-lite/lib"
MAIN_JAR="$(cd "$LIB_DIR" && ls | grep -E '^desktop-lite.*\.jar$' | head -1)"
[ -n "$MAIN_JAR" ] || { echo "ERROR: main jar not found in $LIB_DIR"; exit 1; }
echo "    main jar: $MAIN_JAR"

echo "==> Generating .icns from icon.png"
ICON_PNG="desktop-lite/src/main/resources/icon.png"
WORK="$(mktemp -d)"
ICONSET="$WORK/app.iconset"
mkdir -p "$ICONSET"
for s in 16 32 128 256 512; do
  sips -z "$s" "$s"   "$ICON_PNG" --out "$ICONSET/icon_${s}x${s}.png"    >/dev/null
  d=$((s * 2))
  sips -z "$d" "$d"   "$ICON_PNG" --out "$ICONSET/icon_${s}x${s}@2x.png" >/dev/null
done
ICNS="$WORK/app.icns"
iconutil -c icns "$ICONSET" -o "$ICNS"

echo "==> Bundling mpv + yt-dlp (self-contained, no Homebrew needed at runtime)"
BIN_OUT="$ROOT/desktop-lite/build/bundle/bin"
scripts/bundle-deps.sh "$BIN_OUT"

OUT="build/dist"
mkdir -p "$OUT"
rm -f "$OUT"/*.dmg

echo "==> jpackage (bundles a Java runtime + mpv + yt-dlp; takes a couple minutes)"
jpackage \
  --type dmg \
  --name "$APP_NAME" \
  --app-version "$VERSION" \
  --input "$LIB_DIR" \
  --main-jar "$MAIN_JAR" \
  --main-class "$MAIN_CLASS" \
  --icon "$ICNS" \
  --mac-package-name "$APP_NAME" \
  --app-content "$BIN_OUT" \
  --java-options "-Djava.awt.headless=false" \
  --java-options "-Dapple.awt.application.appearance=system" \
  --java-options "-Dapple.laf.useScreenMenuBar=true" \
  --dest "$OUT"

echo "==> Done:"
ls -la "$OUT"/*.dmg
