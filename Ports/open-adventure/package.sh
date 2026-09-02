#!/usr/bin/env -S bash ../.port_include.sh
port='open-adventure'
version='1.22'
files=(
    "https://gitlab.com/esr/open-adventure/-/archive/${version}/open-adventure-${version}.zip#f81b69edb1aed61cfcbceef02173d217527e83b22a867ccdf8d65936ab9b05db"
)
depends=(
    'editline'
)

build() {
    export PKG_CONFIG_PATH="${SERENITY_INSTALL_ROOT}/usr/local/lib/pkgconfig/"
    run make
}

install() {
    run_nocd mkdir -p "${SERENITY_INSTALL_ROOT}/usr/local/bin"
    run cp advent "${SERENITY_INSTALL_ROOT}/usr/local/bin/"
}
