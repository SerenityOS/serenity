#!/usr/bin/env -S bash ../.port_include.sh

port=SDL2-GNUBoy
version=1.2
useconfigure=false
files="https://github.com/AlexOberhofer/SDL2-GNUBoy/archive/refs/tags/v${version}.tar.gz SDL2-GNUBoy-${version}.tar.gz 7d00a80e4b6bbb4c388b1ea0a34daca5f90fba574f09915c5135431f81091c8a"
auth_type=sha256
depends=("SDL2")
