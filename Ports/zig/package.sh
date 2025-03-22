#!/usr/bin/env -S bash ../.port_include.sh

port='zig'
version='0.15.0-dev.64+2a4e06bcb'
files=(
    'https://github.com/ziglang/zig-bootstrap/archive/11e20c3717ffdc8b2d44aaea24703c434bbdea6c.tar.gz#fa19a22bac6c93ee4f60a3c2f3e4962dd76a2e248cb715764f56df868c61c5d5'
    'https://github.com/ziglang/zig/archive/2a4e06bcb30f71e83b14026bcbade6aac3aece84.tar.gz#1c735c9251cc4f24319d24889b83450a42f17d8538aee84fce2cd8d0ef331e15'
)

# The actual directory to build in.
workdir='zig-bootstrap-11e20c3717ffdc8b2d44aaea24703c434bbdea6c'
# The newer Zig directory we move into the workdir.
zigdir='zig-2a4e06bcb30f71e83b14026bcbade6aac3aece84'

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
