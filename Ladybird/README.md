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
