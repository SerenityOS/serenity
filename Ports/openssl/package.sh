#!/usr/bin/env -S bash ../.port_include.sh
port='openssl'
version='3.5.1'
useconfigure='true'
configscript='Configure'
files=(
    "https://www.openssl.org/source/openssl-${version}.tar.gz#529043b15cffa5f36077a4d0af83f3de399807181d607441d734196d889b641f"
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
