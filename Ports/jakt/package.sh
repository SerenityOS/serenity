#!/usr/bin/env -S bash ../.port_include.sh
port='jakt'
version='git'
useconfigure='true'
depends=(
    'llvm'
)
commit_hash='eb2a4856aa35db47d5e7fa5648e3876ad096bd7f'
archive_hash='d7b5e772074a44f428facd230ac9dafdcf2034c188e31715f592b6058162e4d7'
files=(
    "https://github.com/SerenityOS/jakt/archive/${commit_hash}.tar.gz#${archive_hash}"
)
launcher_name='Jakt'
launcher_category='D&evelopment'
launcher_command='/usr/local/bin/jakt --repl'
launcher_run_in_terminal='true'
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
