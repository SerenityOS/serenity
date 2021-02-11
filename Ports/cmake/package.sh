#!/usr/bin/env -S bash ../.port_include.sh
port=cmake
version=3.19.4
useconfigure=false
files="https://github.com/Kitware/CMake/releases/download/v$version/cmake-$version.tar.gz cmake-$version.tar.gz"
depends="bash gcc make sed"

port_path=$(realpath $(dirname ${BASH_SOURCE[0]}))

build() {
    return
}

install() {
    cmake_dir="${SERENITY_ROOT}/Build/Root/home/anon/Source/cmake"
    run rm -rf "$cmake_dir"
    run mkdir -p "$cmake_dir"
    run cp -r . "$cmake_dir"
}

post_install() {
    echo +===================================================================
    echo "| Successfully prepared the sources for cmake v$version!"
    echo "| The other half of the install has to be done inside serenity"
    echo "| to continue, re-image and run the vm, then do the following:"
    echo '| '
    echo '| $ cd Source/cmake'
    echo '| $ ./bootstrap'
    echo '| $ make && make install'
    echo '| '
    echo "| (These instructions are also available in $port_path/howto.md)"
    echo +===================================================================
}
