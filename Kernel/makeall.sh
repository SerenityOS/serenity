#!/bin/bash
set -e

# Get user and group details for setting qemu disk image ownership
export build_user=$(id -u)
export build_group=$(id -g)

sudo id

if [ -z "$MAKEJOBS" ]; then
    MAKEJOBS=$(nproc)
fi

make_cmd="make -j $MAKEJOBS"

build_targets=""
build_targets="$build_targets ../DevTools/FormCompiler"
build_targets="$build_targets ../Libraries/LibC"
build_targets="$build_targets ../Libraries/LibM"
build_targets="$build_targets ../Libraries/LibCore"
build_targets="$build_targets ../Libraries/LibDraw"
build_targets="$build_targets ../Libraries/LibAudio"
build_targets="$build_targets ../Servers/SystemServer"
build_targets="$build_targets ../Servers/LookupServer"
build_targets="$build_targets ../Servers/WindowServer"
build_targets="$build_targets ../Servers/AudioServer"
build_targets="$build_targets ../Libraries/LibGUI"
build_targets="$build_targets ../Libraries/LibHTML"
build_targets="$build_targets ../Userland"
build_targets="$build_targets ../Applications/Terminal"
build_targets="$build_targets ../Applications/FontEditor"
build_targets="$build_targets ../Applications/Launcher"
build_targets="$build_targets ../Applications/FileManager"
build_targets="$build_targets ../Applications/ProcessManager"
build_targets="$build_targets ../Applications/TextEditor"
build_targets="$build_targets ../Applications/About"
build_targets="$build_targets ../Applications/IRCClient"
build_targets="$build_targets ../Applications/Taskbar"
build_targets="$build_targets ../Applications/Downloader"
build_targets="$build_targets ../Applications/PaintBrush"
build_targets="$build_targets ../Applications/QuickShow"
build_targets="$build_targets ../Applications/Piano"
build_targets="$build_targets ../Applications/SystemDialog"
build_targets="$build_targets ../DevTools/VisualBuilder"
build_targets="$build_targets ../Games/Minesweeper"
build_targets="$build_targets ../Games/Snake"
build_targets="$build_targets ../Shell"
build_targets="$build_targets ../Demos/HelloWorld"
build_targets="$build_targets ../Demos/HelloWorld2"
build_targets="$build_targets ../Demos/RetroFetch"
build_targets="$build_targets ../Demos/WidgetGallery"
build_targets="$build_targets ../Demos/Fire"
build_targets="$build_targets ." # the kernel

for targ in $build_targets; do
    echo "Building $targ"
    #(cd "$targ" && find . -name "*.c" -o -name "*.cpp" -o -name "*.h" -exec clang-format -i {} \;)
    $make_cmd -C "$targ" clean
    $make_cmd -C "$targ"

    if [ -f "$targ/install.sh" ]; then
        echo "Installing $targ"
        (cd "$targ" && ./install.sh)
    fi
done

# has no need to build separately, but install headers.
(cd ../AK && ./install.sh)
(cd ../AK/Tests && $make_cmd clean)
(cd ../AK/Tests && $make_cmd)

sudo -E ./build-image-qemu.sh

