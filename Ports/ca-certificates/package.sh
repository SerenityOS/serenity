#!/usr/bin/env -S bash ../.port_include.sh
port=ca-certificates
version=2022-04-26
files=(
    "https://curl.se/ca/cacert-${version}.pem cacert-${version}.pem 08df40e8f528ed283b0e480ba4bcdbfdd2fdcf695a7ada1668243072d80f8b6f"
)
workdir="."

configure() {
    return
}

build() {
    return
}

install() {
    mkdir -p "${SERENITY_INSTALL_ROOT}/etc/ssl/certs"
    cp -vf "cacert-${version}.pem" "${SERENITY_INSTALL_ROOT}/etc/ssl/certs/ca-certificates.crt"
}
