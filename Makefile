# CASERO — common tasks. Run `make` for the list.
#
#   make run                  Android app on every connected device/emulator (default)
#   make run android          Android app on every connected device/emulator
#   make run ios              iOS app on a simulator
#   make run android DEVICE=<serial>  one Android device (serials: `make devices`)
#   make install android      your physical Android phones
#   make install ios          your iPhone (team from keys/ios.properties or TEAM=<id>)
#   make test                 shared tests on Android (JVM) and the iOS simulator

# `make run ios`, `make install android`… name the platform (or PLATFORM=ios). There
# is no default: running or installing always says where.
PLATFORM ?= $(firstword $(filter ios android,$(MAKECMDGOALS)))
NEED_PLATFORM = $(if $(filter ios android,$(PLATFORM)),,. scripts/lib.sh && fail "Say which platform: make $(1) ios  or  make $(1) android")
APP_ID := com.casero.cu
ACTIVITY := com.casero.cu.android.MainActivity
GRADLE := ./gradlew
ANDROID_HOME ?= $(HOME)/Library/Android/sdk
ADB := $(ANDROID_HOME)/platform-tools/adb
# Empty means every connected device; Gradle's install task honours ANDROID_SERIAL too.
DEVICE ?=
export ANDROID_SERIAL := $(DEVICE)
EMULATOR := $(ANDROID_HOME)/emulator/emulator
# First installed AVD unless one is given: make run AVD=Pixel_8
FAIL := . scripts/lib.sh && fail
AVD ?= $(shell $(EMULATOR) -list-avds 2>/dev/null | head -1)
# Simulator name as listed by `xcrun simctl list devices available`
SIMULATOR ?= iPhone 17 Pro Max
IOS_PROJECT := iosApp/CaseroCu.xcodeproj
IOS_BUILD := build/ios
IOS_APP := $(IOS_BUILD)/Build/Products/Debug-iphonesimulator/CaseroCu.app

.DEFAULT_GOAL := help
.PHONY: help run install build test test-device check clean ios android \
	android-build android-install android-run android-release android-release-apk emulator android-device devices \
	android-device-install ios-project ios-build ios-run ios-device-install simulator

help: ## List the available tasks
	@awk 'BEGIN {FS = ":.*## "} /^[a-zA-Z_-]+:.*## / {printf "  \033[36m%-24s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

run: ## Run the app: `make run ios` or `make run android` (DEVICE=<serial>)
	@$(call NEED_PLATFORM,run)
	@$(MAKE) --no-print-directory $(PLATFORM)-run

install: ## Install on your physical phone: `make install ios` or `make install android`
	@$(call NEED_PLATFORM,install)
	@$(MAKE) --no-print-directory $(PLATFORM)-device-install

# Platform words after `run` / `install`; alone they do nothing.
ios android:
	@:

build: android-build ios-build ## Build both apps (debug)

test: ## Run the shared tests on Android (JVM) and the iOS simulator
	$(GRADLE) :shared:testDebugUnitTest :shared:iosSimulatorArm64Test

test-device: ## Android tests on a device: Keystore, PDF, bundled certificate (DEVICE=<serial>)
	@$(MAKE) --no-print-directory android-device
	$(GRADLE) :androidApp:connectedDebugAndroidTest

check: ## Tests, Android lint and whitespace check
	$(GRADLE) :shared:testDebugUnitTest :shared:iosSimulatorArm64Test :androidApp:lintDebug
	git diff --check

clean: ## Remove build outputs and the generated Xcode project
	$(GRADLE) clean
	rm -rf $(IOS_BUILD) $(IOS_PROJECT)

# --- Android -----------------------------------------------------------------

android-build: ## Build the Android debug APK
	$(GRADLE) :androidApp:assembleDebug

android-release: ## Build the signed Android release bundle (.aab; needs keys/release.properties)
	$(GRADLE) :androidApp:bundleRelease

android-release-apk: ## Build the signed Android release APK (needs keys/release.properties)
	$(GRADLE) :androidApp:assembleRelease

