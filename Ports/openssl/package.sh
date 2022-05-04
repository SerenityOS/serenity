#!/usr/bin/env -S bash ../.port_include.sh
port=openssl
branch='1.1.1'
version="${branch}o"
useconfigure=true
configscript=Configure
files="https://ftp.nluug.nl/security/openssl/openssl-${version}.tar.gz openssl-${version}.tar.gz 9384a2b0570dd80358841464677115df785edb941c71211f75076d72fe6b438f"
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
