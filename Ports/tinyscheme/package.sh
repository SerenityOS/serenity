#!/usr/bin/env -S bash ../.port_include.sh
port=tinyscheme
version=1.42
files="https://downloads.sourceforge.net/project/tinyscheme/tinyscheme/tinyscheme-${version}/tinyscheme-${version}.tar.gz tinyscheme-${version}.tar.gz 273ac5ffe5305986b329e9045f2aea89"

useconfigure=false

build() {
    run make scheme CC="${CC} -fpic -pedantic" SYS_LIBS= FEATURES='-DUSE_NO_FEATURES=1 -DInitFile=\"/usr/local/include/tinyscheme/init.scm\"'
}

install() {
    run mkdir -p "${SERENITY_BUILD_DIR}/Root/usr/local/bin"
    run cp scheme "${SERENITY_BUILD_DIR}/Root/usr/local/bin/tinyscheme"
    run mkdir -p "${SERENITY_BUILD_DIR}/Root/usr/local/include/tinyscheme"
    run cp init.scm "${SERENITY_BUILD_DIR}/Root/usr/local/include/tinyscheme/init.scm"
}
