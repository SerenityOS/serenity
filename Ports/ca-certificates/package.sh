#!/usr/bin/env -S bash ../.port_include.sh
port='ca-certificates'
version='2026-09-25'
files=(
    "https://curl.se/ca/cacert-${version}.pem#a41b5d356aea97a529fe27e0f7316d2f9d946d75927476cf9cf1b90637d00505"
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
