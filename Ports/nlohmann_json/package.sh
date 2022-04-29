#!/usr/bin/env -S bash ../.port_include.sh
port=nlohmann_json
version=3.10.5
files="https://github.com/nlohmann/json/archive/refs/tags/v${version}.tar.gz ${port}-${version}.tar.gz 5daca6ca216495edf89d167f808d1d03c4a4d929cef7da5e10f135ae1540c7e4"
auth_type=sha256
workdir="json-${version}"

install() {
    cp -r ${workdir}/include/nlohmann "${DESTDIR}/usr/local/include"
}
