#!/usr/bin/env -S bash ../.port_include.sh
port='open-adventure'
version='1.20'
files=(
    "https://gitlab.com/esr/open-adventure/-/archive/1.20/open-adventure-${version}.zip#441637afaf4f7db0e96b3d27ab8fb17ca68e0df343c72a9e4416569de1b4ea50"
)

depends=(
    'editline'
)

bin_path='/usr/local/bin'

build() {
    export PKG_CONFIG_PATH="${SERENITY_INSTALL_ROOT}/usr/local/lib/pkgconfig/"
    run make
}

install() {
    run_nocd mkdir -p "${SERENITY_INSTALL_ROOT}/${bin_path}/"
    run cp advent "${SERENITY_INSTALL_ROOT}/${bin_path}/"
}
