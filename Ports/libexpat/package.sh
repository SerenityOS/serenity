#!/usr/bin/env -S bash ../.port_include.sh
port='libexpat'
version='2.6.4'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=(
    'conftools/config.sub'
)
files=(
    "https://github.com/libexpat/libexpat/releases/download/R_${version//./_}/expat-${version}.tar.xz#a695629dae047055b37d50a0ff4776d1d45d0a4c842cf4ccee158441f55ff7ee"
)
workdir="expat-${version}"
