#!/usr/bin/env -S bash ../.port_include.sh
port='pixman'
version='0.40.0'
useconfigure='true'
files=(
    "https://www.cairographics.org/releases/pixman-${version}.tar.gz#6d200dec3740d9ec4ec8d1180e25779c00bc749f94278c8b9021f5534db223fc"
)
use_fresh_config_sub='true'
