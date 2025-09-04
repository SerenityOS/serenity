#!/usr/bin/env -S bash ../.port_include.sh

port='zig'
version='0.16.0-dev.172+a11dfaf61'
files=(
    'https://github.com/ziglang/zig-bootstrap/archive/72469a02edf7a64339d19d24d677cde9bc74c874.tar.gz#acb65327026266246fde1054d759941b1b8d465f2f11b4329f2d753b34635e93'
    'https://github.com/ziglang/zig/archive/a11dfaf61aa83df6c58b647e20c1a03d2e767f7b.tar.gz#64060ce040fdaf0a5eaf7eb8de573c9e134baa7b08be717fad4ae5b71a1966c0'
)

# The actual directory to build in.
workdir='zig-bootstrap-72469a02edf7a64339d19d24d677cde9bc74c874'
# The newer Zig directory we move into the workdir.
zigdir='zig-a11dfaf61aa83df6c58b647e20c1a03d2e767f7b'

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
