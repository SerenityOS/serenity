#!/usr/bin/env -S bash ../.port_include.sh
port='groff'
version='1.22.4'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=('build-aux/config.sub')
files=(
    "https://ftpmirror.gnu.org/gnu/groff/groff-${version}.tar.gz#e78e7b4cb7dec310849004fa88847c44701e8d133b5d4c13057d876c1bad0293"
)
depends=(
    'libiconv'
)
configopts=(
    '--with-doc=no'
)
