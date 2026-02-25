# Workflow for Agents to Work on the Project

## Project Overview

**Spot-Pulse** is a standalone Wear OS application for tracking electricity spot prices in Finland.

### Tech Stack
- **UI:** Jetpack Compose for Wear OS
- **Networking:** Retrofit with GSON
- **Dependency Injection:** Hilt (Dagger)
- **Persistence:** Jetpack DataStore (Preferences)
- **Features:** Wear OS Tiles, Repository Pattern

### Directory Structure
- `app/src/main/java/fi/protonode/ElectricitySpotPrice/`:
  - `ui/`: ViewModels and Compose screens.
  - `repo/`: Data repository handling caching logic.
  - `network/`: API definitions and models.
  - `storage/`: Local data persistence.
  - `tiles/`: Wear OS Tile services.
  - `di/`: Hilt modules.

## Editing the Code

Use following workflow to edit the code:

1. Make your changes in the code files.
2. **IMPORTANT**: Always run a build AND check for errors after making changes.
   ```bash
   ./gradlew clean build
   ```
3. If there are no errors, then install on the emulator or device:
   ```bash
   ./gradlew installDebug
   ```
4. Launch the app on the device or emulator:
   ```bash
   adb shell am start -n fi.protonode.ElectricitySpotPrice/.MainActivity
   ```

## Development & Debugging

### Logging
Monitor app logs specifically for price-related information:
```bash
adb logcat | grep -i price
```

### Launching Directly

To explicitly launch the main application directly via ADB:
```bash
adb shell am start -n fi.protonode.ElectricitySpotPrice/fi.protonode.ElectricitySpotPrice.MainActivity
```

### Wear OS Tile
To explicitly launch and view the tile for debugging without manually configuring it or swiping through the UI:

1. Add the tile using the debug surface (this will output the tile index, e.g. `Index=[0]`):
```bash
adb shell am broadcast -a com.google.android.wearable.app.DEBUG_SURFACE --es operation add-tile --ecn component fi.protonode.ElectricitySpotPrice/fi.protonode.ElectricitySpotPrice.tiles.CurrentPriceTileService
```

2. Show the tile directly using its index (replace `0` with the index from the previous command):
```bash
adb shell am broadcast -a com.google.android.wearable.app.DEBUG_SYSUI --es operation show-tile --ei index 0
```

### Icons
If you modify `app_icon.svg`, update the launcher icons using the provided script:
```bash
python3 generate_icons_online.py
```
*(Requires `librsvg` for local conversion, or it will attempt to use an online service.)*

### Testing
You can run the unit tests directly on the host machine:
```bash
./gradlew testDebugUnitTest
```

For UI tests, an emulator or a physical device must be connected via ADB.

You can start the emulator with:
```bash
emulator -avd wear_emulator &
```

Once the emulator is running, run the following command:
```bash
./gradlew connectedDebugAndroidTest
```

## Using the Android Emulator with MCP

To configure MCP server add following to `~/.gemini/antigravity/mcp_config.json`:

```json
{
    "mcpServers": {
        "android-emulator": {
            "command": "npx",
            "args": [
                "-y",
                "mcp-android-emulator"
            ]
        }
    }
}
```

When running the app on an Android emulator via the MCP server, follow these precise steps to avoid common issues:

1. List available AVDs: `emulator -list-avds`
2. Launch the emulator in the background: `emulator -avd wear_emulator`
3. Wait for the device to boot fully: `adb wait-for-device shell 'while [[ -z $(getprop sys.boot_completed) ]]; do sleep 1; done;'`
4. Build the application APK: `./gradlew assembleDebug`
5. Note: The APK will be located at `app/build/outputs/apk/debug/app-debug.apk`
6. Install via the MCP Server tool: **`mcp_android-emulator_install_apk`** with the absolute path to the built APK.
7. Launch the app using ADB (Do NOT use the MCP tool `mcp_android-emulator_launch_app`, because Wear OS handles the launcher intents differently):
   ```bash
   adb shell am start -n fi.protonode.ElectricitySpotPrice/.MainActivity
   ```
8. Verify the UI is correctly displaying by running **`mcp_android-emulator_get_all_text`** or **`mcp_android-emulator_screenshot`**.

## Troubleshooting

- **Connection Issues:** If the build fails because there is no connection to the device/emulator, ask the user to (re)connect the device or start the emulator.
- **Build Failures:** Read the build error messages carefully. Common issues include missing Hilt annotations or Kapt processing errors.
