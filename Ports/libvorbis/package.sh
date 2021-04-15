#!/usr/bin/env -S bash ../.port_include.sh
port=libvorbis
version=1.3.7
useconfigure=true
files="https://github.com/xiph/vorbis/releases/download/v${version}/libvorbis-${version}.tar.gz libvorbis-${version}.tar.gz"
depends=libogg

install() {
    run make DESTDIR=$DESTDIR $installopts install
    ${CC} -shared -o $DESTDIR/usr/local/lib/libvorbis.so -Wl,--whole-archive $DESTDIR/usr/local/lib/libvorbis.a -Wl,--no-whole-archive -logg
    rm -f $DESTDIR/usr/local/lib/libvorbis.la
    ${CC} -shared -o $DESTDIR/usr/local/lib/libvorbisenc.so -Wl,--whole-archive $DESTDIR/usr/local/lib/libvorbisenc.a -Wl,--no-whole-archive -lvorbis
    rm -f $DESTDIR/usr/local/lib/libvorbisenc.la
    ${CC} -shared -o $DESTDIR/usr/local/lib/libvorbisfile.so -Wl,--whole-archive $DESTDIR/usr/local/lib/libvorbisfile.a -Wl,--no-whole-archive -lvorbis
    rm -f $DESTDIR/usr/local/lib/libvorbisfile.la
}
