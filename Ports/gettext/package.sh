#!/usr/bin/env -S bash ../.port_include.sh
port='gettext'
version='0.21.1'
useconfigure='true'
files="https://ftpmirror.gnu.org/gettext/gettext-${version}.tar.gz gettext-${version}.tar.gz e8c3650e1d8cee875c4f355642382c1df83058bd5a11ee8555c0cf276d646d45"
depends=("libiconv")
use_fresh_config_sub='true'
config_sub_paths=("build-aux/config.sub" "libtextstyle/build-aux/config.sub")

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} "${installopts[@]}" install
    ${CC} -shared -pthread -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libintl.so -Wl,-soname,libintl.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libintl.a -Wl,--no-whole-archive -liconv
    rm -f ${SERENITY_INSTALL_ROOT}/usr/local/lib/libintl.la
}
