#!/bin/sh

./update_build_info.sh
# flutter build appbundle --target-platform android-arm,android-arm64,android-x64 --release
flutter clean
flutter build appbundle --target-platform android-arm,android-arm64 --release 
# flutter build appbundle --bundle-sksl-path flutter_01.sksl.android.json --target-platform android-arm,android-arm64 --release 
# --obfuscate --split-debug-info=./obs
