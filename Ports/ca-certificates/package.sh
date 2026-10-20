#!/usr/bin/env -S bash ../.port_include.sh
port='ca-certificates'
version='2026-10-20'
files=(
    "https://curl.se/ca/cacert-${version}.pem#796f1389bfea13c0ed78f663b765baaff54a508e5483786bd6ece73c5e3e3cbe"
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
