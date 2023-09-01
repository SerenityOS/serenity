#!/usr/bin/env -S bash ../.port_include.sh
port='x264'
version='a8b68ebfaa68621b5ac8907610d3335971839d52'
files=(
    "https://code.videolan.org/videolan/x264/-/archive/${version}/x264-master.tar.gz#164688b63f11a6e4f6d945057fc5c57d5eefb97973d0029fb0303744e10839ff"
)
useconfigure='true'
use_fresh_config_sub='true'
configopts=(
    '--enable-shared'
)
