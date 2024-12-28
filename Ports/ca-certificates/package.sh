#!/usr/bin/env -S bash ../.port_include.sh
port='ca-certificates'
version='2024-11-26'
files=(
    "https://curl.se/ca/cacert-${version}.pem#bb1782d281fe60d4a2dcf41bc229abe3e46c280212597d4abcc25bddf667739b"
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
