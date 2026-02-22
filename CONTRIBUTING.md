# Contributing

## Prerequisites
- Android Studio with Wear OS support
- JDK 17 or newer

## Setting up Emulator

Check available and installed system images:

```
sdkmanager --list
sdkmanager --list_installed
```

Install Wear OS system image if not already installed, for example on MacOS ARM architecture:

```
sdkmanager "system-images;android-33;android-wear;arm64-v8a"
```

List and create the virtual device

````
avdmanager list avd
avdmanager create avd -n wear_emulator -k "system-images;android-33;android-wear;arm64-v8a" -d "wearos_large_round"
````


Start the emulator:

```
emulator -avd wear_emulator
```

## Connecting a Physical Device

Run on a physical Wear OS device via ADB over Wi-Fi:

Pairing (first-time only):

```
adb kill-server
adb start-server
adb mdns services
read -p "Code: " c && adb pair $(adb mdns services | grep "_adb-tls-pairing" | awk '{print $3}') $c
```

Connecting / Reconnecting:

```
adb connect $(adb mdns services | grep "_adb-tls-connect" | awk '{print $3}')
```

## Building and Installing

Compile and install the app:

```
./gradlew installDebug
adb shell am start -n fi.protonode.ElectricitySpotPrice/.MainActivity
```
