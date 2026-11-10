#!/bin/bash
# usage: shoot.sh <project> <package> [prefs-name] [prefs-xml-body]
P=$1; PKG=$2; PREF=$3; BODY=$4
OUT=shots/$P; mkdir -p $OUT
adb root; sleep 3
APK=$(ls $P/app/build/outputs/apk/debug/*.apk 2>/dev/null | head -1)
[ -z "$APK" ] && { echo "no apk" > $OUT/BUILD_FAILED.txt; exit 0; }
adb install -r "$APK"
n=0
snap(){ n=$((n+1)); sleep 6; adb exec-out screencap -p > "$OUT/$(printf %02d $n)-$1.png"; }
launch(){ adb shell am force-stop $PKG; adb shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 >/dev/null; }
# tap every clickable labelled element once (skipping actions that send data or log out)
tapall(){
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null; adb pull /sdcard/ui.xml ui.xml >/dev/null
  grep -oE '<node [^>]*>' ui.xml | grep 'clickable="true"' | grep -E '(text|content-desc)="[^"]+"' \
    | grep -viE 'logout|log out|clear|sign in|login|submit|ship' | while read -r l; do
    t=$(echo "$l" | grep -oE '(text|content-desc)="[^"]+"' | head -1 | cut -d'"' -f2 | tr -c 'A-Za-z0-9' '_' | cut -c1-30)
    set -- $(echo "$l" | grep -oE 'bounds="[^"]+"' | grep -oE '[0-9]+')
    adb shell input tap $(( ($1+$3)/2 )) $(( ($2+$4)/2 )); snap "tap-$t"; adb shell input keyevent 4; sleep 2
  done; }
launch; snap first-screen; snap first-screen-later
tapall
if [ -n "$PREF" ]; then
  adb shell am force-stop $PKG
  printf "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>%s</map>\n" "$BODY" > p.xml
  adb push p.xml /data/local/tmp/p.xml
  adb shell "run-as $PKG mkdir -p shared_prefs; run-as $PKG sh -c 'cat /data/local/tmp/p.xml > shared_prefs/$PREF.xml'"
  launch; snap home; tapall
fi
exit 0
