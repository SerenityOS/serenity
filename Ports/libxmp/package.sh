#!/usr/bin/env -S bash ../.port_include.sh
port='libxmp'
version='4.6.1'
files=(
    "https://github.com/libxmp/libxmp/releases/download/libxmp-${version}/libxmp-${version}.tar.gz#af605e72c83b24abaf03269347e24ebc3fc06cd7b495652a2c619c1f514bc5cb"
)
useconfigure='true'
use_fresh_config_sub='false'
