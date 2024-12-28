#!/usr/bin/env -S bash ../.port_include.sh
port='findutils'
version='4.10.0'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=('build-aux/config.sub')
files=(
    "https://ftpmirror.gnu.org/gnu/findutils/findutils-${version}.tar.xz#1387e0b67ff247d2abde998f90dfbf70c1491391a59ddfecb8ae698789f0a4f5"
)
