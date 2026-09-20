#!/usr/bin/env -S bash ../.port_include.sh
port='m4'
version='1.4.21'
files=(
    "mirror://gnu/m4/m4-${version}.tar.gz#38ae59f7a30bf9c108193cc5c25fbb06014f21e230c7ede2eff614f7b7c37ed8"
)
useconfigure='true'

# Stack overflow detection needs siginfo and sbrk, neither of which we support
export M4_cv_use_stackovf=no
