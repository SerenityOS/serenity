#!/usr/bin/env -S bash ../.port_include.sh
port='openssl'
version='3.1.2'
useconfigure='true'
configscript='Configure'
files=(
    "https://www.openssl.org/source/openssl-${version}.tar.gz#a0ce69b8b97ea6a35b96875235aa453b966ba3cba8af2de23657d8b6767d6539"
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
    export LDFLAGS="-L${SERENITY_INSTALL_ROOT}/usr/local/lib"
    run ./"${configscript}" "${configopts[@]}"
}

install() {
    # The default "install" also installs docs, which we don't want.
    run make DESTDIR="${DESTDIR}" install_sw "${installopts[@]}"
    run make DESTDIR="${DESTDIR}" install_ssldirs "${installopts[@]}"
}
