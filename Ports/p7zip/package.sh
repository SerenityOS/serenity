#!/usr/bin/env -S bash ../.port_include.sh

port="p7zip"
version="17.04"
useconfigure=true
files=(
    "https://github.com/jinfeihan57/p7zip/archive/refs/tags/v${version}.tar.gz#ea029a2e21d2d6ad0a156f6679bd66836204aa78148a4c5e498fe682e77127ef"
)
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")
workdir=$port-$version/CPP
depends=("libiconv")

post_fetch() {
    run_replace_in_file "s/\r//" 7zip/CMAKE/7za/CMakeLists.txt
}

configure() {
    run cmake 7zip/CMAKE "${configopts[@]}"
}

build() {
    run make "${makeopts[@]}"
}

install() {
    run cp -r bin/Codecs $SERENITY_INSTALL_ROOT/usr/local/bin
    run cp bin/7z_ $SERENITY_INSTALL_ROOT/usr/local/bin
    run cp bin/7z.so $SERENITY_INSTALL_ROOT/usr/local/bin
    run cp bin/7za $SERENITY_INSTALL_ROOT/usr/local/bin
    run cp bin/7zCon.sfx $SERENITY_INSTALL_ROOT/usr/local/bin
    run cp bin/7zr $SERENITY_INSTALL_ROOT/usr/local/bin
}
