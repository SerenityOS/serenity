#!/usr/bin/env -S bash ../.port_include.sh
port='Super-Mario'
version='7293c0776f23f1d7fc1e106d1f32ffda83d7e3ac'
depends=(
    'SDL2'
    'SDL2_mixer'
    'SDL2_image'
)
workdir=Super-Mario-Clone-Cpp-${version}
useconfigure='true'
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    # Upstream declares 2.6 which is no longer supported
    '-DCMAKE_POLICY_VERSION_MINIMUM=3.25'
)
files=(
    "https://github.com/Bennyhwanggggg/Super-Mario-Clone-Cpp/archive/${version}.zip#2167f9676cf053d719e865e6e82e81c7dc98ef6053dd961f302f72c70cf2b76f"
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
