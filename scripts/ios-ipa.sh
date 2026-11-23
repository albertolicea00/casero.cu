#!/usr/bin/env bash
# Archives the app in Release and exports build/ipa/casero-v<version>-<build>-release.ipa.
# Version and build come from MARKETING_VERSION and CURRENT_PROJECT_VERSION in iosApp/project.yml.
# Signing uses DEVELOPMENT_TEAM from keys/ios.properties, or TEAM=<id>.
# IPA_METHOD picks the export: debugging (default for a free personal team: your own devices,
# expires after 7 days), release-testing (Ad Hoc) or app-store-connect (default for a paid team).
set -euo pipefail
cd "$(dirname "$0")/.."
. scripts/lib.sh

BUILD=build/ios-archive
OUT=build/ipa
LOG=$BUILD/xcodebuild.log

command -v xcodebuild >/dev/null || fail "Xcode is not installed." "Install Xcode from the App Store and open it once."
command -v xcodegen >/dev/null || fail "XcodeGen is not installed." "brew install xcodegen"

team="${TEAM:-$(prop keys/ios.properties DEVELOPMENT_TEAM)}"
[ -n "$team" ] || fail "No Apple development team set." "Copy keys/ios.properties.example to keys/ios.properties and set DEVELOPMENT_TEAM, or pass TEAM=<id>."

version=$(sed -n 's/^[[:space:]]*MARKETING_VERSION:[[:space:]]*"\{0,1\}\([^"]*\)"\{0,1\}[[:space:]]*$/\1/p' iosApp/project.yml | head -1)
build=$(sed -n 's/^[[:space:]]*CURRENT_PROJECT_VERSION:[[:space:]]*"\{0,1\}\([^"]*\)"\{0,1\}[[:space:]]*$/\1/p' iosApp/project.yml | head -1)
[ -n "$version" ] && [ -n "$build" ] || fail "No version in iosApp/project.yml." "Set MARKETING_VERSION and CURRENT_PROJECT_VERSION under settings.base."

free=0
defaults read com.apple.dt.Xcode IDEProvisioningTeamByIdentifier 2>/dev/null | grep -B1 "teamID = $team;" | grep -q "isFreeProvisioningTeam = 1" && free=1
method="${IPA_METHOD:-$([ "$free" = 1 ] && echo debugging || echo app-store-connect)}"
[ "$free" = 1 ] && [ "$method" != debugging ] && fail "A free personal team can only export IPA_METHOD=debugging." "Join the Apple Developer Program for Ad Hoc, TestFlight or App Store builds."

ipa="$OUT/casero-v$version-$build-release.ipa"
rm -rf "$BUILD"
mkdir -p "$BUILD" "$OUT"

step "Generating the Xcode project"
(cd iosApp && xcodegen generate --quiet) || fail "XcodeGen could not generate the project." "Check iosApp/project.yml."

step "Archiving $version ($build) in Release (team $team) — this takes a few minutes"
if ! xcodebuild -project iosApp/CaseroCu.xcodeproj -scheme CaseroCu -configuration Release \
        -destination "generic/platform=iOS" -archivePath "$BUILD/CaseroCu.xcarchive" \
        -allowProvisioningUpdates DEVELOPMENT_TEAM="$team" CODE_SIGN_STYLE=Automatic \
        archive >"$LOG" 2>&1; then
    grep -E "error:|e: |What went wrong" "$LOG" | sort -u | head -20 >&2 || true
    if grep -qE "No profiles for|requires a provisioning profile|No Account for Team|Signing for" "$LOG"; then
        fail "Signing failed for team $team." "Open Xcode → Settings → Accounts and sign in with the Apple ID of that team, then run make install ios once so Xcode creates the profile. Full log: $LOG"
    fi
    fail "The archive failed." "The errors are above. Full log: $LOG"
fi

cat >"$BUILD/ExportOptions.plist" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>method</key><string>$method</string>
    <key>teamID</key><string>$team</string>
    <key>signingStyle</key><string>automatic</string>
</dict>
</plist>
PLIST

step "Exporting the IPA ($method)"
if ! xcodebuild -exportArchive -archivePath "$BUILD/CaseroCu.xcarchive" -exportPath "$BUILD/export" \
        -exportOptionsPlist "$BUILD/ExportOptions.plist" -allowProvisioningUpdates >>"$LOG" 2>&1; then
    grep -E "error:|error " "$LOG" | sort -u | tail -10 >&2 || true
    fail "The export failed." "The errors are above. Full log: $LOG"
fi
mv "$BUILD/export/CaseroCu.ipa" "$ipa"
done_ "$ipa"
[ "$free" = 1 ] && warn "Personal (free) team: the IPA installs only on your registered devices and stops opening after 7 days."
exit 0
