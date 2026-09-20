#!/usr/bin/env -S bash ../.port_include.sh
port='mednafen'
version='1.32.1'
files=(
    "https://mednafen.github.io/releases/files/mednafen-${version}.tar.xz#de7eb94ab66212ae7758376524368a8ab208234b33796625ca630547dbc83832"
)
workdir="mednafen"
useconfigure='true'
use_fresh_config_sub='true'
use_fresh_config_guess='true'
depends=("SDL2" "zlib" "flac")
