#!/usr/bin/env -S bash ../.port_include.sh
port=vitetris
useconfigure="true"
version="0.59.1"
files="https://github.com/vicgeralds/vitetris/archive/refs/tags/v${version}.tar.gz vitetris.tar.gz"
configopts="--without-xlib --without-joystick --without-network"

configure() {
    run chmod +x "$configscript"
    run ./"$configscript" $configopts
}
