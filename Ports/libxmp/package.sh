#!/usr/bin/env -S bash ../.port_include.sh
port='libxmp'
version='4.6.0'
files=(
    "https://github.com/libxmp/libxmp/releases/download/libxmp-${version}/libxmp-${version}.tar.gz#2d3c45fe523b50907e89e60f9a3b7f4cc9aab83ec9dbba7743eaffbcdcb35ea6"
)
useconfigure='true'
use_fresh_config_sub='false'
