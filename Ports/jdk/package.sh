#!/usr/bin/env -S bash ../.port_include.sh
port=jdk
useconfigure=true

##### Java 18 #####
version="jdk-18-10"
download_version="18+10"
sha="0eebf9c698163edcfa7a2ef773123e3c714d273894b4367f9cac8f7ad21eab42"

##### Java 11 #####
#version="jdk-11-28"
#download_version="11+28"
#sha="306fee8a3945a87abaff67f09810a41a6983acb2a7b1583f7782ce1b7e0b04e6"

files="https://github.com/openjdk/jdk/archive/refs/tags/jdk-${download_version}.tar.gz jdk-${version}.tar.gz ${sha}"
auth_type="sha256"

configure() {
    host_env
    CC=../../../Toolchain/Local/i686/bin/i686-pc-serenity-gcc
    run sh configure --openjdk-target=${SERENITY_ARCH}-pc-serenity --enable-headless-only=yes --enable-ccache $configopts
}

build() {
    host_env
    run make $makeopts images
}

#install() {
#    run make DESTDIR=${SERENITY_INSTALL_ROOT} $installopts install
#    #run sh -c "cd target-build && make DESTDIR="${SERENITY_INSTALL_ROOT}" $installopts install"
#}
