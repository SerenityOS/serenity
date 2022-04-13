#!/usr/bin/env -S bash ../.port_include.sh
port="aria2"
version="1.36.0"
files="https://github.com/aria2/aria2/releases/download/release-${version}/aria2-${version}.tar.xz ${port}-${version}.tar.xz 58d1e7608c12404f0229a3d9a4953d0d00c18040504498b483305bcb3de907a5"
auth_type="sha256"
depends=("libssh2" "libxml2" "openssl" "zlib" "libuv")
useconfigure="true"
configopts+=("--with-libuv")

pre_configure() {
    run autoreconf -i
}
