#!/usr/bin/env -S bash ../.port_include.sh
port=libmpeg2
version=0.5.1
useconfigure=true
use_fresh_config_sub=true
config_sub_paths=(".auto/config.sub")
configopts=("--disable-sdl")
files=(
    "https://libmpeg2.sourceforge.io/files/libmpeg2-${version}.tar.gz#dee22e893cb5fc2b2b6ebd60b88478ab8556cb3b93f9a0d7ce8f3b61851871d4"
)
