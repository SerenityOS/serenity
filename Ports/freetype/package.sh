#!/usr/bin/env -S bash ../.port_include.sh
port=freetype
version=2.12.0
useconfigure=true
use_fresh_config_sub=true
config_sub_path=builds/unix/config.sub
files="https://download.savannah.gnu.org/releases/freetype/freetype-${version}.tar.gz freetype-${version}.tar.gz 7940a46eeb0255baaa87c553d72778c4f8daa2b8888c8e2a05766a2a8686740c"
auth_type=sha256
configopts=("--with-brotli=no" "--with-bzip2=no" "--with-zlib=no" "--with-harfbuzz=no" "--with-png=no")
config_sub_path="builds/unix/config.sub"

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} "${installopts[@]}" install
    ${CC} -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libfreetype.so -Wl,-soname,libfreetype.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libfreetype.a -Wl,--no-whole-archive
    rm -f ${SERENITY_INSTALL_ROOT}/usr/local/lib/libfreetype.la
}
