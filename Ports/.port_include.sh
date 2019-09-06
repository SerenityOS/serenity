#!/bin/sh

# This script contains common helpers for all ports.
set -e

export CC=i686-pc-serenity-gcc
export CXX=i686-pc-serenity-g++

if [ -z "$MAKEOPTS" ]; then
    MAKEOPTS="-j $(nproc)"
fi
if [ -z "$INSTALLOPTS" ]; then
    INSTALLOPTS=""
fi
if [ -z "$SERENITY_ROOT" ]; then
    echo "You must have source'd UseIt.sh to build any ports!"
    exit 1
fi

if [ -z "$PORT_DIR" ]; then
    echo "Must set PORT_DIR to where the source should be cloned."
    exit 1
fi

run_command() {
    echo "+ $@"
    (cd "$PORT_DIR" && "$@")
    echo "+ FINISHED: $@"
}

run_command_nocd() {
    echo "+ $@ (nocd)"
    ("$@")
    echo "+ FINISHED (nocd): $@"
}

run_fetch_git() {
    if [ -d "$PORT_DIR/.git" ]; then
        run_command git fetch
        run_command git reset --hard FETCH_HEAD
        run_command git clean -fx
    else
        run_command_nocd git clone "$1" "$PORT_DIR"
    fi
}

run_fetch_web() {
    if [ -d "$PORT_DIR" ]; then
        run_command_nocd rm -rf "$PORT_DIR"
    fi
    file=$(basename "$1")
    run_command_nocd curl -L "$1" -o "$file"
    mkdir "$PORT_DIR"

    # may need to make strip-components configurable, as I bet some sick person
    # out there has an archive that isn't in a directory :shrug:
    run_command_nocd tar xavf "$file" -C "$PORT_DIR" --strip-components=1
}

run_export_env() {
    export $1="$2"
}

run_replace_in_file() {
    run_command perl -p -i -e "$1" $2
}

run_patch() {
    echo "+ Applying patch $1"
    run_command patch "$2" < "$1"
}

run_configure_cmake() {
    run_command cmake -DCMAKE_TOOLCHAIN_FILE="$SERENITY_ROOT/Toolchain/CMakeToolchain.txt" $CMAKEOPTS .
}

run_configure_autotools() {
    run_command ./configure --host=i686-pc-serenity "$@"
}

run_make() {
    run_command make $MAKEOPTS "$@"
}

run_make_install() {
    run_command make $INSTALLOPTS install "$@"
}

run_send_to_file() {
    echo "+ rewrite '$1'"
	(cd "$PORT_DIR" && echo "$2" > "$1")
    echo "+ FINISHED"
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

if [ "$1" = "fetch" ]; then
    echo "+ Fetching..."
    fetch
elif [ "$1" = "configure" ]; then
    echo "+ Configuring..."
    configure
elif [ "$1" = "build" ]; then
    echo "+ Building..."
    build
elif [ "$1" = "install" ]; then
    echo "+ Installing..."
    install
else
    echo "Unknown verb: $1"
    echo "Supported: (one of) fetch configure build install"
    exit 1
fi
