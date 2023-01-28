#!/usr/bin/env -S bash ../.port_include.sh
port=ca-certificates
description='Mozilla CA certificate store'
version=2022-04-26
website='https://curl.se/docs/caextract.html'
files="https://curl.se/ca/cacert-${version}.pem cacert-${version}.pem 08df40e8f528ed283b0e480ba4bcdbfdd2fdcf695a7ada1668243072d80f8b6f"
auth_type=sha256
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
