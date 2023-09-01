#!/usr/bin/env -S bash ../.port_include.sh
port='qoi'
version='351450e00d6d7b062ab028110b72b317a98e03ea'
files=(
    "https://github.com/phoboslab/qoi/archive/${version}.zip#245c0262d64ed49fd3c9f05313d85912b9fcda1095bd492242f4f8130b02c192"
)
depends=(
    'libpng'
    'stb'
)

install() {
    run cp qoibench "${SERENITY_INSTALL_ROOT}/bin"
    run cp qoiconv "${SERENITY_INSTALL_ROOT}/bin"
}
