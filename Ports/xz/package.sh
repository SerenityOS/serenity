#!/usr/bin/env -S bash ../.port_include.sh
port=xz
version=5.2.5
useconfigure=true
files="https://tukaani.org/xz/xz-${version}.tar.gz xz-${version}.tar.gz f6f4910fd033078738bd82bfba4f49219d03b17eb0794eb91efbae419f4aba10"
auth_type=sha256
depends=("zlib" "libiconv")

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} "${installopts[@]}" install
    ${CC} -pthread -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/liblzma.so -Wl,-soname,liblzma.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/liblzma.a -Wl,--no-whole-archive -lz -liconv
    rm -f ${SERENITY_INSTALL_ROOT}/usr/local/lib/liblzma.la
}
