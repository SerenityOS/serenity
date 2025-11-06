#!/usr/bin/env -S bash ../.port_include.sh
port='openssl'
version='3.6.0'
useconfigure='true'
configscript='Configure'
files=(
    "https://www.openssl.org/source/openssl-${version}.tar.gz#b6a5f44b7eb69e3fa35dbf15524405b44837a481d43d81daddde3ff21fcbb8e9"
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
