#!/usr/bin/env -S bash ../.port_include.sh
port='findutils'
version='4.9.0'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=('build-aux/config.sub')
files=(
    "https://ftpmirror.gnu.org/gnu/findutils/findutils-${version}.tar.xz a2bfb8c09d436770edc59f50fa483e785b161a3b7b9d547573cb08065fd462fe"
)
