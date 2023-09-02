#!/usr/bin/env -S bash ../.port_include.sh
port='mednafen'
version='1.31.0-UNSTABLE'
files=(
    "https://mednafen.github.io/releases/files/mednafen-${version}.tar.xz#bfcff72e370e09e12ba3791600782187fbf5e2cc9d6b5fe4f9f3471642046367"
)
workdir="mednafen"
useconfigure='true'
use_fresh_config_sub='true'
use_fresh_config_guess='true'
depends=("SDL2" "zlib" "flac")
