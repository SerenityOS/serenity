#!/usr/bin/env -S bash ../.port_include.sh
port='julius'
version='1.7.0'
useconfigure='true'
files=(
    "https://github.com/bvschaik/julius/archive/refs/tags/v${version}.tar.gz#3ee62699bcbf6c74fe5a9c940c62187141422a9bd98e01747a554fd77483431f"
)
depends=(
    'libpng'
    'SDL2'
    'SDL2_mixer'
)
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
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
