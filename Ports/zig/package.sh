#!/usr/bin/env -S bash ../.port_include.sh

port='zig'
version='0.11.0-dev.4003+c6aa29b6f'
files=(
    'https://github.com/ziglang/zig-bootstrap/archive/bf1b2cdb83141ad9336eec42160c9fe87f90198d.tar.gz bf1b2cdb83141ad9336eec42160c9fe87f90198d.tar.gz 363f97884f4a291c7167468e53cf4570fa03bc7b8973365dbce2019ffc103150'
    'https://github.com/ziglang/zig/archive/c6aa29b6fdba1606bfd218b17de89f64179c0ed8.tar.gz c6aa29b6fdba1606bfd218b17de89f64179c0ed8.tar.gz d63c5087a737c46072f155eacacaa406af67addab39ad8179c44b0fc7d698ac1'
)

# The actual directory to build in.
workdir='zig-bootstrap-bf1b2cdb83141ad9336eec42160c9fe87f90198d'
# The newer Zig directory we move into the workdir.
zigdir='zig-c6aa29b6fdba1606bfd218b17de89f64179c0ed8'

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
    cp -rv "${zig_install_dir}/bin/"* "${DESTDIR}/usr/local/bin/"
    cp -rv "${zig_install_dir}/lib/"* "${DESTDIR}/usr/local/lib/"
}
