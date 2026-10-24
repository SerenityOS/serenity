#!/usr/bin/env -S bash ../.port_include.sh
port='openssl'
version='4.1.0'
useconfigure='true'
configscript='Configure'
files=(
    "https://www.openssl.org/source/openssl-${version}.tar.gz#50c17baf54cb008f6ce42d50deb4b28050a1aad711636cabd4b241c7e90cc2fd"
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
