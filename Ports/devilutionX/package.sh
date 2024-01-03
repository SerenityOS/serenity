#!/usr/bin/env -S bash ../.port_include.sh
port='devilutionX'
version='1.5.1'
useconfigure='true'
files=(
    "https://github.com/diasurgical/devilutionX/archive/refs/tags/${version}.tar.gz#9d88dec4df58e391940b77edb86f356b34e2c085c5e3191262fa6276d745d1cb"
)
depends=(
    'bzip2'
    'libpng'
    'SDL2'
    'SDL2_image'
)
configopts=(
    '-DCMAKE_BUILD_TYPE=Release'
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"    
    '-DDEVILUTIONX_SYSTEM_LIBFMT=OFF'
    '-DNONET=ON'
)
install_dir='/opt/devilutionx'
launcher_name='DevilutionX'
launcher_category='&Games'
launcher_command="${install_dir}/devilutionx"
icon_file='Packaging/resources/icon_32.png'

configure() {
    # TODO: Figure out why GCC doesn't autodetect that libgcc_s is needed.
    if [ "${SERENITY_TOOLCHAIN}" = "GNU" ]; then
        export LDFLAGS="-lgcc_s"
    fi
    run cmake "${configopts[@]}" .
}

install() {
    run_nocd mkdir -p "${SERENITY_INSTALL_ROOT}${install_dir}/"
    run cp -r devilutionx assets/ "${SERENITY_INSTALL_ROOT}${install_dir}/"
    run_nocd mkdir -p "${SERENITY_INSTALL_ROOT}/usr/local/bin/"
    run ln -sf "${install_dir}/devilutionx" "${SERENITY_INSTALL_ROOT}/usr/local/bin/devilutionx"
}

post_install() {
    echo
    echo 'DevilutionX is installed!'
    echo
    echo 'Make sure the DIABDAT.MPQ file from your installation CD is in the following directory:'
    echo "    Inside SerenityOS: ${install_dir}/"
    echo "    Outside SerenityOS: ${SERENITY_SOURCE_DIR}/Base${install_dir}/"
    echo
}
