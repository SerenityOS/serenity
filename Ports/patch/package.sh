#!/usr/bin/env -S bash ../.port_include.sh
port=patch
version=2.7.6
useconfigure=true
use_fresh_config_sub=true
config_sub_paths=("build-aux/config.sub")
files=(
    "https://ftpmirror.gnu.org/gnu/patch/patch-${version}.tar.gz#8cf86e00ad3aaa6d26aca30640e86b0e3e1f395ed99f189b06d4c9f74bc58a4e"
)
