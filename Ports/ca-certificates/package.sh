#!/usr/bin/env -S bash ../.port_include.sh
port='ca-certificates'
version='2024-07-02'
files=(
    "https://curl.se/ca/cacert-${version}.pem#1bf458412568e134a4514f5e170a328d11091e071c7110955c9884ed87972ac9"
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
