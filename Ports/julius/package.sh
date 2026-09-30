#!/usr/bin/env -S bash ../.port_include.sh
port='julius'
version='1.8.0'
useconfigure='true'
files=(
    "https://github.com/bvschaik/julius/archive/refs/tags/v${version}.tar.gz#e479e0b60074497b3e81b30749e040c423f493d469d630a774c06b3d61d91159"
)
depends=(
    'libpng'
    'SDL2'
    'SDL2_mixer'
)
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    # Upstream declares 3.1 which is no longer supported
    "-DCMAKE_POLICY_VERSION_MINIMUM=3.25"
)
data_dir='/home/anon/Games/julius'
launcher_name='Julius'
launcher_category='&Games'
launcher_workdir="${data_dir}/"
launcher_command="/usr/local/bin/julius"
icon_file='res/julius_32.png'

configure() {
    run cmake "${configopts[@]}" .
}

install() {
    run_nocd mkdir -p "${SERENITY_INSTALL_ROOT}/usr/local/bin/"
    run cp -r julius "${SERENITY_INSTALL_ROOT}/usr/local/bin/"
}

post_install() {
    echo
    echo 'Julius is installed!'
    echo
    echo 'Make sure your game files are present in the following directory:'
    echo "    Inside SerenityOS: ${data_dir}/"
    echo "    Outside SerenityOS: $(realpath ${SERENITY_INSTALL_ROOT}/${data_dir})/"
    echo
}
