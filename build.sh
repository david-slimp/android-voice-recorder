#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
if [[ -f /strg/t/android-dev/env.sh ]]; then source /strg/t/android-dev/env.sh; fi
exec ./gradlew --no-daemon assembleDebug lintDebug "$@"
