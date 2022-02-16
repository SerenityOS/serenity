#!/usr/bin/env -S bash ../.port_include.sh

port="OpenJDK"
version="17"
workdir=jdk-jdk-17-ga
useconfigure="true"
files="https://github.com/openjdk/jdk/archive/refs/tags/jdk-17-ga.tar.gz jdk-17-ga.tar.gz"
depends=("cups" "fontconfig" "libffi")

configure() {
    run bash configure --host=${SERENITY_ARCH}-pc-serenity --target=${SERENITY_ARCH}-pc-serenity --build=x86_64-unknown-linux-gnu --with-jvm-variants=zero --enable-headless-only --with-debug-level=slowdebug --with-native-debug-symbols=internal
}

build() {
    run make java.base
}

install() {
    run pwd
    run sh -c "cp ./build/serenity-*/jdk/ ${SERENITY_INSTALL_ROOT}/home/anon/ -rf"
}
