#!/usr/bin/env -S bash ../.port_include.sh
port=ca-certificates
version=2022-03-29
files="https://curl.se/ca/cacert-${version}.pem cacert-${version}.pem 1979e7fe618c51ed1c9df43bba92f977a0d3fe7497ffa2a5e80dfc559a1e5a29"
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
