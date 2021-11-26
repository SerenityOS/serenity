#!/usr/bin/env -S bash ../.port_include.sh
port=gettext
version=0.21
useconfigure=true
files="https://ftp.gnu.org/pub/gnu/gettext/gettext-${version}.tar.gz gettext-${version}.tar.gz c77d0da3102aec9c07f43671e60611ebff89a996ef159497ce8e59d075786b12"
auth_type=sha256
depends=("libiconv")

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} "${installopts[@]}" install
    ${CC} -shared -pthread -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libintl.so -Wl,-soname,libintl.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libintl.a -Wl,--no-whole-archive -liconv
    rm -f ${SERENITY_INSTALL_ROOT}/usr/local/lib/libintl.la
}
