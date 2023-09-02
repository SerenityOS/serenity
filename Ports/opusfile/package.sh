#!/usr/bin/env -S bash ../.port_include.sh
port='opusfile'
version='0.12'
useconfigure='true'
use_fresh_config_sub='true'
files=(
    "https://downloads.xiph.org/releases/opus/opusfile-${version}.tar.gz#118d8601c12dd6a44f52423e68ca9083cc9f2bfe72da7a8c1acb22a80ae3550b"
)
depends=(
    'libogg'
    'libopus'
)
