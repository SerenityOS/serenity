#!/usr/bin/env -S bash ../.port_include.sh
port='jakt'
version='git'
useconfigure='true'
depends=(
    'llvm'
)
commit_hash='685ea81f7b2f6f98778ac06601bb1c678b73584d'
archive_hash='9d1206ed137277ee9ffb24555652570d3bdb29d4d60617a63d69caaeebcc4638'
files=(
    "https://github.com/SerenityOS/jakt/archive/${commit_hash}.tar.gz#${archive_hash}"
)
workdir="jakt-${commit_hash}"

configure() {
    host_env
    install_path="$(realpath "${workdir}/jakt-install")"
    run cmake \
        -GNinja \
        -B build-host \
        -S . \
        -DCMAKE_BUILD_TYPE=Release \
        -DSERENITY_SOURCE_DIR="${SERENITY_SOURCE_DIR}" \
        -DCMAKE_INSTALL_PREFIX="${install_path}"

    target_env
    # FIXME: CMAKE_INSTALL_PREFIX should be correctly set by the cmake toolchain file,
    #        but CMakeToolchain.txt sets it to the host path /usr/local.
    run cmake \
        -GNinja \
        -B build \
        -S . \
        -DCMAKE_TOOLCHAIN_FILE="${SERENITY_BUILD_DIR}/CMakeToolchain.txt" \
        -DSERENITY_SOURCE_DIR="${SERENITY_SOURCE_DIR}" \
        -DCMAKE_BUILD_TYPE=Release \
        -DJAKT_BOOTSTRAP_COMPILER="${install_path}/bin/jakt" \
        -DCMAKE_INSTALL_PREFIX="${SERENITY_INSTALL_ROOT}/usr/local"
}

build() {
    run cmake --build build-host
    run cmake --install build-host
    run cmake --build build
}

install() {
    run cmake --install build
}
