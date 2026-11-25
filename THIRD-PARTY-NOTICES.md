# Third-party notices

The original DBS-VocRec application code is AGPL-3.0-only. Third-party sources in this project retain the licenses and copyright notices that accompany those sources.

- **[Java LAME](https://github.com/nwaldispuehl/java-lame)**: upstream revision `0383c5d82e5f3d0acab347498743a74e9e69d256`, vendored under `app/src/main/java/net/sourceforge/lame/`. Upstream distribution uses LGPL v3; several original LAME source headers permit LGPL v2 or later. Those original notices are preserved. [LGPL text](app/src/main/assets/licenses/java-lame-LGPL.txt), [GPL v3 incorporated terms](app/src/main/assets/licenses/GPL-3.0.txt). The source tree and build scripts permit rebuilding with modified codec source.
- **[Concentus](https://github.com/lostromb/concentus)**: upstream revision `9edc8dcd3ae9f3fa4793c03ff3caeaec1add42c5`, vendored under `app/src/main/java/org/concentus/`. [BSD-style license and attribution](app/src/main/assets/licenses/concentus-BSD.txt). 
- Android SDK platform codecs (including AAC and FLAC) are part of the Android system; the app invokes them through platform APIs.
- Gradle wrapper and Android Gradle Plugin are build tools, not included as runtime libraries.

For any public binary release, retain the license notices, provide the exact corresponding source and build instructions, and ensure recipients can rebuild/recombine the modified LGPL library with the app. Do not impose restrictions on debugging modifications to the library. Review any additional bundled upstream source notices and distribution requirements before publishing binaries.
