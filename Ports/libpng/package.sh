#!/usr/bin/env -S bash ../.port_include.sh
port=libpng
version=1.6.37
useconfigure=true
use_fresh_config_sub=true
files="https://download.sourceforge.net/libpng/libpng-${version}.tar.gz libpng-${version}.tar.gz daeb2620d829575513e35fecc83f0d3791a620b9b93d800b763542ece9390fb4"
auth_type=sha256
depends=("zlib")

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} "${installopts[@]}" install
    ${CC} -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libpng16.so -Wl,-soname,libpng16.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libpng16.a -Wl,--no-whole-archive -lz
    ln -sf libpng16.so ${SERENITY_INSTALL_ROOT}/usr/local/lib/libpng.so
    rm -f ${SERENITY_INSTALL_ROOT}/usr/local/lib/libpng16.la ${SERENITY_INSTALL_ROOT}/usr/local/lib/libpng.la
}
