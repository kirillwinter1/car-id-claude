#!/bin/sh

echo "\n\n\n"
echo "--------------------------------------------------"
echo "|"
echo "|"
echo "\nTotal Dart code size: "
du -ach ./lib | tail -1
echo "\nTotal lines in Dart code:"
find ./lib -name '*.dart' -type f -print0 | xargs -0 cat | wc -l
echo "\nTotal .dart files:"
find ./lib -name '*.dart' -type f | wc -l
echo "                                                 |"
echo "                                                 |"
echo "--------------------------------------------------"


read -r -p "Wait 5 seconds or press any key to continue immediately" -t 5 -n 1 -s
