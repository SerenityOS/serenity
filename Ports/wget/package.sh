#!/usr/bin/env -S bash ../.port_include.sh
port=wget
version=1.21.3
useconfigure="true"
use_fresh_config_sub=true
config_sub_paths=("build-aux/config.sub")
depends=("openssl")
files=(
    "https://ftpmirror.gnu.org/gnu/wget/wget-${version}.tar.gz wget-${version}.tar.gz 5726bb8bc5ca0f6dc7110f6416e4bb7019e2d2ff5bf93d1ca2ffcc6656f220e5"
)
configopts=("--with-ssl=openssl" "--disable-ipv6")

export OPENSSL_LIBS="-lssl -lcrypto -ldl"
