#!/usr/bin/env -S bash ../.port_include.sh
port='glm'
version='1.0.3'
files=(
    "https://github.com/g-truc/glm/releases/download/${version}/glm-${version}.zip#1c0a0fced9b0d87c7b7bc94e40be490cff6d4c83c25db8488d8f33754e7fdeb2"
)
workdir='glm'
useconfigure='true'
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")

configure() {
    run cmake "${configopts[@]}"
}

install() {
    target_dir="${SERENITY_INSTALL_ROOT}/usr/local/include/"
    run_nocd mkdir -p "${target_dir}"
    run cp -R glm "${target_dir}"
}
