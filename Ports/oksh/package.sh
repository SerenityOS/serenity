#!/usr/bin/env -S bash ../.port_include.sh
port='oksh'
version='7.1'
files=(
    "https://github.com/ibara/oksh/releases/download/oksh-${version}/oksh-${version}.tar.gz#9dc0b0578d9d64d10c834f9757ca11f526b562bc5454da64b2cb270122f52064"
)
useconfigure='true'
depends=("ncurses")

export LDFLAGS='-lncurses'

configure() {
    run ./configure --no-thanks
}

install() {
    run mkdir -p "${SERENITY_INSTALL_ROOT}/usr/local/bin"
    run cp oksh "${SERENITY_INSTALL_ROOT}/usr/local/bin"
}
