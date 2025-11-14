#!/usr/bin/env -S bash ../.port_include.sh
port='m4'
version='1.4.20'
files=(
    "https://ftpmirror.gnu.org/gnu/m4/m4-${version}.tar.gz#6ac4fc31ce440debe63987c2ebbf9d7b6634e67a7c3279257dc7361de8bdb3ef"
)
useconfigure='true'

# Stack overflow detection needs siginfo and sbrk, neither of which we support
export M4_cv_use_stackovf=no
