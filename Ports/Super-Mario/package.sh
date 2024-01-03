#!/usr/bin/env -S bash ../.port_include.sh
port=Super-Mario
useconfigure=true
version=git
depends=("SDL2" "SDL2_mixer" "SDL2_image")
workdir=Super-Mario-Clone-Cpp-master
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")
files=(
    "https://github.com/Bennyhwanggggg/Super-Mario-Clone-Cpp/archive/refs/heads/master.zip#fcacc15d3b5afccb3227f982d3e05f2cfeb198f0fffd008fdcda005cb7f87f91"
)
launcher_name="Super Mario"
launcher_category='&Games'
launcher_command=/opt/Super_Mario/uMario

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run mkdir -p "${SERENITY_INSTALL_ROOT}/opt/Super_Mario"
    run cp -r uMario files "${SERENITY_INSTALL_ROOT}/opt/Super_Mario"
    if command -v convert >/dev/null; then
        run convert "app.ico[0]" app-16x16.png
        run convert "app.ico[1]" app-32x32.png
        run $OBJCOPY --add-section serenity_icon_s="app-16x16.png" "${SERENITY_INSTALL_ROOT}/opt/Super_Mario/uMario"
        run $OBJCOPY --add-section serenity_icon_m="app-32x32.png" "${SERENITY_INSTALL_ROOT}/opt/Super_Mario/uMario"
    fi
}
