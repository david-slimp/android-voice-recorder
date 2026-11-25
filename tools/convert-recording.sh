#!/usr/bin/env bash
# Desktop export, no changes to original.
set -euo pipefail
if [[ $# -ne 2 ]]; then echo "Usage: $0 input.wav flac|mp3|ogg" >&2; exit 2; fi
input=$1
format=$2
case "$format" in
 flac) options=(-c:a flac);;
 mp3) options=(-c:a libmp3lame -b:a 128k);;
 ogg) options=(-c:a libvorbis -q:a 4);;
 *) echo "Unsupported format" >&2; exit 2;;
esac
output="${input%.*}.$format"
[[ "$input" != "$output" && ! -e "$output" ]] || { echo "Output exists or would replace input: $output" >&2; exit 1; }
ffmpeg -n -v error -i "$input" "${options[@]}" "$output"
ffprobe -v error -show_entries format=duration,size:stream=codec_name,sample_rate,channels -of json "$output"
