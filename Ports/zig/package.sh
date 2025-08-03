#!/usr/bin/env -S bash ../.port_include.sh

port='zig'
version='0.15.0-dev.1380+e98aeeb73'
files=(
    'https://github.com/ziglang/zig-bootstrap/archive/de424301411b3a34a8a908d8dca01a1d29f2c6df.tar.gz#f344c005f44976124bdad801b1de8f8b9b33b59ac006c95b1caa37d0ab917425'
    'https://github.com/ziglang/zig/archive/e98aeeb73fba942c8e061bb8158ed073a7b19e1d.tar.gz#b0c9da0cf761873e0eb01bd97b1b8643b80a106a9cdba79cf8443638a7e8155a'
)

# The actual directory to build in.
workdir='zig-bootstrap-de424301411b3a34a8a908d8dca01a1d29f2c6df'
# The newer Zig directory we move into the workdir.
zigdir='zig-e98aeeb73fba942c8e061bb8158ed073a7b19e1d'

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
