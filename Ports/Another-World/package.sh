#!/usr/bin/env -S bash ../.port_include.sh
port=Another-World
useconfigure=true
version=git
depends=("SDL2" "zlib")
workdir=Another-World-Bytecode-Interpreter-master
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt" "-DSDL2_INCLUDE_DIR=${SERENITY_INSTALL_ROOT}/usr/local/include/SDL2")
files=(
    "https://github.com/fabiensanglard/Another-World-Bytecode-Interpreter/archive/refs/heads/master.zip#326de7622e5f83a83fce76e6032240157a9dde83c0d65319095c7e0b312af317"
)
launcher_name="Another World"
launcher_category='&Games'
launcher_command="/opt/Another-World/raw --datapath=/opt/Another-World"

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run mkdir -p "${SERENITY_INSTALL_ROOT}/opt/Another-World"
    run cp -r raw "${SERENITY_INSTALL_ROOT}/opt/Another-World"
    echo "INFO: Copy BANK* and MEMLIST.BIN files from MS-DOS distribution of the game to the /opt/Another_World directory"
}
