#!/usr/bin/env -S bash ../.port_include.sh
port='qop'
version='8dbec2d286c1d3450e42d3422fd655f78818a0f9'
files=(
    "https://github.com/phoboslab/qop/archive/${version}.zip#a900408f3f67cb8666dd5a66abf962ac5a1dc24a3974c4495f061e9d2bd936ff"
)

build() {
    export CC="${SERENITY_SOURCE_DIR}/Toolchain/Local/${SERENITY_ARCH}/bin/${SERENITY_ARCH}-pc-serenity-gcc"
    run make CC="${CC}"
}

install() {
    run cp qopconv "${SERENITY_INSTALL_ROOT}/bin"

    target_dir="${SERENITY_INSTALL_ROOT}/usr/local/include/"
    run cp qop.h "${target_dir}"
}
