#!/usr/bin/env -S bash ../.port_include.sh
port=openssl
branch='1.1.1'
version="${branch}k"
useconfigure=true
configscript=Configure
files="https://ftp.nluug.nl/security/openssl/openssl-${version}.tar.gz openssl-${version}.tar.gz 892a0875b9872acd04a9fde79b1f943075d5ea162415de3047c327df33fbaee5"
auth_type=sha256

depends=("zlib")
configopts=("--prefix=/usr/local" "-DOPENSSL_SYS_SERENITY=1" "-DOPENSSL_USE_IPV6=0" "zlib" "threads" "no-tests" "no-asm" "serenity-generic")

configure() {
    run ./"$configscript" "${configopts[@]}"
}

install() {
    # The default "install" also installs docs, which we don't want.
    run make DESTDIR=$DESTDIR install_sw "${installopts[@]}"
    run make DESTDIR=$DESTDIR install_ssldirs "${installopts[@]}"
}
