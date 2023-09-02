#!/usr/bin/env -S bash ../.port_include.sh
port='boost'
version='1.80.0'
useconfigure='true'
workdir="boost_${version//./_}"
depends=(
    'zlib'
    'bzip2'
    'zstd'
    'xz'
    'libicu'
)
files=(
    "https://boostorg.jfrog.io/artifactory/main/release/${version}/source/boost_${version//./_}.tar.bz2#1e19565d82e43bc59209a168f5ac899d3ba471d55c7610c677d4ccf2c9c500c0"
)
bjamopts=(
    '--user-config=user-config.jam'
    'toolset=gcc'
    'target-os=serenity'
)

configure() {
    run ./bootstrap.sh --with-icu=${DESTDIR}/usr/local --prefix=${DESTDIR}/usr/local --without-libraries=python
    echo "using gcc : : $CXX ;" >$workdir/user-config.jam
}

build() {
    run ./b2 "${bjamopts[@]}"
}

install() {
    run ./b2 "${bjamopts[@]}" install
}
