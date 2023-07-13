#!/usr/bin/env -S bash ../.port_include.sh
port=openssl
branch='1.1.1'
version="${branch}s"
useconfigure=true
configscript=Configure
files="https://www.openssl.org/source/openssl-${version}.tar.gz openssl-${version}.tar.gz c5ac01e760ee6ff0dab61d6b2bbd30146724d063eb322180c6f18a6f74e4b6aa"

depends=("zlib")
configopts=("--prefix=/usr/local" "-DOPENSSL_SYS_SERENITY=1" "-DOPENSSL_USE_IPV6=0" "zlib" "threads" "no-tests" "no-asm" "serenity-generic")

configure() {
    export LDFLAGS="-L${SERENITY_INSTALL_ROOT}/usr/local/lib"
    run ./"$configscript" "${configopts[@]}"
}

install() {
    # The default "install" also installs docs, which we don't want.
    run make DESTDIR=$DESTDIR install_sw "${installopts[@]}"
    run make DESTDIR=$DESTDIR install_ssldirs "${installopts[@]}"
}
