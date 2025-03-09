#!/usr/bin/env -S bash ../.port_include.sh

port='zig'
version='0.14.0'
files=(
    "https://github.com/ziglang/zig/releases/download/${version}/zig-bootstrap-${version}.tar.xz#bf3fcb22be0b83f4791748adb567d3304779d66d7bf9b1bd557ef6c2e0232807"
    "https://github.com/ziglang/zig/archive/refs/tags/${version}.tar.gz#b45589b782a9406a414a3cac201ca0243650f9bedcb453730c124bf6f07ab33f"
)

# The actual directory to build in.
workdir="zig-bootstrap-${version}"
# The newer Zig directory we move into the workdir.
zigdir="zig-${version}"

post_fetch() {
    # NOTE: Running this multiple times is a massive footgun as patches only get applied once, the
    #       next time we'd end up with a clean copy of the original Zig sources.
    if [ -f .post-fetch-executed ]; then
        return
    fi
    run touch .post-fetch-executed

    # Move the newer version of Zig into the bootstrap
    run rm -rf zig
    run cp -r "../${zigdir}" zig

    # Copy the scripts that the build process will use
    run mkdir -p out
    run cp -r "${PORT_META_DIR}/scripts" out/

    # Copy libSystem definitions which are required on macOS, once we set $ZIG_LIBC it will no
    # longer be found in its original place
    run cp zig/lib/libc/darwin/libSystem.tbd "${DESTDIR}/usr/lib/"
}

build() {
    host_env
    cd "${workdir}"
    ./build "${SERENITY_ARCH}-serenity-none" "native"
}

install() {
    zig_install_dir="${workdir}/out/zig-${SERENITY_ARCH}-serenity-none-native"

    mkdir -p "${DESTDIR}/usr/local/bin/."
    mkdir -p "${DESTDIR}/usr/local/lib/."
    cp -rv "${zig_install_dir}/zig" "${DESTDIR}/usr/local/bin/"
    cp -rv "${zig_install_dir}/lib/"* "${DESTDIR}/usr/local/lib/"
}
