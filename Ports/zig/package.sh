#!/usr/bin/env -S bash ../.port_include.sh

source libc.sh

port='zig'
version='0.17.1'
files=(
    "https://ziglang.org/download/${version}/zig-bootstrap-${version}.tar.xz#58c099060ad9237496c7e7f57235d9eb9e5a3713e7df5f90d8ccc06391a359dd"
)
useconfigure='true'
workdir="zig-bootstrap-${version}"

post_fetch() {
    # Copy libSystem definitions which are required on macOS, once we set $ZIG_LIBC it will no
    # longer be found in its original place
    run cp zig/lib/libc/darwin/libSystem.tbd "${DESTDIR}/usr/lib/"
}

configure() {
    check_gcc_crt_files
    # The patched Zig build script exports ZIG_LIBC
    create_libc_file "${PORT_BUILD_DIR}/${workdir}/out/libc.txt"
}

build() {
    host_env
    run ./build "${SERENITY_ARCH}-serenity-none" "baseline"
}

install() {
    local zig_install_dir="out/zig-${SERENITY_ARCH}-serenity-none-baseline"

    run mkdir -p "${DESTDIR}/usr/local/bin/."
    run mkdir -p "${DESTDIR}/usr/local/lib/."
    run cp -rv "${zig_install_dir}/zig" "${DESTDIR}/usr/local/bin/"
    run cp -rv "${zig_install_dir}/lib/." "${DESTDIR}/usr/local/lib/"
}
