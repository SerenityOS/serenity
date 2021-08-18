#!/usr/bin/env -S bash ../.port_include.sh
port=mc
version=4.8.27
useconfigure=true
configopts="--disable-vfs-ftp"
files="http://ftp.midnight-commander.org/mc-${version}.tar.xz mc-${version}.tar.xz 31be59225ffa9920816e9a8b3be0ab225a16d19e4faf46890f25bdffa02a4ff4"
auth_type=sha256
depends="glib slang"
