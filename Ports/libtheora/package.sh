#!/usr/bin/env -S bash ../.port_include.sh
port=libtheora
version=1.1.1
useconfigure=true
configopts=()
use_fresh_config_sub=true
files=(
    "https://ftp.osuosl.org/pub/xiph/releases/theora/libtheora-${version}.tar.bz2#b6ae1ee2fa3d42ac489287d3ec34c5885730b1296f0801ae577a35193d3affbc"
)
depends=("libvorbis")
configopts=("--disable-examples" "--disable-static" "--enable-shared")
