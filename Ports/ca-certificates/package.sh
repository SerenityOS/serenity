#!/usr/bin/env -S bash ../.port_include.sh
port='ca-certificates'
version='2024-12-31'
files=(
    "https://curl.se/ca/cacert-${version}.pem#a3f328c21e39ddd1f2be1cea43ac0dec819eaa20a90425d7da901a11531b3aa5"
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
