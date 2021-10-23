#!/usr/bin/env -S bash ../.port_include.sh
port=VVVVVV
useconfigure=true
version=2.4
depends=("SDL2" "SDL2_mixer")
workdir=VVVVVV-master
configopts=("-Sdesktop_version" "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt" "-DSDL2_INCLUDE_DIR=${SERENITY_INSTALL_ROOT}/usr/local/include/SDL2")
files="https://github.com/xslendix/VVVVVV/archive/refs/heads/master.zip master.zip 31fba882028b2b041418f52afa970ab06817a79d2edef789464800e6e521642b"
auth_type=sha256
launcher_name="VVVVVV"
launcher_category=Games
launcher_command="/opt/VVVVVV/VVVVVV"

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run mkdir -p "${SERENITY_INSTALL_ROOT}/opt/VVVVVV"
    run cp -r VVVVVV liblodepng-static.a libphysfs-static.a libtinyxml2-static.a "${SERENITY_INSTALL_ROOT}/opt/VVVVVV"
    echo "INFO: Copy data.zip file from PC distribution of the game to the /opt/VVVVVV directory"
}
