#!/usr/bin/env bash
# Builds, installs and opens the app on an iPhone connected to this Mac (cable or
# same Wi-Fi). It builds for any iPhone, so it does not wait for Xcode to finish
# preparing the device; devicectl does the install. DEVICE=<name or id> picks one when several are connected.
# Signing uses DEVELOPMENT_TEAM from keys/ios.properties, or TEAM=<id>.
set -euo pipefail
cd "$(dirname "$0")/.."
. scripts/lib.sh

APP_ID=com.casero.cu
BUILD=build/ios-device
LOG=$BUILD/xcodebuild.log

command -v xcodebuild >/dev/null || fail "Xcode is not installed." "Install Xcode from the App Store and open it once."
command -v xcodegen >/dev/null || fail "XcodeGen is not installed." "brew install xcodegen"
xcrun devicectl --version >/dev/null 2>&1 || fail "devicectl is missing (needs Xcode 15 or later)." "Update Xcode."

team="${TEAM:-$(prop keys/ios.properties DEVELOPMENT_TEAM)}"
if [ -z "$team" ]; then
    teams=$(defaults read com.apple.dt.Xcode IDEProvisioningTeamByIdentifier 2>/dev/null \
        | awk -F' = ' '/teamID/ { gsub(/[;"]/, "", $2); id = $2 } /teamName/ { gsub(/[;"]/, "", $2); print "    " id "  " $2 }' || true)
    fail "No Apple development team set." "Copy keys/ios.properties.example to keys/ios.properties and set DEVELOPMENT_TEAM, or pass TEAM=<id>. Teams signed in to Xcode:
$teams"
fi

mkdir -p "$BUILD"
json="$BUILD/devices.json"
# Prints OK/NONE/MANY plus the chosen iPhone, from devicectl's JSON.
pick_iphone() {
python3 -I - "$json" "${DEVICE:-}" <<'PY'
import json, sys
devices = json.load(open(sys.argv[1]))["result"]["devices"]
wanted = sys.argv[2].lower()
phones = [d for d in devices if d["hardwareProperties"].get("platform") == "iOS"]
def label(d): return d["deviceProperties"].get("name", "?")
if wanted:
    phones = [d for d in phones if wanted in (label(d).lower(), d["identifier"].lower(), d["hardwareProperties"].get("udid", "").lower())]
ready = [d for d in phones if d["connectionProperties"].get("tunnelState") == "connected"]
if not ready:
    print("NONE\t" + "; ".join(f'{label(d)} ({d["connectionProperties"].get("tunnelState", "?")})' for d in phones))
elif len(ready) > 1:
    print("MANY\t" + "; ".join(label(d) for d in ready))
else:
    d = ready[0]
    print("\t".join(["OK", d["identifier"], d["hardwareProperties"].get("udid", ""), label(d)]))
PY
}

# An iPhone that is reconnecting shows as "connecting"; give it up to 30 s.
for attempt in 1 2 3 4 5 6 7; do
    xcrun devicectl list devices --json-output "$json" >/dev/null 2>&1 || fail "Could not list the iPhones." "Open Xcode once (it finishes installing device support), then run again."
    selection=$(pick_iphone)
    case "$selection" in NONE*connecting*) [ "$attempt" -lt 7 ] && { [ "$attempt" = 1 ] && step "Waiting for the iPhone to reconnect…"; sleep 5; continue; } ;; esac
    break
done
IFS=$'\t' read -r status id udid name <<<"$selection"
case "$status" in
    NONE) fail "No iPhone connected${DEVICE:+ named $DEVICE}." "Connect it by cable (or the same Wi-Fi), unlock it and tap \"Trust\". Developer Mode must be on: Settings → Privacy & Security → Developer Mode. Known iPhones: ${id:-none}" ;;
    MANY) fail "Several iPhones are connected: $id." "Pick one: make install ios DEVICE=\"<name>\"" ;;
esac

step "Generating the Xcode project"
(cd iosApp && xcodegen generate --quiet) || fail "XcodeGen could not generate the project." "Check iosApp/project.yml."

step "Building for $name (team $team) — this takes a few minutes the first time"
if ! xcodebuild -project iosApp/CaseroCu.xcodeproj -scheme CaseroCu -configuration Debug \
        -destination "generic/platform=iOS" -derivedDataPath "$BUILD" \
        -allowProvisioningUpdates DEVELOPMENT_TEAM="$team" CODE_SIGN_STYLE=Automatic \
        build >"$LOG" 2>&1; then
    grep -E "error:|e: |What went wrong" "$LOG" | sort -u | head -20 >&2 || true
    if grep -qE "No profiles for|requires a provisioning profile|No Account for Team|Signing for" "$LOG"; then
        fail "Signing failed for team $team." "Open Xcode → Settings → Accounts and sign in with the Apple ID of that team. Then open iosApp/CaseroCu.xcodeproj → target CaseroCu → Signing & Capabilities once, so Xcode creates the profile. Full log: $LOG"
    fi
    if grep -qE "Device is busy|Preparing|Timed out waiting|is not available|Unable to find a destination" "$LOG"; then
        fail "Xcode cannot build for $name." "Unlock the iPhone, keep it connected, and wait until Xcode → Window → Devices and Simulators finishes \"Preparing\". Full log: $LOG"
    fi
    fail "The iOS build failed." "The errors are above. Full log: $LOG"
fi
app="$BUILD/Build/Products/Debug-iphoneos/CaseroCu.app"

step "Installing on $name"
if ! out=$(xcrun devicectl device install app --device "$id" "$app" 2>&1); then
    printf '%s\n' "$out" | grep -iE "error|fail" | head -5 >&2
    fail "Could not install on $name." "Unlock the iPhone and run again. If it is out of space or a different CASEROcu is installed, delete it from the iPhone first."
fi

if ! out=$(xcrun devicectl device process launch --device "$id" "$APP_ID" 2>&1); then
    if printf '%s' "$out" | grep -qiE "untrusted|not been explicitly trusted|profile"; then
        fail "Installed, but iOS does not trust the developer yet." "On the iPhone: Settings → General → VPN & Device Management → your Apple ID → Trust. Then open CASEROcu."
    fi
    if printf '%s' "$out" | grep -qi "locked"; then
        fail "Installed, but the iPhone is locked." "Unlock it and open CASEROcu."
    fi
    warn "Installed, but the app did not open: $(printf '%s' "$out" | grep -iE "error" | head -1)"
    exit 0
fi
done_ "CASEROcu installed and opened on $name"
if defaults read com.apple.dt.Xcode IDEProvisioningTeamByIdentifier 2>/dev/null | grep -B1 "teamID = $team;" | grep -q "isFreeProvisioningTeam = 1"; then
    warn "Personal (free) team: iOS stops opening the app after 7 days. Run this again to renew it."
fi
exit 0
