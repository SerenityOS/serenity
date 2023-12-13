#!/usr/bin/env -S bash ../.port_include.sh
port='ca-certificates'
version='2023-12-12'
files=(
    "https://curl.se/ca/cacert-${version}.pem#ccbdfc2fe1a0d7bbbb9cc15710271acf1bb1afe4c8f1725fe95c4c7733fcbe5a"
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
