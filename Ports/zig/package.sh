#!/usr/bin/env -S bash ../.port_include.sh

source libc.sh

port='zig'
version='0.17.0'
files=(
    "https://ziglang.org/download/${version}/zig-bootstrap-${version}.tar.xz#1e9e9b8e3c753b35dfb1d9ea48f097579fadbd3e5c1e724ab3fc5bce05477b71"
    "https://ziglang.org/download/${version}/zig-${version}.tar.xz#b6c7f1728f043700d6529bac980800792f824256a9d2f1839b3d62beed0b8abd"
)
useconfigure='true'

# The actual directory to build in.
workdir="zig-bootstrap-${version}"
# The newer Zig directory we move into the workdir.
zigdir="zig-${version}"

post_fetch() {
    # NOTE: Running this multiple times is a massive footgun as patches only get applied once,
    #       the next time we'd end up with a clean copy of the original Zig sources.
    if [ -f "${workdir}/.post-fetch-executed" ]; then
        return
    fi
    run touch .post-fetch-executed

    # Move the newer version of Zig into the bootstrap
    run rm -rf zig
    run cp -r "../${zigdir}" zig

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
