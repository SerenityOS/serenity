#!/usr/bin/env -S bash ../.port_include.sh
port='ca-certificates'
version='2023-08-22'
files=(
    "https://curl.se/ca/cacert-${version}.pem#23c2469e2a568362a62eecf1b49ed90a15621e6fa30e29947ded3436422de9b9"
)
workdir='.'

configure() {
    :
}

build() {
    :
}

install() {
    mkdir -p "${SERENITY_INSTALL_ROOT}/etc/ssl/certs"
    cp -vf "cacert-${version}.pem" "${SERENITY_INSTALL_ROOT}/etc/ssl/certs/ca-certificates.crt"
}
