#!/usr/bin/env -S bash ../.port_include.sh
port='cpio'
version='2.14'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=('build-aux/config.sub')
files=(
    "https://ftpmirror.gnu.org/gnu/cpio/cpio-${version}.tar.gz#145a340fd9d55f0b84779a44a12d5f79d77c99663967f8cfa168d7905ca52454"
)
