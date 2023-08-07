#!/usr/bin/env -S bash ../.port_include.sh

port='x264'
version='baee400fa9ced6f5481a728138fed6e867b0ff7f'
files=(
    "https://code.videolan.org/videolan/x264/-/archive/${version}/x264-master.tar.gz 436a2be54d8bc0cb05dd33ecbbcb7df9c3b57362714fcdaa3a5991189a33319b"
)
useconfigure='true'
use_fresh_config_sub='true'
configopts=("--enable-shared")
