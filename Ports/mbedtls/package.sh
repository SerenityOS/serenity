#!/usr/bin/env -S bash ../.port_include.sh
port=mbedtls
version=2.16.2
files="https://tls.mbed.org/download/mbedtls-${version}-apache.tgz mbedtls-${version}-apache.tgz a6834fcd7b7e64b83dfaaa6ee695198cb5019a929b2806cb0162e049f98206a4"
makeopts=("CFLAGS=-DPLATFORM_UTIL_USE_GMTIME")
auth_type=sha256

install() {
    run make DESTDIR="${SERENITY_INSTALL_ROOT}/usr/local" "${installopts[@]}" install
    ${CC} -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libmbedcrypto.so -Wl,-soname,libmbedcrypto.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libmbedcrypto.a -Wl,--no-whole-archive
    ${CC} -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libmbedx509.so -Wl,-soname,libmbedx509.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libmbedx509.a -Wl,--no-whole-archive -lmbedcrypto
    ${CC} -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libmbedtls.so -Wl,-soname,libmbedtls.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libmbedtls.a -Wl,--no-whole-archive -lmbedcrypto -lmbedx509
}
