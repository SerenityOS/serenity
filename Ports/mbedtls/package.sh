#!/usr/bin/env -S bash ../.port_include.sh
port='mbedtls'
version='3.4.1'
files=(
    "https://github.com/Mbed-TLS/mbedtls/archive/refs/tags/v${version}.tar.gz#a420fcf7103e54e775c383e3751729b8fb2dcd087f6165befd13f28315f754f5"
)
makeopts+=(
    'SHARED=1'
)

install() {
    run make \
        DESTDIR="${SERENITY_INSTALL_ROOT}/usr/local" \
        "${installopts[@]}" \
        install
}
