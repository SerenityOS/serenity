#!/usr/bin/env -S bash ../.port_include.sh
port='htop'
version='3.5.0'
files=(
    "https://github.com/htop-dev/htop/releases/download/${version}/htop-${version}.tar.xz#b6586e405c5223ebe5ac7828df21edad45cbf90288088bd1b18ad8fa700ffa05"
)
depends=("ncurses")
useconfigure='true'
export CXXFLAGS="-std=c++26"
configopts=(
    "--disable-affinity"
    "--disable-delayacct"
    "--with-ncurses"
)

post_fetch() {
    cp -r "${PORT_META_DIR}/src/serenity" "${workdir}/"
}

pre_configure() {
    run autoreconf -fi
}
