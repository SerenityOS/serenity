#!/usr/bin/env -S bash ../.port_include.sh
port=libpng
version=1.6.37
useconfigure=true
files="https://download.sourceforge.net/libpng/libpng-${version}.tar.gz libpng-${version}.tar.gz 6c7519f6c75939efa0ed3053197abd54"
auth_type=md5
depends="zlib"

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} $installopts install
    ${CC} -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libpng16.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libpng16.a -Wl,--no-whole-archive -lz
    ln -sf libpng16.so ${SERENITY_INSTALL_ROOT}/usr/local/lib/libpng.so
    rm -f ${SERENITY_INSTALL_ROOT}/usr/local/lib/libpng16.la ${SERENITY_INSTALL_ROOT}/usr/local/lib/libpng.la
}
