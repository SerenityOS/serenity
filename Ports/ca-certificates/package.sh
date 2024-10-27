#!/usr/bin/env -S bash ../.port_include.sh
port='ca-certificates'
version='2024-09-24'
files=(
    "https://curl.se/ca/cacert-${version}.pem#189d3cf6d103185fba06d76c1af915263c6d42225481a1759e853b33ac857540"
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
