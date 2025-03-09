#!/usr/bin/env -S bash ../.port_include.sh

port='zig'
version='0.15.0-dev.208+8acedfd5b'
files=(
    'https://github.com/ziglang/zig-bootstrap/archive/03a95efaf981b9cee59af8027304c37a59d36fc0.tar.gz#6541363baacc64ede96bcfb81b562a8a825d65fb4a05ab4e321be74c656a61d1'
    'https://github.com/ziglang/zig/archive/8acedfd5baabab705946ad097746f9183ef62420.tar.gz#062ad37a4b501340e10b121ef4f825c411ec51385d34666e5f11f18ef9d6d6d9'
)

# The actual directory to build in.
workdir='zig-bootstrap-03a95efaf981b9cee59af8027304c37a59d36fc0'
# The newer Zig directory we move into the workdir.
zigdir='zig-8acedfd5baabab705946ad097746f9183ef62420'

# This is read in the build script to set gcc_dir in the custom libc definition file
# TODO: Maybe we can just symlink the crt files to /usr/local/lib or otherwise remove the need for a hardcoded version
export SERENITY_GCC_VERSION='13.2.0'

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

    # Copy libSystem definitions which are required on macOS, once we set $ZIG_LIBC it will no
    # longer be found in its original place
    run cp zig/lib/libc/darwin/libSystem.tbd "${DESTDIR}/usr/lib/"
}

build() {
    if [ ! -f "${DESTDIR}/usr/local/lib/gcc/${SERENITY_ARCH}-pc-serenity/${SERENITY_GCC_VERSION}/crtbeginS.o" ] ||
       [ ! -f "${DESTDIR}/usr/local/lib/gcc/${SERENITY_ARCH}-pc-serenity/${SERENITY_GCC_VERSION}/crtendS.o" ]; then
        echo "crtbeginS.o or crtendS.o could not be found, ensure the GCC port is installed."
        exit 1
    fi

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
