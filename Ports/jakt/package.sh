#!/usr/bin/env -S bash ../.port_include.sh
port='jakt'
version='git'
useconfigure='true'
depends=(
    'llvm'
)
commit_hash='063e9767ff80db1a1cfe1a805cc8b7e2e577d9f3'
archive_hash='0cb858291d0426e80c8378d7d5876a2a8de747467a289bb691782316c79a2f59'
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
