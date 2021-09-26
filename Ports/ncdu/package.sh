#!/usr/bin/env -S bash ../.port_include.sh
port=ncdu
useconfigure="true"
version="1.16"
files="https://dev.yorhel.nl/download/ncdu-${version}.tar.gz ncdu-${version}.tar.gz 2b915752a183fae014b5e5b1f0a135b4b408de7488c716e325217c2513980fd4"
auth_type=sha256
depends=("ncurses")

export CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include/ncurses"
