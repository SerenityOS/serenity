#!/bin/bash ../.port_include.sh
port='SpaceCadetPinball'
version='2.1.0'
workdir="SpaceCadetPinball-Release_${version}"
useconfigure='true'
files=(
    "https://github.com/k4zmu2a/SpaceCadetPinball/archive/refs/tags/Release_${version}.tar.gz#b647dc59abad3d52378b9f67ff4fb70a37e9752afaff1d098b71028cad29b8d6"
)
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
)
depends=(
    "SDL2"
    "SDL2_mixer"
)
launcher_name='SpaceCadetPinball'
launcher_category='&Games'
launcher_command='/usr/local/bin/SpaceCadetPinball'

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make install
	echo "SpaceCadetPinball is installed!"
	echo ""
	echo "Make sure the data files from your original Pinball install are in the following directory:"
	echo "    Inside SerenityOS: /usr/share/SpaceCadetPinball/"
	echo "    Outside SerenityOS: ${SERENITY_SOURCE_DIR}/Base/usr/share/SpaceCadetPinball/"
}
