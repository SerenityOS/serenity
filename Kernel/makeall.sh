#!/bin/sh
if [ -z "$SERENITY_ROOT" ]; then
    echo "You must source Toolchain/UseIt.sh to build Serenity."
    exit 1
fi

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

# Build the host-side tools first, since they are needed to build some programs.
build_targets="$build_targets ../DevTools/IPCCompiler"
build_targets="$build_targets ../DevTools/FormCompiler"
build_targets="$build_targets ../Libraries/LibHTML/CodeGenerators/Generate_CSS_PropertyID_cpp"
build_targets="$build_targets ../Libraries/LibHTML/CodeGenerators/Generate_CSS_PropertyID_h"

# Build LibC, LibCore, LibIPC and LibThread before IPC servers, since they depend on them.
build_targets="$build_targets ../Libraries/LibC"
build_targets="$build_targets ../Libraries/LibCore"
build_targets="$build_targets ../Libraries/LibDraw"
build_targets="$build_targets ../Libraries/LibIPC"
build_targets="$build_targets ../Libraries/LibThread"
build_targets="$build_targets ../Libraries/LibPthread"

# Build IPC servers before their client code to ensure the IPC definitions are available.
build_targets="$build_targets ../Servers/AudioServer"
build_targets="$build_targets ../Servers/LookupServer"
build_targets="$build_targets ../Servers/ProtocolServer"
build_targets="$build_targets ../Libraries/LibAudio"
build_targets="$build_targets ../Servers/WindowServer"

build_targets="$build_targets ../AK"

build_targets="$build_targets ../Libraries/LibGUI"
build_targets="$build_targets ../Libraries/LibHTML"
build_targets="$build_targets ../Libraries/LibM"
build_targets="$build_targets ../Libraries/LibPCIDB"
build_targets="$build_targets ../Libraries/LibVT"
build_targets="$build_targets ../Libraries/LibMarkdown"
build_targets="$build_targets ../Libraries/LibProtocol"

build_targets="$build_targets ../Applications/About"
build_targets="$build_targets ../Applications/Calculator"
build_targets="$build_targets ../Applications/ChanViewer"
build_targets="$build_targets ../Applications/DisplayProperties"
build_targets="$build_targets ../Applications/FileManager"
build_targets="$build_targets ../Applications/FontEditor"
build_targets="$build_targets ../Applications/IRCClient"
build_targets="$build_targets ../Applications/PaintBrush"
build_targets="$build_targets ../Applications/Piano"
build_targets="$build_targets ../Applications/QuickShow"
build_targets="$build_targets ../Applications/SystemDialog"
build_targets="$build_targets ../Applications/SystemMonitor"
build_targets="$build_targets ../Applications/Taskbar"
build_targets="$build_targets ../Applications/Terminal"
build_targets="$build_targets ../Applications/TextEditor"
build_targets="$build_targets ../Applications/HexEditor"
build_targets="$build_targets ../Applications/SoundPlayer"
build_targets="$build_targets ../Applications/Welcome"
build_targets="$build_targets ../Applications/Help"
build_targets="$build_targets ../Applications/Browser"

build_targets="$build_targets ../Demos/Fire"
build_targets="$build_targets ../Demos/HelloWorld"
build_targets="$build_targets ../Demos/HelloWorld2"
build_targets="$build_targets ../Demos/WidgetGallery"

build_targets="$build_targets ../DevTools/HackStudio"
build_targets="$build_targets ../DevTools/VisualBuilder"
build_targets="$build_targets ../DevTools/Inspector"

build_targets="$build_targets ../Games/Minesweeper"
build_targets="$build_targets ../Games/Snake"

build_targets="$build_targets ../Servers/SystemServer"
build_targets="$build_targets ../Servers/TTYServer"
build_targets="$build_targets ../Servers/TelnetServer"

build_targets="$build_targets ../Shell"

build_targets="$build_targets ../Userland"

build_targets="$build_targets ../MenuApplets/CPUGraph"

build_targets="$build_targets ." # the kernel

(cd ../AK/Tests && $make_cmd clean)
(cd ../AK/Tests && $make_cmd clean && $make_cmd)
(cd ../AK/Tests && $make_cmd clean)

for targ in $build_targets; do
    #(cd "$targ" && find . -name "*.c" -o -name "*.cpp" -o -name "*.h" -exec clang-format -i {} \;)

    if [ -f "$targ/Makefile" ]; then
        echo "Building $targ"
        $make_cmd -C "$targ" clean
        $make_cmd -C "$targ"
    fi

    if [ -f "$targ/install.sh" ]; then
        echo "Installing $targ"
        (cd "$targ" && ./install.sh)
    fi
done

sudo -E PATH="$PATH" ./build-image-qemu.sh
