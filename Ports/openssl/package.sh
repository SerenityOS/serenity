#!/usr/bin/env -S bash ../.port_include.sh
port=openssl
branch='1.1.1'
version="${branch}m"
useconfigure=true
configscript=Configure
files="https://ftp.nluug.nl/security/openssl/openssl-${version}.tar.gz openssl-${version}.tar.gz f89199be8b23ca45fc7cb9f1d8d3ee67312318286ad030f5316aca6462db6c96"
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
