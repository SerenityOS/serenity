#!/usr/bin/env -S bash ../.port_include.sh
port=oksh
useconfigure=true
version=7.0
depends=("ncurses")
workdir=oksh-${version}
files="https://github.com/ibara/oksh/releases/download/oksh-${version}/oksh-${version}.tar.gz oksh-${version}.tar.gz 21d5891f38ffea3a5d1aa8c494f0a5579c93778535e0a92275b102dec3221da1"
auth_type=sha256

export CPPFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include/ncurses"

configure() {
    export CFLAGS="" 
    export LDFLAGS="-lncurses"
    run ./configure --no-thanks
}

install() {
    run cp oksh "${SERENITY_INSTALL_ROOT}/bin"
}
