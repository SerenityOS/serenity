#!/bin/bash
set -e

sudo id

if [ -z "$MAKEJOBS" ]; then
    MAKEJOBS=$(nproc)
fi

make_cmd="make -j $MAKEJOBS"

build_targets=""
build_targets="$build_targets ../LibC"
build_targets="$build_targets ../LibM"
build_targets="$build_targets ../LibCore"
build_targets="$build_targets ../Servers/LookupServer"
build_targets="$build_targets ../Servers/WindowServer"
build_targets="$build_targets ../LibGUI"
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
build_targets="$build_targets ../DevTools/VisualBuilder"
build_targets="$build_targets ../Games/Minesweeper"
build_targets="$build_targets ../Games/Snake"
build_targets="$build_targets ../Shell"
build_targets="$build_targets ../Demos/HelloWorld"
build_targets="$build_targets ../Demos/RetroFetch"
build_targets="$build_targets ." # the kernel

for targ in $build_targets; do
    echo "Building $targ"
    $make_cmd -C "$targ" clean
    $make_cmd -C "$targ"

    if [ -f "$targ/install.sh" ]; then
        echo "Installing $targ"
        (cd "$targ" && ./install.sh)
    fi
done

# has no need to build separately, but install headers.
(cd ../SharedGraphics && ./install.sh)
(cd ../AK && ./install.sh)

sudo ./sync.sh

