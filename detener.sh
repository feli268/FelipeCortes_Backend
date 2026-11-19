#!/usr/bin/env bash
cd "$(dirname "$0")"
if [ -f logs/pids.txt ]; then
  xargs kill < logs/pids.txt 2>/dev/null || true
  rm -f logs/pids.txt
  echo "Servicios detenidos"
fi
