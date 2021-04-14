#!/usr/bin/env -S bash ../.port_include.sh
port=emu2
version=ff276eb0a755a3e784f73da00b5db6c1b25c1f83
files="https://github.com/dmsc/emu2/archive/${version}.zip emu2-${version}.zip 2640a713d6c7ed98d020e0b7dccbc404"

build() {
    export CC="${SERENITY_ROOT}/Toolchain/Local/${SERENITY_ARCH}/bin/${SERENITY_ARCH}-pc-serenity-gcc"
    run make DESTDIR="${SERENITY_BUILD_DIR}/Root" CC="${CC}" $installopts
}
