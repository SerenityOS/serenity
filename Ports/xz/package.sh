#!/usr/bin/env -S bash ../.port_include.sh
port=xz
version=5.2.5
useconfigure=true
files="https://tukaani.org/xz/xz-${version}.tar.gz xz-${version}.tar.gz f6f4910fd033078738bd82bfba4f49219d03b17eb0794eb91efbae419f4aba10"
auth_type=sha256
depends="zlib libiconv"

install() {
    run make DESTDIR=$DESTDIR $installopts install
    ${CC} -shared -o $DESTDIR/usr/local/lib/liblzma.so -Wl,--whole-archive $DESTDIR/usr/local/lib/liblzma.a -Wl,--no-whole-archive -lz -liconv
    rm -f $DESTDIR/usr/local/lib/liblzma.la
}
