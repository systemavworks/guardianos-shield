#!/usr/bin/env bash
# scripts/push_tun2socks.sh
# Usage: ./scripts/push_tun2socks.sh /path/to/tun2socks
set -euo pipefail
BIN="$1"
if [ -z "$BIN" ]; then
  echo "Usage: $0 /path/to/tun2socks"
  exit 2
fi
if [ ! -f "$BIN" ]; then
  echo "File not found: $BIN"
  exit 2
fi
DEST="/data/local/tmp/tun2socks"
adb push "$BIN" "$DEST"
adb shell chmod 755 "$DEST"
# Start it in background and redirect output to /sdcard/tun2socks.log
adb shell "nohup $DEST > /sdcard/tun2socks.log 2>&1 &"
echo "Tun2socks pushed and started. Logs: /sdcard/tun2socks.log"
