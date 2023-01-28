#!/usr/bin/env -S bash ../.port_include.sh
port=termcap
description='GNU termcap'
version=1.3.1
website='https://www.gnu.org/software/termutils/'
useconfigure=true
configopts=("--prefix=${SERENITY_INSTALL_ROOT}/usr/local")
files="https://ftpmirror.gnu.org/gnu/termcap/termcap-${version}.tar.gz termcap-${version}.tar.gz 91a0e22e5387ca4467b5bcb18edf1c51b930262fd466d5fda396dd9d26719100"
auth_type=sha256
