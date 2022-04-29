#!/usr/bin/env -S bash ../.port_include.sh
port=OpenRCT2
version=0.4.0
useconfigure="true"
files="https://github.com/OpenRCT2/OpenRCT2/archive/refs/tags/v${version}.tar.gz ${port}-${version}.tar.gz fc02d002a5dc12861a50d30da8fbcfb26eeb2faa3e2b6d56108fc8a9ed35e5e9
       https://github.com/EWouters/g2/releases/download/v0.1/g2.dat.tar.gz g2.dat.tar.gz 4e38ac1686fd8a3b3bda726d7ec677d465c13c8fe704d8d40bf6e2b1d1875b44"
auth_type=sha256
depends=("curl" "fontconfig" "freetype" "libicu" "libpng" "libzip" "nlohmann_json" "openssl")

configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
            "-DDISABLE_GUI=ON"
            "-DDISABLE_GOOGLE_BENCHMARK=ON"
            "-DDISABLE_DISCORD_RPC=ON"
            "-DDISABLE_NETWORK=ON"
)

pre_configure() {
    export CFLAGS+="-L${SERENITY_INSTALL_ROOT}/usr/local/include"
    export LDFLAGS+="-L${SERENITY_INSTALL_ROOT}/usr/local/lib"
}

configure() {
    mkdir -p ${workdir}/build
    mv g2.dat ${workdir}/build/
    cmake -G Ninja \
        -S ${workdir} \
        -B ${workdir}/build \
        "${configopts[@]}"
}

build() {
    ninja -C ${workdir}/build
}

install() {
    ninja -C ${workdir}/build install
}
