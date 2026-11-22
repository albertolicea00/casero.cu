# CASERO — common tasks. Run `make` for the list.
#
#   make run                  Android app on a device/emulator (default)
#   make run PLATFORM=ios     iOS app on a simulator
#   make test                 shared tests on Android (JVM) and the iOS simulator

PLATFORM ?= android
APP_ID := com.casero.cu
ACTIVITY := com.casero.cu.android.MainActivity
GRADLE := ./gradlew
ANDROID_HOME ?= $(HOME)/Library/Android/sdk
ADB := $(ANDROID_HOME)/platform-tools/adb
EMULATOR := $(ANDROID_HOME)/emulator/emulator
# First installed AVD unless one is given: make run AVD=Pixel_8
AVD ?= $(shell $(EMULATOR) -list-avds 2>/dev/null | head -1)
# Simulator name as listed by `xcrun simctl list devices available`
SIMULATOR ?= iPhone 17 Pro Max
IOS_PROJECT := iosApp/CaseroCu.xcodeproj
IOS_BUILD := build/ios
IOS_APP := $(IOS_BUILD)/Build/Products/Debug-iphonesimulator/CaseroCu.app

.DEFAULT_GOAL := help
.PHONY: help run build test check clean \
	android-build android-install android-run android-release emulator \
	ios-project ios-build ios-run simulator

help: ## List the available tasks
	@awk 'BEGIN {FS = ":.*## "} /^[a-zA-Z_-]+:.*## / {printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

run: ## Run the app (PLATFORM=android|ios)
	@$(MAKE) --no-print-directory $(PLATFORM)-run

build: android-build ios-build ## Build both apps (debug)

test: ## Run the shared tests on Android (JVM) and the iOS simulator
	$(GRADLE) :shared:testDebugUnitTest :shared:iosSimulatorArm64Test

check: ## Tests, Android lint and whitespace check
	$(GRADLE) :shared:testDebugUnitTest :shared:iosSimulatorArm64Test :androidApp:lintDebug
	git diff --check

clean: ## Remove build outputs and the generated Xcode project
	$(GRADLE) clean
	rm -rf $(IOS_BUILD) $(IOS_PROJECT)

# --- Android -----------------------------------------------------------------

android-build: ## Build the Android debug APK
	$(GRADLE) :androidApp:assembleDebug

android-release: ## Build the Android release bundle (needs your signing config)
	$(GRADLE) :androidApp:bundleRelease

emulator: ## Start the Android emulator (AVD=name) unless a device is connected
	@if [ -z "$$($(ADB) devices | sed 1d | grep -w device)" ]; then \
		test -n "$(AVD)" || { echo "No AVD found. Create one in Android Studio or pass AVD=name."; exit 1; }; \
		echo "Starting emulator $(AVD)…"; \
		nohup $(EMULATOR) -avd "$(AVD)" >/dev/null 2>&1 & \
		$(ADB) wait-for-device; \
		until [ "$$($(ADB) shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 2; done; \
	fi

android-install: emulator ## Build and install the debug APK
	$(GRADLE) :androidApp:installDebug

android-run: android-install ## Build, install and open the Android app
	$(ADB) shell am start -n $(APP_ID)/$(ACTIVITY)

# --- iOS ---------------------------------------------------------------------

ios-project: ## Generate the Xcode project from iosApp/project.yml
	cd iosApp && xcodegen generate

$(IOS_PROJECT): iosApp/project.yml
	cd iosApp && xcodegen generate

ios-build: $(IOS_PROJECT) ## Build the iOS app for the simulator
	xcodebuild -project $(IOS_PROJECT) -scheme CaseroCu -configuration Debug \
		-destination 'generic/platform=iOS Simulator' -derivedDataPath $(IOS_BUILD) \
		CODE_SIGNING_ALLOWED=NO build

simulator: ## Boot the iOS simulator (SIMULATOR="iPhone 17 Pro Max")
	@xcrun simctl boot "$(SIMULATOR)" 2>/dev/null || true
	@open -a Simulator

ios-run: ios-build simulator ## Build, install and open the iOS app in the simulator
	xcrun simctl install "$(SIMULATOR)" $(IOS_APP)
	xcrun simctl launch "$(SIMULATOR)" $(APP_ID)
