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

function run_command() {
    echo "+ $@"
    (cd "$PORT_DIR" && "$@")
    echo "+ FINISHED: $@"
}

function run_command_nocd() {
    echo "+ $@ (nocd)"
    ("$@")
    echo "+ FINISHED (nocd): $@"
}

function run_fetch_git() {
    if [ -d "$PORT_DIR/.git" ]; then
        run_command git fetch
        run_command git reset --hard FETCH_HEAD
    else
        run_command_nocd git clone "$1" "$PORT_DIR"
    fi
}

function run_patch() {
    echo "+ Applying patch $1"
    run_command patch "$2" < "$1"
}

function run_configure_cmake() {
    run_command cmake -DCMAKE_TOOLCHAIN_FILE="$SERENITY_ROOT/Toolchain/CMakeToolchain.txt" .
}

function run_configure_autotools() {
    run_command ./configure --host=i686-pc-serenity "$@"
}

function run_make() {
    run_command make $MAKEOPTS "$@"
}

function run_make_install() {
    run_command make $INSTALLOPTS install "$@"
}

if [ -z "$1" ]; then
    echo "+ Fetching..."
    fetch
    echo "+ Configuring..."
    configure
    echo "+ Building..."
    build
    echo "+ Installing..."
    install
    exit 0
fi

if [ "$1" == "fetch" ]; then
    echo "+ Fetching..."
    fetch
elif [ "$1" == "configure" ]; then
    echo "+ Configuring..."
    configure
elif [ "$1" == "build" ]; then
    echo "+ Building..."
    build
elif [ "$1" == "install" ]; then
    echo "+ Installing..."
    install
else
    echo "Unknown verb: $1"
    echo "Supported: (one of) fetch configure build install"
    exit 1
fi
