#!/usr/bin/env -S bash ../.port_include.sh
port=oksh
useconfigure=true
version=6.8.1
depends="ncurses"
workdir=oksh-${version}
files="https://github.com/ibara/oksh/releases/download/oksh-${version}/oksh-${version}.tar.gz oksh-${version}.tar.gz ce8b7c278e6d36bbbd7b54c218fae7ba"
auth_type=md5

configure() {
    export CC=${SERENITY_SOURCE_DIR}/Toolchain/Local/${SERENITY_ARCH}/bin/${SERENITY_ARCH}-pc-serenity-gcc 
    export CFLAGS="" 
    export LDFLAGS="-lncurses" 
    run ./configure --no-thanks
}

install() {
    run cp oksh "${SERENITY_BUILD_DIR}/Root/bin"
}
