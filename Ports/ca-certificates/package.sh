#!/usr/bin/env -S bash ../.port_include.sh
port=ca-certificates
version=2022-02-01
files="https://curl.se/ca/cacert-${version}.pem cacert-${version}.pem 1d9195b76d2ea25c2b5ae9bee52d05075244d78fcd9c58ee0b6fac47d395a5eb"
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
