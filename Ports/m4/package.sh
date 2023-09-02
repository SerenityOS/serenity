#!/usr/bin/env -S bash ../.port_include.sh
port='m4'
version='1.4.19'
files=(
    "https://ftpmirror.gnu.org/gnu/m4/m4-${version}.tar.gz#3be4a26d825ffdfda52a56fc43246456989a3630093cced3fbddf4771ee58a70"
)
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=("build-aux/config.sub")

# Stack overflow detection needs siginfo and sbrk, neither of which we support
export M4_cv_use_stackovf=no
