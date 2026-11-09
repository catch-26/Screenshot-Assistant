#!/bin/bash
set -x
PKG=de.paradox.screenshotassistant
adb shell settings put global sysui_demo_allowed 1
demo() { adb shell am broadcast -a com.android.systemui.demo -e command "$@"; }
demo enter
demo clock -e hhmm 1200
demo battery -e level 100 -e plugged false
demo network -e wifi show -e level 4 -e fully true
demo network -e mobile show -e datatype none -e level 4 -e fully true
demo notifications -e visible false
adb shell cmd role add-role-holder android.app.role.ASSISTANT $PKG 0
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS
mkdir -p out
for mode in yes; do
  adb shell cmd uimode night $mode
  sleep 3
  demo enter
  demo clock -e hhmm 1200
  adb shell am instrument -w -e class de.beatbrot.screenshotassistant.ReadmeScreenshots -e night $mode $PKG.test/androidx.test.runner.AndroidJUnitRunner | tee -a instrument.log
  adb shell input keyevent KEYCODE_HOME
done
adb pull /sdcard/Android/data/$PKG/files/readme/. out/
ls -la out
