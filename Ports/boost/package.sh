#!/usr/bin/env -S bash ../.port_include.sh
port='boost'
version='1.83.0'
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
    "https://boostorg.jfrog.io/artifactory/main/release/${version}/source/boost_${version//./_}.tar.bz2#6478edfe2f3305127cffe8caf73ea0176c53769f4bf1585be237eb30798c3b8e"
)
bjamopts=(
    '--user-config=user-config.jam'
    'toolset=gcc'
    'target-os=serenity'
)

configure() {
    run ./bootstrap.sh \
        --with-icu="${DESTDIR}/usr/local" \
        --prefix="${DESTDIR}/usr/local" \
        --without-libraries='python'
    echo "using gcc : : ${CXX} ;" > "${workdir}/user-config.jam"
}

build() {
    run ./b2 "${bjamopts[@]}"
}

install() {
    run ./b2 "${bjamopts[@]}" install
}
