#!/usr/bin/env -S bash ../.port_include.sh
port='diffutils'
version='3.10'
depends=(
    'libiconv'
)
files=(
    "https://ftpmirror.gnu.org/gnu/diffutils/diffutils-${version}.tar.xz#90e5e93cc724e4ebe12ede80df1634063c7a855692685919bfe60b556c9bd09e"
)
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=(
    'build-aux/config.sub'
)
