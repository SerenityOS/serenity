#!/usr/bin/env -S bash ../.port_include.sh

port=SDL2-GNUBoy
version=1.2.1
useconfigure=false
files="https://github.com/AlexOberhofer/SDL2-GNUBoy/archive/refs/tags/v${version}.tar.gz SDL2-GNUBoy-${version}.tar.gz d8b729aa88747301ed39514ad9dc857b842332ac87242993881e15125af1be20"
auth_type=sha256
depends=("SDL2")
