#!/usr/bin/env -S bash ../.port_include.sh
port='qoi'
version='edb8d7b1140be3168cc99ed87edc605c7c1cf31f'
files=(
    "https://github.com/phoboslab/qoi/archive/${version}.zip ${version}.zip 3d3df95fb0b59aca2113ce45396f887eaba1be914cd54b56804efc241f93f203"
)
depends=('libpng' 'stb')

install() {
    run cp qoibench "${SERENITY_INSTALL_ROOT}/bin"
    run cp qoiconv "${SERENITY_INSTALL_ROOT}/bin"
}
