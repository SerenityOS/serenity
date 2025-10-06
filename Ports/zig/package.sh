#!/usr/bin/env -S bash ../.port_include.sh

port='zig'
version='0.16.0-dev.627+e6e4792a5'
files=(
    'https://github.com/ziglang/zig-bootstrap/archive/47f663af1a3c25da6003f662bc2c1b89b91b67b4.tar.gz#9b60c37236fa05534347ee88a563683794111487aaa72670a21b64fa5c31e901'
    'https://github.com/ziglang/zig/archive/e6e4792a585d4fb462749d78fa73d1403c97caf0.tar.gz#8650970982dcf0e066cd23cbc9fb9c7eebe9e3c2f19d4bacbdfabf603aa510de'
)

# The actual directory to build in.
workdir='zig-bootstrap-47f663af1a3c25da6003f662bc2c1b89b91b67b4'
# The newer Zig directory we move into the workdir.
zigdir='zig-e6e4792a585d4fb462749d78fa73d1403c97caf0'

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
