#!/bin/sh

./update_build_info.sh
flutter clean
flutter build apk --release
# --bundle-sksl-path flutter_01.sksl.android.json --release
# --obfuscate --split-debug-info=./obs --release
