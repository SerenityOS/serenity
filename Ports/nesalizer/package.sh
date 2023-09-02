#!/usr/bin/env -S bash ../.port_include.sh
port=nesalizer
version=5bb045845a5cc829a78b7384f848fdd886cd98c8
files=(
    "https://github.com/ulfalizer/nesalizer/archive/${version}.tar.gz#4282cb0e4af0585af4a594dfa30b2e350dd0efc39e5bc2d8312f637f50397107"
)
depends=("SDL2")
makeopts+=("CONF=release")

install() {
    run mkdir -p "${SERENITY_INSTALL_ROOT}/usr/local/bin/"
    run cp build/nesalizer "${SERENITY_INSTALL_ROOT}/usr/local/bin/"
}
