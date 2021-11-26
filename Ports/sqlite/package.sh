#!/usr/bin/env -S bash ../.port_include.sh
port=sqlite
useconfigure="true"
version="3350500"
files="https://www.sqlite.org/2021/sqlite-autoconf-${version}.tar.gz sqlite-autoconf-${version}.tar.gz f52b72a5c319c3e516ed7a92e123139a6e87af08a2dc43d7757724f6132e6db0"
auth_type=sha256
workdir="sqlite-autoconf-${version}"

post_install() {
    ${CC} -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libsqlite3.so -Wl,-soname,libsqlite3.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libsqlite3.a -Wl,--no-whole-archive
    rm ${SERENITY_INSTALL_ROOT}/usr/local/lib/libsqlite3.la
}
