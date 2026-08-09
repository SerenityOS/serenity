#!/usr/bin/env -S bash ../.port_include.sh
port='ca-certificates'
version='2026-07-16'
files=(
    "https://curl.se/ca/cacert-${version}.pem#3ff344e30b9b1ed2971044eabb438a08f2e2245ddb5f8ab1a3ad8b63ab4eaf91"
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

    # Create symlinks in /usr/local/ssl since some ports don't read /etc/ssl
    mkdir -p "${SERENITY_INSTALL_ROOT}/usr/local/ssl"
    rm -rvf "${SERENITY_INSTALL_ROOT}/usr/local/ssl/certs"
    ln -svf "/etc/ssl/certs" "${SERENITY_INSTALL_ROOT}/usr/local/ssl"
    ln -svf "/etc/ssl/certs/ca-certificates.crt" "${SERENITY_INSTALL_ROOT}/usr/local/ssl/cert.pem"
}
