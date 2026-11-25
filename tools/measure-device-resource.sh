#!/usr/bin/env bash
# Measures an already-running app, does not start or stop recording.
set -euo pipefail
if [[ $# -lt 1 || $# -gt 2 ]]; then echo "Usage: $0 package [seconds]" >&2; exit 2; fi
case "$1" in com.rock808.voicerecorder.test|com.media.bestrecorder.audiorecorder) ;; *) echo "Unexpected package" >&2; exit 1;; esac
seconds=${2:-12}
[[ "$seconds" =~ ^[0-9]+$ ]] && ((seconds >= 1 && seconds <= 120)) || exit 2
if [[ -f /strg/t/android-dev/env.sh ]]; then source /strg/t/android-dev/env.sh; fi
pkg=$1
pid=$(adb shell pidof "$pkg" | tr -d '\r')
[[ "$pid" =~ ^[0-9]+$ ]] || { echo "Expected one active process" >&2; exit 1; }
echo "Package $pkg; PID $pid"
adb shell dumpsys meminfo "$pkg" | grep -E 'TOTAL|Native Heap|Java Heap|Graphics|Code|Stack|Private Other' | head -15
adb shell "cat /proc/$pid/stat; cat /proc/uptime; sleep $seconds; cat /proc/$pid/stat; cat /proc/uptime" | python3 -c '
import sys
s=sys.stdin.read().splitlines()
def ticks(t):
    parts=t.rsplit(")",1)[1].split()
    return int(parts[11])+int(parts[12])
t0,t1=map(lambda x: float(x.split()[0]),[s[1],s[3]])
delta=ticks(s[2])-ticks(s[0])
print(f"CPU ticks: {delta}, interval: {t1-t0:.2f} s; estimated CPU (100Hz ticks, one core): {delta/(t1-t0):.2f}%")
'
