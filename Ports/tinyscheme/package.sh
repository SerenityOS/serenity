#!/usr/bin/env -S bash ../.port_include.sh
port=tinyscheme
version=1.42
files=(
    "https://downloads.sourceforge.net/project/tinyscheme/tinyscheme/tinyscheme-${version}/tinyscheme-${version}.tar.gz#17b0b1bffd22f3d49d5833e22a120b339039d2cfda0b46d6fc51dd2f01b407ad"
)
useconfigure=false

build() {
    run make scheme CC="${CC} -fpic -pedantic" SYS_LIBS='-ldl' FEATURES='-DUSE_NO_FEATURES=1 -DInitFile=\"/usr/local/include/tinyscheme/init.scm\"'
}

install() {
    run mkdir -p "${SERENITY_BUILD_DIR}/Root/usr/local/bin"
    run cp scheme "${SERENITY_BUILD_DIR}/Root/usr/local/bin/tinyscheme"
    run mkdir -p "${SERENITY_BUILD_DIR}/Root/usr/local/include/tinyscheme"
    run cp init.scm "${SERENITY_BUILD_DIR}/Root/usr/local/include/tinyscheme/init.scm"
}
