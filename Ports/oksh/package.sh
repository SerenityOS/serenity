#!/usr/bin/env -S bash ../.port_include.sh
port=oksh
useconfigure=true
version=6.8.1
depends=("ncurses")
workdir=oksh-${version}
files="https://github.com/ibara/oksh/releases/download/oksh-${version}/oksh-${version}.tar.gz oksh-${version}.tar.gz ddd2b27b99009a4ee58ddf58da73edf83962018066ccf33b2fe1f570a00917b0"
auth_type=sha256

configure() {
    export CC=${SERENITY_SOURCE_DIR}/Toolchain/Local/${SERENITY_ARCH}/bin/${SERENITY_ARCH}-pc-serenity-gcc 
    export CFLAGS="" 
    export LDFLAGS="-lncurses" 
    run ./configure --no-thanks
}

install() {
    run cp oksh "${SERENITY_INSTALL_ROOT}/bin"
}
