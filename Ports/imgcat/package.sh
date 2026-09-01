#!/usr/bin/env -S bash ../.port_include.sh
port='imgcat'
version='2.6.0'
useconfigure='true'
installopts=("PREFIX=${SERENITY_INSTALL_ROOT}/usr/local")
depends=(
    'ncurses'
    'libpng'
    'libjpeg'
)
files=(
    "https://github.com/eddieantonio/imgcat/releases/download/v${version}/imgcat-${version}.tar.gz#1e7e69670ad73e36ba1a9f0a09b6a787cf4e141dfe7885ae7ad77c293fb999a6"
)
