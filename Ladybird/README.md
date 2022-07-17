# Ladybird Web Browser

The Ladybird Web Browser is a browser using the SerenityOS LibWeb engine with a Qt GUI.

## Build Prerequisites

Qt6 development packages and a c++20-enabled compiler are required. On Debian/Ubuntu required packages include, but are not limited to:

```
sudo apt install build-essential cmake libgl1-mesa-dev ninja-build qt6-base-dev qt6-tools-dev-tools
```

On Arch Linux/Manjaro:

```
sudo pacman -S base-devel cmake libgl ninja qt6-base qt6-tools qt6-wayland
```

For the c++ compiler, gcc-11 or clang-13 are required at a minimum for c++20 support.

For Ubuntu 20.04 and above, ensure that the Qt6 Wayland packages are available:

```
sudo apt install qt6-wayland
```


## Build steps

Basic workflow, using serenity source dir cloned from github:

```
cmake -GNinja -B Build
cmake --build Build
ninja -C Build run
```

Advanced workflow, using pre-existing serenity checkout.

If you previously didn't set SERENITY_SOURCE_DIR, probably want to blast the Build directory before doing this:

```
cmake -GNinja -B Build -DSERENITY_SOURCE_DIR=/path/to/serenity
ninja -C Build run
```

To automatically run in gdb:
```
ninja -C Build debug
```

To run without ninja rule:
```
# or your existing serenity checkout /path/to/serenity
export SERENITY_SOURCE_DIR=${PWD}/Build/serenity
./Build/ladybird
```

## Experimental Android Build Steps

### Prepping Qt Creator

In order to build an Android APK, the following additional dependencies are required/recommended:

* Qt Creator 6.4.0 (dev branch)
* Android Studio 2021.2 (dev branch)

Note that Qt Creator 6.3.x LTS does NOT have the required fix to [QTBUG-104580](https://bugreports.qt.io/browse/QTBUG-104580) as of 2022-07-16 in order to use NDK 24.

The build configuration was tested with the following pacakges from the Android SDK:

* Android Platform and Build Tools version 33
* Android System Images for API 33 aka ``"system-images;android-33;google-apis;x86_64"``
* Android NDK 24.0.8215888 for the llvm-14 based toolchain

In order to build ladybird for cross compilation, a separate serenity checkout is recommended.

e.g.
```
cd ~/Repos
git clone https://github.com/SerenityOS/serenity
```

First create a LagomTools build:

```
cmake -GNinja -S /path/to/serenity -B BuildTools -Dpackage=LagomTools -DCMAKE_INSTALL_PREFIX=tool-install
ninja -C BuildTools install
```

Next, create a build configuration in Qt Creator that uses an ``Android Qt 6.4.0 Debug x86_64`` Kit by following the instructions [here](https://doc.qt.io/qt-6/android-getting-started.html).

Ensure that you get Android API 30 or higher, and Android NDK 24 or higher. In the initial standup, an API 33 SDK for Android 13 was used.

Setup Android device settings in Qt Creator following this [link](https://doc.qt.io/qtcreator/creator-developing-android.html). Note that Qt Creator might not like the Android NDK version 24 we downloaded earlier, as it's "too new" and "not supported". No worries, we can force it to like our version by editing the ``sdk_defintions.json`` file as described uner [Viewing Android Tool Chain Settings](https://doc.qt.io/qtcreator/creator-developing-android.html#viewing-android-tool-chain-settings)

The relevant snippets of that JSON file are reproduced below. Just have to make sure it's happy with "platforms;android-33" and the exact installed NDK version.

```json
        "sdk_essential_packages": {
            "default": ["platform-tools", "platforms;android-33", "cmdline-tools;latest"],
            "linux": [],
            "mac": [],
            "windows": ["extras;google;usb_driver"]
        }
    },
    "specific_qt_versions": [
        {
            "versions": ["default"],
            "sdk_essential_packages": ["build-tools;33.0.0", "ndk;24.0.8215888"],
            "ndk_path": "ndk/24.0.8215888"
        },
```


### Building Ladybird for Android

Next, we can select the ``Android Qt 6.4.0 Debug x86_64`` Kit under the Projects tab of the Qt Creator, and watch CMake have a bad time because we need to edit the configuration.

In the ``Initial Configuration`` Tab of the CMake configuration for the Kit, edit the following initial values:

* ANDROID_NATIVE_API_LEVEL: 23 --> 30
* LagomTools_DIR: New Directory setting, set to `/path/to/ladybird/tool-install/share/Lagom` for the LagomTools build we created earlier
* SERENITY_SOURCE_DIR: New path setting, set to your local serenity checkout

Make sure to click the ``Reconfigure With Initial Parameters`` button, and triple check you've been editing the ``Initial Configuration`` tab and not the ``Current Configuration`` one.

Build the project, and cross your fingers that it all works :)

### Running the Android APK

In order to run the ladybird application, first make sure that the Debug settings in the bottom left of the Qt Creator window are trying to debug ladybird, and not another Lagom target, like LibArchive.

Create an Android device to test using the Tools->Options->Devices->Devices add button. This will only work for an Android device if the ``"system-images;android-33;google-apis;x86_64"`` or similar package is installed with the Android SDK ``sdkmanager`` tool.

Open up Android Studio, and in the Device Manager edit the created AVD to update its Internal Storage under Advanced Settings. Make sure it's at least 1 GiB. The default of 800 MiB is generally too small to install ladybird.

Hit the Debug or Run green arrows and hope for the best!

With luck the application should start up, install the required resources into the internal storage from the APK, and open up the default webpage. Clicking the home button to load serenityos.org should work.
