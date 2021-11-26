#!/usr/bin/env -S bash ../.port_include.sh
port=c-ray
version=c094d64570c30c70f4003e9428d31a2a0d9d3d41
useconfigure=true
files="https://github.com/vkoskiv/c-ray/archive/${version}.tar.gz ${version}.tar.gz 1e0663a1d83e8a9984aced33b9307471f3302c8a5ea7ec47954854d60902a747"
auth_type=sha256
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")
depends=("SDL2")
workdir="${port}-${version}"

configure() {
    run cmake "${configopts[@]}"
}

install() {
    mkdir -p "${SERENITY_INSTALL_ROOT}/home/anon/c-ray"
    cp -r "${port}-${version}"/* "${SERENITY_INSTALL_ROOT}/home/anon/c-ray"
}
