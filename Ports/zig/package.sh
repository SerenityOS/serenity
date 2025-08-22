#!/usr/bin/env -S bash ../.port_include.sh

port='zig'
version='0.16.0-dev.9+8248597a2'
files=(
    'https://github.com/ziglang/zig-bootstrap/archive/c6bc9398c72c7a63fe9420a9055dcfd1845bc266.tar.gz#ff051150d10333a6dfaa91173d79b10498cf3d77a09bb27cbcc8818ed3127013'
    'https://github.com/ziglang/zig/archive/8248597a2726663a89e31896f0e9751db5b9db82.tar.gz#3e0937fda9cb822cfb6f88c0315658664262480f367beec62c3abeed93200db4'
)

# The actual directory to build in.
workdir='zig-bootstrap-c6bc9398c72c7a63fe9420a9055dcfd1845bc266'
# The newer Zig directory we move into the workdir.
zigdir='zig-8248597a2726663a89e31896f0e9751db5b9db82'

# TODO: This should probably be exported by .hosted_defs.sh for convenience
ports_dir=$(realpath "$(dirname "${BASH_SOURCE[0]}")/..")

# The patched Zig build script uses this to set gcc_dir in the generated libc_installation.txt
export SERENITY_GCC_VERSION="$("${ports_dir}/gcc/package.sh" showproperty version)"

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

build() {
    local gcc_lib_dir="${DESTDIR}/usr/local/lib/gcc/${SERENITY_ARCH}-pc-serenity/${SERENITY_GCC_VERSION}"

    if [ ! -f "${gcc_lib_dir}/crtbeginS.o" ] || [ ! -f "${gcc_lib_dir}/crtendS.o" ]; then
        echo "crtbeginS.o or crtendS.o could not be found, ensure the GCC port is installed."
        exit 1
    fi

    host_env
    run ./build "${SERENITY_ARCH}-serenity-none" "native"
}

install() {
    local zig_install_dir="out/zig-${SERENITY_ARCH}-serenity-none-native"

    run mkdir -p "${DESTDIR}/usr/local/bin/."
    run mkdir -p "${DESTDIR}/usr/local/lib/."
    run cp -rv "${zig_install_dir}/zig" "${DESTDIR}/usr/local/bin/"
    run cp -rv "${zig_install_dir}/lib/." "${DESTDIR}/usr/local/lib/"
}
