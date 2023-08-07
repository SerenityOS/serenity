#!/usr/bin/env -S bash ../.port_include.sh
port='mbedtls'
version='3.1.0'
files=(
    "https://github.com/Mbed-TLS/mbedtls/archive/refs/tags/v${version}.tar.gz b02df6f68dd1537e115a8497d5c173dc71edc55ad084756e57a30f951b725acd"
)
makeopts=(
    "SHARED=1"
)

install() {
    run make DESTDIR="${SERENITY_INSTALL_ROOT}/usr/local" "${installopts[@]}" install
}
