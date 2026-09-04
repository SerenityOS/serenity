#!/usr/bin/env -S bash ../.port_include.sh
port='libtheora'
version='1.2.0'
useconfigure='true'
files=(
    "https://ftp.osuosl.org/pub/xiph/releases/theora/libtheora-${version}.tar.gz#279327339903b544c28a92aeada7d0dcfd0397b59c2f368cc698ac56f515906e"
)
depends=("libvorbis")
configopts=("--disable-examples" "--disable-static" "--enable-shared")
