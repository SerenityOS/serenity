#!/usr/bin/env -S bash ../.port_include.sh
port=bzip2
version=1.0.8
files=(
    "https://sourceware.org/pub/bzip2/bzip2-${version}.tar.gz#ab5a03176ee106d3f0fa90e381da478ddae405918153cca248e682cd0c4a2269"
)
makeopts=("bzip2")
installopts=("PREFIX=${SERENITY_INSTALL_ROOT}/usr/local")

build() {
    run make CC="${CC}" AR="${AR}" RANLIB="${RANLIB}" "${makeopts[@]}" bzip2
}

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} CC="${CC}" AR="${AR}" RANLIB="${RANLIB}" "${installopts[@]}" install
}
