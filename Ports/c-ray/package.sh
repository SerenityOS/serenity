#!/usr/bin/env -S bash ../.port_include.sh
port=c-ray
version=8f30eb9904a4d20a78e9387d79dc049c5ed69b0c
useconfigure=true
files=(
    "https://github.com/vkoskiv/c-ray/archive/${version}.tar.gz#27fa6496721faf69f18dc0946f0747b64f3ced748440a8f906f51fcb7e5cb008"
)
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
