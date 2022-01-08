#!/usr/bin/env -S bash ../.port_include.sh
port=curl
version=7.81.0
useconfigure=true
files="https://curl.se/download/curl-${version}.tar.bz2 curl-${version}.tar.bz2 1e7a38d7018ec060f1f16df839854f0889e94e122c4cfa5d3a37c2dc56f1e258"
auth_type=sha256
depends=("openssl" "zlib" "zstd")
configopts=("--disable-ntlm-wb" "--with-openssl=${SERENITY_INSTALL_ROOT}/usr/local" "--disable-symbol-hiding")

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} "${installopts[@]}" install
    ${CC} -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libcurl.so -Wl,-soname,libcurl.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libcurl.a -Wl,--no-whole-archive -lzstd
    rm -f ${SERENITY_INSTALL_ROOT}/usr/local/lib/libcurl.la
}
