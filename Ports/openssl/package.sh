#!/usr/bin/env -S bash ../.port_include.sh
port='openssl'
version='3.3.1'
useconfigure='true'
configscript='Configure'
files=(
    "https://www.openssl.org/source/openssl-${version}.tar.gz#777cd596284c883375a2a7a11bf5d2786fc5413255efab20c50d6ffe6d020b7e"
)
depends=(
    'zlib'
)
configopts=(
    '--prefix=/usr/local'
    '-DOPENSSL_SYS_SERENITY=1'
    '-DOPENSSL_USE_IPV6=0'
    'no-asm'
    'no-tests'
    'serenity-generic'
    'threads'
    'zlib'
)

configure() {
    run ./"${configscript}" "${configopts[@]}"
}

install() {
    # The default "install" also installs docs, which we don't want.
    run make DESTDIR="${DESTDIR}" install_sw "${installopts[@]}"
    run make DESTDIR="${DESTDIR}" install_ssldirs "${installopts[@]}"
}
