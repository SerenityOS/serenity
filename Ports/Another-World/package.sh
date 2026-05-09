#!/usr/bin/env -S bash ../.port_include.sh
port=Another-World
useconfigure=true
version=170b7d466a6ea6cc36a9e53506d5b858ed503a84
depends=("SDL2" "zlib")
workdir=Another-World-Bytecode-Interpreter-${version}
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt" "-DSDL2_INCLUDE_DIR=${SERENITY_INSTALL_ROOT}/usr/local/include/SDL2")
files=(
    "https://github.com/fabiensanglard/Another-World-Bytecode-Interpreter/archive/${version}.zip#1035c08f0c040d81d210dc5879156d11de3a77c9a1bebf409c4da966cbe955a6"
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
