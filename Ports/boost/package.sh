#!/usr/bin/env -S bash ../.port_include.sh
port='boost'
version='1.83.0'
useconfigure='true'
depends=(
    'zlib'
    'bzip2'
    'zstd'
    'xz'
    'libicu'
)
files=(
    "https://github.com/boostorg/boost/releases/download/boost-${version}/boost-${version}.tar.gz#0c6049764e80aa32754acd7d4f179fd5551d8172a83b71532ae093e7384e98da"
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