emulator: ## Open the Android emulator window (AVD=name) unless one is already running
	@test -x $(ADB) || { $(FAIL) "adb not found at $(ADB)." "Install the Android SDK (Android Studio → SDK Manager) or set ANDROID_HOME."; }
	@if [ -z "$$($(ADB) devices | awk 'NR > 1 && $$1 ~ /^emulator-/')" ]; then \
		test -n "$(AVD)" || { $(FAIL) "No Android emulator (AVD) exists." "Create one in Android Studio → Device Manager, or pass AVD=<name>."; }; \
		$(EMULATOR) -list-avds | grep -qx "$(AVD)" || { $(FAIL) "No emulator named $(AVD)." "Available: $$($(EMULATOR) -list-avds | tr '\n' ' ')"; }; \
		echo "Starting emulator $(AVD)…"; \
		nohup $(EMULATOR) -avd "$(AVD)" >/dev/null 2>&1 & \
		until [ -n "$$($(ADB) devices | awk 'NR > 1 && $$1 ~ /^emulator-/ && $$2 == "device"')" ]; do sleep 2; done; \
		until [ "$$($(ADB) -s $$($(ADB) devices | awk 'NR > 1 && $$1 ~ /^emulator-/ { print $$1; exit }') shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 2; done; \
	fi

# Starts the emulator only when nothing at all is connected.
android-device:
	@if [ -z "$$($(ADB) devices | sed 1d | grep -w device)" ]; then $(MAKE) --no-print-directory emulator; fi

android-install: android-device ## Build and install the debug APK
	$(GRADLE) :androidApp:installDebug

android-device-install: ## Install on the Android phones connected by USB or Wi-Fi
	@scripts/android-device.sh

android-run: android-install ## Build, install and open the Android app on every device
	@for serial in $${ANDROID_SERIAL:-$$($(ADB) devices | awk 'NR > 1 && $$2 == "device" { print $$1 }')}; do \
		echo "Opening on $$serial"; \
		$(ADB) -s $$serial shell am start -n $(APP_ID)/$(ACTIVITY) >/dev/null \
			|| { $(FAIL) "Installed on $$serial, but the app did not open." "Unlock the device and open CASEROcu."; }; \
	done

devices: ## List connected Android devices and emulators
	$(ADB) devices -l

# --- iOS ---------------------------------------------------------------------

ios-project: ## Generate the Xcode project from iosApp/project.yml
	@command -v xcodegen >/dev/null || { $(FAIL) "XcodeGen is not installed." "brew install xcodegen"; }
	cd iosApp && xcodegen generate

$(IOS_PROJECT): iosApp/project.yml
	@$(MAKE) --no-print-directory ios-project

ios-build: $(IOS_PROJECT) ## Build the iOS app for the simulator
	@mkdir -p $(IOS_BUILD)
	@echo "Building the iOS app (log: $(IOS_BUILD)/xcodebuild.log)…"
	@xcodebuild -project $(IOS_PROJECT) -scheme CaseroCu -configuration Debug \
		-destination 'generic/platform=iOS Simulator' -derivedDataPath $(IOS_BUILD) \
		CODE_SIGNING_ALLOWED=NO build > $(IOS_BUILD)/xcodebuild.log 2>&1 \
		|| { grep -E "error:|e: |What went wrong" $(IOS_BUILD)/xcodebuild.log | sort -u | head -20; \
			$(FAIL) "The iOS build failed." "The errors are above. Full log: $(IOS_BUILD)/xcodebuild.log"; }

ios-device-install: ## Install on the iPhone connected to this Mac
	@scripts/ios-device.sh

simulator: ## Boot the iOS simulator (SIMULATOR="iPhone 17 Pro Max")
	@xcrun simctl list devices available | grep -q "    $(SIMULATOR) (" || { $(FAIL) "No simulator named \"$(SIMULATOR)\"." "Pick one of: $$(xcrun simctl list devices available | sed -n 's/^    \(iPhone[^(]*\) (.*/\1/p' | sed 's/ *$$//' | sort -u | paste -sd ',' -). Example: make run ios SIMULATOR=\"iPhone 17\""; }
	@xcrun simctl boot "$(SIMULATOR)" 2>/dev/null || true
	@open -a Simulator

ios-run: ios-build simulator ## Build, install and open the iOS app in the simulator
	@xcrun simctl install "$(SIMULATOR)" $(IOS_APP) || { $(FAIL) "Could not install on the simulator." "Wait until it finishes booting and run again."; }
	@xcrun simctl launch "$(SIMULATOR)" $(APP_ID) >/dev/null || { $(FAIL) "Installed, but the app did not open in the simulator." "Open CASEROcu from the simulator's home screen."; }
	@echo "✓ CASEROcu opened in $(SIMULATOR)"
