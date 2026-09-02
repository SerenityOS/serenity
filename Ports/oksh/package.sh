#!/usr/bin/env -S bash ../.port_include.sh
port='oksh'
version='7.9'
files=(
    "https://github.com/ibara/oksh/releases/download/oksh-${version}/oksh-${version}.tar.gz#51b2d92515950c959dbf24f6fc33336db8c0526c2a50fee4ca598a18a6114a49"
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
