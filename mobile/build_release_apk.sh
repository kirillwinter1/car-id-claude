#!/bin/sh

optstring=":o"
shouldOpenApkDirectory=false

while getopts ${optstring} arg; do
  case ${arg} in
    o)
      shouldOpenApkDirectory=true
      break;;
  esac
done

./update_build_info.sh
flutter clean
#flutter build apk --bundle-sksl-path flutter_01.sksl.android.json --split-per-abi --release
flutter build apk --split-per-abi --release

# If "-o" flag provided then apk source directory will be open after build is done
if [ "$shouldOpenApkDirectory" = true ]
then
  open build/app/outputs/flutter-apk/
fi

