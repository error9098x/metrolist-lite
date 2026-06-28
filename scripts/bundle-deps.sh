#!/usr/bin/env bash
#
# Assemble a self-contained bin/ with mpv (+ its dylibs) and a standalone yt-dlp,
# so the packaged app needs no Homebrew installs. Output: desktop-lite/build/bundle/bin
#
#   mpv          (rpaths rewritten to @executable_path/libs)
#   libs/*.dylib (mpv's non-system dependencies, vendored)
#   yt-dlp       (official single-file macOS build)
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

BIN_OUT="${1:-$ROOT/desktop-lite/build/bundle/bin}"
command -v dylibbundler >/dev/null || { echo "ERROR: dylibbundler not found (brew install dylibbundler)"; exit 1; }

echo "==> Staging $BIN_OUT"
rm -rf "$BIN_OUT"
mkdir -p "$BIN_OUT/libs"

# --- mpv + vendored dylibs ---
MPV_SRC="$(command -v mpv || echo /opt/homebrew/bin/mpv)"
MPV_SRC="$(readlink -f "$MPV_SRC" 2>/dev/null || echo "$MPV_SRC")"
[ -x "$MPV_SRC" ] || { echo "ERROR: mpv not found (brew install mpv)"; exit 1; }
echo "==> Copying mpv from $MPV_SRC"
cp "$MPV_SRC" "$BIN_OUT/mpv"; chmod +x "$BIN_OUT/mpv"

echo "==> Vendoring mpv dylibs with dylibbundler"
dylibbundler -of -b -x "$BIN_OUT/mpv" -d "$BIN_OUT/libs" -p "@executable_path/libs/" </dev/null

# --- yt-dlp standalone binary ---
echo "==> Downloading yt-dlp_macos"
curl -fL --retry 3 -o "$BIN_OUT/yt-dlp" \
  "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_macos"
chmod +x "$BIN_OUT/yt-dlp"

echo "==> Bundle ready:"
du -sh "$BIN_OUT"
echo "    mpv deps remaining outside the bundle (should be system-only):"
otool -L "$BIN_OUT/mpv" | awk 'NR>1{print $1}' | grep -vE '^@executable_path|/usr/lib/|/System/' || echo "    (none)"
