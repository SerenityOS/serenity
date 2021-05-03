#!/usr/bin/env -S bash ../.port_include.sh
port=openttd
version=1.11.0
auth_type=sha256
depends="SDL2 libpng zlib xz"
files="https://cdn.openttd.org/openttd-releases/${version}/openttd-${version}-source.tar.xz openttd-${version}.tar.xz 5e65184e07368ba1afa62dbb3e35abaee6c4da6730ff4bc9eb4447d53363c7a8"
useconfigure=true
configopts="-DCMAKE_TOOLCHAIN_FILE=$SERENITY_ROOT/Toolchain/CMake/CMakeToolchain.txt"

configure() {
    host_env
    mkdir -p $workdir/host-build
    (
        cd $workdir/host-build
        cmake .. -DOPTION_TOOLS_ONLY=1
    )

    target_env
    mkdir -p $workdir/build
    (
        cd $workdir/build
        cmake .. $configopts -DHOST_BINARY_DIR=$(pwd)/../host-build
    )
}

build() {
    host_env
    (
        cd $workdir/host-build
        make $makeopts
    )

    target_env
    (
        cd $workdir/build
        make $makeopts
    )
}

install() {
    (
        cd $workdir/build
        make install
    )

    ln -sf /usr/local/games/openttd $DESTDIR/usr/local/bin/openttd
}
