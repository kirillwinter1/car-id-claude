#!/bin/sh

./update_build_info.sh
flutter clean
flutter build ipa 
#--bundle-sksl-path flutter_01.sksl.ios.json
