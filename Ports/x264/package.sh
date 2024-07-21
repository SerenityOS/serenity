#!/usr/bin/env -S bash ../.port_include.sh
port='x264'
version='4613ac3c15fd75cebc4b9f65b7fb95e70a3acce1'
files=(
    "https://code.videolan.org/videolan/x264/-/archive/${version}/x264-master.tar.gz#44ce79258656d7dbe06165321cb989cc242a66effa308e25d2f0a197b50f8398"
)
useconfigure='true'
use_fresh_config_sub='true'
configopts=(
    '--enable-shared'
)
