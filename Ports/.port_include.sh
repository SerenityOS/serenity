#!/bin/sh

# This script contains common helpers for all ports.
set -e

if [ -z "$MAKEOPTS" ]; then
    MAKEOPTS="-j $(nproc)"
fi
if [ -z "$INSTALLOPTS" ]; then
    MAKEOPTS="-j $(nproc)"
fi
if [ -z "$SERENITY_ROOT" ]; then
    echo "You must have source'd UseIt.sh to build any ports!"
    exit 1
fi

if [ -z "$PORT_DIR" ]; then
    echo "Must set PORT_DIR to where the source should be cloned."
    exit 1
fi

function run_fetch_git() {
    if [ -d "$PORT_DIR/.git" ]; then
        (cd "$PORT_DIR" && git fetch && git reset --hard FETCH_HEAD)
    else
        git clone "$1" "$PORT_DIR"
    fi
}

function run_configure_cmake() {
    (
        cd "$PORT_DIR"
        cmake -DCMAKE_TOOLCHAIN_FILE="$SERENITY_ROOT/Toolchain/CMakeToolchain.txt" .
    )
}

function run_configure_autotools() {
    (
        cd "$PORT_DIR"
        ./configure --host=i686-pc-serenity
    )
}

function run_make() {
    (
        cd "$PORT_DIR"
        make $MAKEOPTS "$@"
    )
}

function run_make_install() {
    (
        cd "$PORT_DIR"
        make $INSTALLOPTS install "$@"
    )
}

if [ -z "$1" ]; then
    fetch
    configure
    build
    install
    exit 0
fi

if [ "$1" == "fetch" ]; then
    fetch
elif [ "$1" == "configure" ]; then
    configure
elif [ "$1" == "build" ]; then
    build
elif [ "$1" == "install" ]; then
    install
else
    echo "Unknown verb: $1"
    echo "Supported: (one of) fetch configure build install"
    exit 1
fi
