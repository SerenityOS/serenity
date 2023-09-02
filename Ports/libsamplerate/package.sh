#!/usr/bin/env -S bash ../.port_include.sh
port='libsamplerate'
version='0.2.2'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=('build-aux/config.sub')
configopts=(
    '--disable-static'
    '--enable-shared'
)
files=(
    "https://github.com/libsndfile/libsamplerate/releases/download/${version}/libsamplerate-${version}.tar.xz#3258da280511d24b49d6b08615bbe824d0cacc9842b0e4caf11c52cf2b043893"
)
