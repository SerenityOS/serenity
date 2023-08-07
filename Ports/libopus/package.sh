#!/usr/bin/env -S bash ../.port_include.sh
port='libopus'
version='1.3.1'
workdir='opus-1.3.1'
useconfigure='true'
use_fresh_config_sub='true'
files=(
    "https://archive.mozilla.org/pub/opus/opus-${version}.tar.gz 65b58e1e25b2a114157014736a3d9dfeaad8d41be1c8179866f144a2fb44ff9d"
)
