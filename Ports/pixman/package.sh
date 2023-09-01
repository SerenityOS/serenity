#!/usr/bin/env -S bash ../.port_include.sh
port='pixman'
version='0.42.2'
useconfigure='true'
files=(
    "https://www.cairographics.org/releases/pixman-${version}.tar.gz#ea1480efada2fd948bc75366f7c349e1c96d3297d09a3fe62626e38e234a625e"
)
use_fresh_config_sub='true'
