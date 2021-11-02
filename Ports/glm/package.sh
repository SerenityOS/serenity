#!/usr/bin/env -S bash ../.port_include.sh
port=glm
version=0.9.9.8
files="https://github.com/g-truc/glm/releases/download/${version}/glm-${version}.zip glm-${version}.zip 37e2a3d62ea3322e43593c34bae29f57e3e251ea89f4067506c94043769ade4c"
auth_type=sha256
depends=()
workdir=glm
useconfigure=true
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run cp -R glm "${SERENITY_BUILD_DIR}/Root/usr/local/include/"
}
