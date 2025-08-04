#!/usr/bin/env -S bash ../.port_include.sh
port='x264'
version='b35605ace3ddf7c1a5d67a2eb553f034aef41d55'
files=(
    "https://code.videolan.org/videolan/x264/-/archive/${version}/x264-master.tar.gz#cd71a7515b0e9a012e1ac9b1f8415bebcaf6fc97d4db32286642ac4c0fbe24f9"
)
useconfigure='true'
use_fresh_config_sub='true'
configopts=(
    '--enable-shared'
)
