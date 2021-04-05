#!/usr/bin/env -S bash ../.port_include.sh
port=oksh
useconfigure=true
version=6.8.1
depends="ncurses"
workdir=oksh-${version}
files="https://github.com/ibara/oksh/releases/download/oksh-${version}/oksh-${version}.tar.gz oksh-${version}.tar.gz"

configure() {
    export CC=${SERENITY_ROOT}/Toolchain/Local/${SERENITY_ARCH}/bin/${SERENITY_ARCH}-pc-serenity-gcc 
    export CFLAGS="" 
    export LDFLAGS="-lncurses" 
    run ./configure --no-thanks
}

install() {
    run cp oksh "${SERENITY_BUILD_DIR}/Root/bin"
}
