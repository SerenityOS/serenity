#!/usr/bin/env -S bash ../.port_include.sh
port='ca-certificates'
version='2024-12-31'
files=(
    "https://curl.se/ca/cacert-${version}.pem#a3f328c21e39ddd1f2be1cea43ac0dec819eaa20a90425d7da901a11531b3aa5"
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
