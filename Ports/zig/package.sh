#!/usr/bin/env -S bash ../.port_include.sh

port='zig'
version='0.12.0-dev.141+ddf5859c2'
files=(
    'https://github.com/ziglang/zig-bootstrap/archive/34644ad5032c58e39327d33d7f96d63d7c330003.tar.gz#e502ae17b01f03c627927d60b2e26b5f7f83b0e8be27b6ef55511d52e5892ccf'
    'https://github.com/ziglang/zig/archive/ddf5859c22527c6bf5d8bb13310db996fcc58874.tar.gz#9adaf787b6233cfbe784d2d8a72398784f3742e2f5ac700cbd59ba952f9491ad'
)

# The actual directory to build in.
workdir='zig-bootstrap-34644ad5032c58e39327d33d7f96d63d7c330003'
# The newer Zig directory we move into the workdir.
zigdir='zig-ddf5859c22527c6bf5d8bb13310db996fcc58874'

post_fetch() {
    # Move the newer version of Zig into the bootstrap
    run rm -rf zig
    run mv "../${zigdir}" zig

    # Copy the scripts that the build process will use
    run mkdir -p out
    run cp -r "${PORT_META_DIR}/scripts" out/
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
