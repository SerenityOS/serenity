#!/usr/bin/env -S bash ../.port_include.sh
port=openssl
branch='1.1.1'
version="${branch}q"
useconfigure=true
configscript=Configure
files="https://www.openssl.org/source/openssl-${version}.tar.gz openssl-${version}.tar.gz d7939ce614029cdff0b6c20f0e2e5703158a489a72b2507b8bd51bf8c8fd10ca"
auth_type=sha256

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
