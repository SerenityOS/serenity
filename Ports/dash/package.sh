#!/usr/bin/env -S bash ../.port_include.sh
port='dash'
version='0.5.13.6'
useconfigure='true'
files=(
    "http://gondor.apana.org.au/~herbert/dash/files/dash-${version}.tar.gz#26aac476f476cef391c600cb064417d82a5465d8fc7c4fd56158c276e86538c1"
)

configure() {
    host_env
    run autoupdate
    run autoconf
    run aclocal
    run automake --add-missing
    run mkdir -p host-build
    run sh -c "cd host-build && ../configure ${configopts[@]} CFLAGS=-I."
    target_env
    run mkdir -p target-build
    run sh -c "cd target-build && ../configure --host="${SERENITY_ARCH}-serenity" --disable-helpers ${configopts[@]} CFLAGS=-I."
}

build() {
    host_env
    run sh -c "cd host-build && make ${makeopts[@]}"
    run cp host-build/src/{mkinit,mksyntax,mknodes,mksignames} src
    target_env
    run sh -c "cd target-build && make ${makeopts[@]}"
}

install() {
    run sh -c "cd target-build && make DESTDIR="${SERENITY_INSTALL_ROOT}" ${installopts[@]} install"
}
