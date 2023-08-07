#!/usr/bin/env -S bash ../.port_include.sh
port=openttd
version=12.2
depends=("freetype" "SDL2" "libicu" "libpng" "zlib" "xz" "openttd-opengfx" "openttd-opensfx")
files=(
    "https://cdn.openttd.org/openttd-releases/${version}/openttd-${version}-source.tar.xz 81508f0de93a0c264b216ef56a05f8381fff7bffa6d010121a21490b4dace95c"
)
useconfigure=true
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")
launcher_name=OpenTTD
launcher_category=Games
launcher_command=/usr/local/games/openttd

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
        cmake .. "${configopts[@]}" -DHOST_BINARY_DIR=$(pwd)/../host-build
    )
}

build() {
    host_env
    (
        cd $workdir/host-build
        make "${makeopts[@]}"
    )

    target_env
    (
        cd $workdir/build
        make "${makeopts[@]}"
    )
}

install() {
    (
        cd $workdir/build
        make install
    )

    ln -sf /usr/local/games/openttd $DESTDIR/usr/local/bin/openttd
}
