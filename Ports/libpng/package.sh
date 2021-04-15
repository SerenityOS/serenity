#!/usr/bin/env -S bash ../.port_include.sh
port=libpng
version=1.6.37
useconfigure=true
files="https://download.sourceforge.net/libpng/libpng-${version}.tar.gz libpng-${version}.tar.gz"
depends="zlib"

install() {
    run make DESTDIR=$DESTDIR $installopts install
    ${CC} -shared -o $DESTDIR/usr/local/lib/libpng16.so -Wl,--whole-archive $DESTDIR/usr/local/lib/libpng16.a -Wl,--no-whole-archive -lz
    ln -sf libpng16.so $DESTDIR/usr/local/lib/libpng.so
    rm -f $DESTDIR/usr/local/lib/libpng16.la $DESTDIR/usr/local/lib/libpng.la
}
