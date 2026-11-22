#!/usr/bin/env bash
# Builds, installs and opens the app on the Android phones connected to this Mac
# (USB or wireless debugging). Emulators are skipped. DEVICE=<serial> picks one.
set -euo pipefail
cd "$(dirname "$0")/.."
. scripts/lib.sh

APP_ID=com.casero.cu
ACTIVITY=com.casero.cu.android.MainActivity
ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
[ -x "$ADB" ] || fail "adb not found at $ADB." "Install the Android SDK (Android Studio → SDK Manager) or set ANDROID_HOME."

phones=$("$ADB" devices | awk 'NR > 1 && NF >= 2 && $1 !~ /^emulator-/ { print $1 " " $2 }')
if [ -n "${DEVICE:-}" ]; then phones=$(printf '%s\n' "$phones" | awk -v d="$DEVICE" '$1 == d'); fi

if printf '%s\n' "$phones" | grep -q " unauthorized$"; then
    fail "The phone has not authorized this Mac." "Unlock the phone and accept \"Allow USB debugging?\", then run again."
fi
if printf '%s\n' "$phones" | grep -q " offline$"; then
    fail "The phone is connected but offline." "Unplug and plug it in again (or turn wireless debugging off and on)."
fi
serials=$(printf '%s\n' "$phones" | awk '$2 == "device" { print $1 }')
if [ -z "$serials" ]; then
    if [ -n "${DEVICE:-}" ]; then
        fail "No connected Android phone with serial $DEVICE." "Run \`make devices\` to see the serials."
    fi
    fail "No Android phone connected." "On the phone: Settings → About phone → tap Build number 7 times, then Developer options → USB debugging. Connect it by cable and accept the prompt. For Wi-Fi: Developer options → Wireless debugging → \`adb pair\`."
fi

for serial in $serials; do
    name=$("$ADB" -s "$serial" shell getprop ro.product.model 2>/dev/null | tr -d '\r')
    step "Installing on ${name:-$serial}"
    if ! log=$(ANDROID_SERIAL="$serial" ./gradlew -q :androidApp:installDebug 2>&1); then
        printf '%s\n' "$log" | grep -E "e: |error|FAILURE|What went wrong" -A3 | head -30 >&2
        if printf '%s' "$log" | grep -q "INSTALL_FAILED_UPDATE_INCOMPATIBLE"; then
            fail "${name:-$serial} has another build of CASEROcu signed with a different key." "Uninstall it first: $ADB -s $serial uninstall $APP_ID"
        fi
        if printf '%s' "$log" | grep -q "INSTALL_FAILED_USER_RESTRICTED"; then
            fail "${name:-$serial} blocked the install." "Accept the install prompt on the phone (Xiaomi/Samsung: allow \"Install via USB\" in Developer options)."
        fi
        fail "Could not build or install on ${name:-$serial}." "The errors are above. \`./gradlew :androidApp:installDebug\` shows the full output."
    fi
    "$ADB" -s "$serial" shell am start -n "$APP_ID/$ACTIVITY" >/dev/null || warn "Installed, but the app did not open. Unlock the phone and open CASEROcu."
    done_ "CASEROcu installed and opened on ${name:-$serial}"
done
