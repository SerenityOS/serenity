#!/usr/bin/env -S bash ../.port_include.sh
port='libexpat'
version='2.5.0'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=(
    'conftools/config.sub'
)
files=(
    "https://github.com/libexpat/libexpat/releases/download/R_${version//./_}/expat-${version}.tar.xz#ef2420f0232c087801abf705e89ae65f6257df6b7931d37846a193ef2e8cdcbe"
)
workdir="expat-${version}"
